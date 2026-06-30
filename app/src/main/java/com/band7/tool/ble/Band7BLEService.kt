package com.band7.tool.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import com.band7.tool.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Band7BLEService(private val context: Context) {

    companion object {
        private const val TAG = "Band7BLE"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 状态流
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 发现的设备
    private val _scannedDevices = MutableStateFlow<List<BandDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BandDevice>> = _scannedDevices.asStateFlow()

    // 设备信息
    private val _deviceInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val deviceInfo: StateFlow<Map<String, String>> = _deviceInfo.asStateFlow()

    // 文件列表
    private val _fileList = MutableStateFlow<List<BandFile>>(emptyList())
    val fileList: StateFlow<List<BandFile>> = _fileList.asStateFlow()

    // BLE 回调
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT")
                    bluetoothGatt = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected")
                    _connectionState.value = ConnectionState.Disconnected
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                val device = _connectionState.value.let {
                    if (it is ConnectionState.Connecting) it.device else null
                }
                device?.let { _connectionState.value = ConnectionState.Connected(it) }
                enableAuthNotification(gatt)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleNotification(characteristic)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeAck.complete(Unit)
            } else {
                writeAck.completeExceptionally(Exception("Write failed: $status"))
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readResult.complete(characteristic.value)
            } else {
                readResult.completeExceptionally(Exception("Read failed: $status"))
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown"
            val address = device.address
            val rssi = result.rssi
            val currentDevices = _scannedDevices.value.toMutableList()

            // 更新或添加
            val existing = currentDevices.indexOfFirst { it.address == address }
            val bandDevice = BandDevice(name, address, rssi)
            if (existing >= 0) {
                currentDevices[existing] = bandDevice
            } else {
                currentDevices.add(bandDevice)
            }
            _scannedDevices.value = currentDevices.sortedByDescending { it.rssi }
        }
    }

    // 认证相关
    private var authKey: ByteArray? = null
    private var authCompletable = CompletableDeferred<Boolean>()
    private var notificationCompletable = CompletableDeferred<ByteArray>()
    private var writeAck = CompletableDeferred<Unit>()
    private var readResult = CompletableDeferred<ByteArray>()
    private var authStage = 0
    private var clientNonce: ByteArray? = null

    // --- 公开 API ---

    fun startScan() {
        _connectionState.value = ConnectionState.Scanning
        _scannedDevices.value = emptyList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // 扫描所有 BLE 设备
        bluetoothLeScanner.startScan(null, settings, scanCallback)
        scope.launch {
            delay(15000) // 15秒后停止
            stopScan()
        }
    }

    fun stopScan() {
        try {
            bluetoothLeScanner.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Stop scan error: $e")
        }
        _connectionState.value = ConnectionState.Disconnected
    }

    fun connectToDevice(device: BandDevice) {
        _connectionState.value = ConnectionState.Connecting(device)
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
        bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun connectToAddress(address: String) {
        val device = BandDevice("手环 ($address)", address, 0)
        connectToDevice(device)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun setAuthKey(key: String) {
        authKey = hexStringToBytes(key)
    }

    suspend fun authenticate(): Boolean {
        if (authKey == null) return false
        authStage = 0
        return performAuth()
    }

    suspend fun getDeviceInfo(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        val gatt = bluetoothGatt ?: return info

        val charMap = mapOf(
            Band7Constants.CHAR_FIRMWARE to "firmware",
            Band7Constants.CHAR_SERIAL to "serial",
            Band7Constants.CHAR_HARDWARE to "hardware"
        )

        for ((uuid, key) in charMap) {
            try {
                val characteristic = findCharacteristic(Band7Constants.MI_BAND_SERVICE, uuid)
                if (characteristic != null) {
                    gatt.readCharacteristic(characteristic)
                    val value = readResult.await()
                    info[key] = String(value, Charsets.UTF_8).trimEnd('\u0000')
                    readResult = CompletableDeferred()
                }
            } catch (e: Exception) {
                info[key] = "Unknown"
            }
        }
        _deviceInfo.value = info
        return info
    }

    suspend fun listFiles(path: String = "/"): List<BandFile> {
        val gatt = bluetoothGatt ?: return emptyList()
        val characteristic = findCharacteristic(
            Band7Constants.MI_BAND_SERVICE, Band7Constants.CHAR_DATA_WRITE
        ) ?: return emptyList()

        val packet = Band7Protocol.buildFilePath(Band7Constants.CMD_LIST_FILES, path)
        gatt.writeCharacteristic(characteristic, packet, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        writeAck = CompletableDeferred()
        writeAck.await()

        // 等待响应
        notificationCompletable = CompletableDeferred()
        val response = notificationCompletable.await()

        return parseFileList(response)
    }

    suspend fun deleteFile(path: String): Boolean {
        val gatt = bluetoothGatt ?: return false
        val characteristic = findCharacteristic(
            Band7Constants.MI_BAND_SERVICE, Band7Constants.CHAR_DATA_WRITE
        ) ?: return false

        val packet = Band7Protocol.buildFilePath(Band7Constants.CMD_DELETE_FILE, path)
        gatt.writeCharacteristic(characteristic, packet, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        writeAck = CompletableDeferred()
        writeAck.await()
        delay(200)
        return true
    }

    fun cleanup() {
        scope.cancel()
        disconnect()
    }

    // --- 内部方法 ---

    private fun enableAuthNotification(gatt: BluetoothGatt) {
        val characteristic = findCharacteristic(
            Band7Constants.MI_BAND_SERVICE, Band7Constants.CHAR_AUTH_NOTIFY
        )
        characteristic?.let {
            gatt.setCharacteristicNotification(it, true)
        }
    }

    private suspend fun performAuth(): Boolean {
        try {
            val gatt = bluetoothGatt ?: return false
            val authWrite = findCharacteristic(
                Band7Constants.MI_BAND_SERVICE, Band7Constants.CHAR_AUTH_WRITE
            ) ?: return false

            // Step 1: 发送客户端 nonce
            clientNonce = Band7Protocol.generateNonce()
            val packet1 = Band7Protocol.createAuthPacket(
                Band7Constants.AUTH_COMMAND,
                Band7Constants.AUTH_SUBCMD_START,
                clientNonce!!
            )
            notificationCompletable = CompletableDeferred()
            gatt.writeCharacteristic(authWrite, packet1, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            writeAck = CompletableDeferred()
            writeAck.await()

            // 等待服务器 nonce 响应
            val response = withTimeout(10000) { notificationCompletable.await() }
            val serverNonce = response.copyOfRange(2, 18)

            // Step 2: 派生 session key 并发送加密验证
            val encKey = Band7Protocol.deriveSessionKeys(authKey!!, serverNonce, clientNonce!!)
            val encryptedNonce = Band7Protocol.aesEncrypt(encKey, serverNonce)
            val packet3 = Band7Protocol.createAuthPacket(
                Band7Constants.AUTH_COMMAND,
                Band7Constants.AUTH_SUBCMD_SEND_ENC,
                encryptedNonce
            )
            notificationCompletable = CompletableDeferred()
            gatt.writeCharacteristic(authWrite, packet3, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            writeAck = CompletableDeferred()
            writeAck.await()

            val authResult = withTimeout(10000) { notificationCompletable.await() }
            val success = authResult.size >= 3 && authResult[2] == Band7Constants.AUTH_SUCCESS.toByte()

            if (success) {
                val device = _connectionState.value.let {
                    (it as? ConnectionState.Connected)?.device
                }
                device?.let { _connectionState.value = ConnectionState.Authenticated(it) }
            }
            return success

        } catch (e: Exception) {
            Log.e(TAG, "Auth failed: $e")
            return false
        }
    }

    private fun handleNotification(characteristic: BluetoothGattCharacteristic) {
        val uuid = characteristic.uuid
        val value = characteristic.value

        if (uuid == Band7Constants.CHAR_AUTH_NOTIFY) {
            notificationCompletable.complete(value)
        } else if (uuid == Band7Constants.CHAR_DATA_NOTIFY) {
            notificationCompletable.complete(value)
        }
    }

    private fun findCharacteristic(
        serviceUuid: UUID, characteristicUuid: UUID
    ): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null
        val service = gatt.getService(serviceUuid) ?: return null
        return service.getCharacteristic(characteristicUuid)
    }

    private fun parseFileList(data: ByteArray): List<BandFile> {
        val files = mutableListOf<BandFile>()
        if (data.size < 4) return files

        var pos = 2 // 跳过命令头
        while (pos < data.size - 3) {
            try {
                val nameLen = (data[pos].toInt() and 0xFF) or
                        ((data[pos + 1].toInt() and 0xFF) shl 8)
                pos += 2
                if (pos + nameLen > data.size) break
                val name = String(data.copyOfRange(pos, pos + nameLen), Charsets.UTF_8)
                pos += nameLen
                if (pos + 5 > data.size) break
                val size = (data[pos].toLong() and 0xFF) or
                        ((data[pos + 1].toLong() and 0xFF) shl 8) or
                        ((data[pos + 2].toLong() and 0xFF) shl 16) or
                        ((data[pos + 3].toLong() and 0xFF) shl 24)
                pos += 4
                val type = data[pos].toInt() and 0xFF
                pos += 1
                files.add(BandFile(name, size, type, "/$name"))
            } catch (e: Exception) {
                break
            }
        }
        return files
    }

    private fun hexStringToBytes(hex: String): ByteArray? {
        try {
            val cleanHex = hex.replace(" ", "").replace(":", "")
            val len = cleanHex.length
            if (len % 2 != 0) return null
            val data = ByteArray(len / 2)
            for (i in data.indices) {
                data[i] = ((Character.digit(cleanHex[i * 2], 16) shl 4) +
                        Character.digit(cleanHex[i * 2 + 1], 16)).toByte()
            }
            return data
        } catch (e: Exception) {
            return null
        }
    }
}
