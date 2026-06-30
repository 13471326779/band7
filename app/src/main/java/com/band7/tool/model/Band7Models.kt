package com.band7.tool.model

import java.util.UUID

// 小米手环7 BLE 常量
object Band7Constants {
    // 服务
    val MI_BAND_SERVICE = UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")

    // 认证特征
    val CHAR_AUTH_WRITE = UUID.fromString("00000051-0000-1000-8000-00805f9b34fb")
    val CHAR_AUTH_READ = UUID.fromString("00000052-0000-1000-8000-00805f9b34fb")
    val CHAR_AUTH_NOTIFY = UUID.fromString("00000053-0000-1000-8000-00805f9b34fb")

    // 数据特征
    val CHAR_DATA_WRITE = UUID.fromString("00000054-0000-1000-8000-00805f9b34fb")
    val CHAR_DATA_READ = UUID.fromString("00000055-0000-1000-8000-00805f9b34fb")
    val CHAR_DATA_NOTIFY = UUID.fromString("00000056-0000-1000-8000-00805f9b34fb")

    // 设备信息
    val CHAR_FIRMWARE = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    val CHAR_SERIAL = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
    val CHAR_HARDWARE = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
    val CHAR_BATTERY = UUID.fromString("0000000f-0000-1000-8000-00805f9b34fb")

    // 手环屏幕
    const val SCREEN_WIDTH = 192
    const val SCREEN_HEIGHT = 490
    const val DISPLAY_NAME = "Xiaomi Smart Band 7"

    // 认证命令
    const val AUTH_COMMAND = 0x01
    const val AUTH_RESPONSE = 0x02
    const val AUTH_SUBCMD_START = 0x00
    const val AUTH_SUBCMD_SEND_KEY = 0x01
    const val AUTH_SUBCMD_REQUEST_ENC = 0x02
    const val AUTH_SUBCMD_SEND_ENC = 0x03
    const val AUTH_SUCCESS = 0x01

    // 文件命令
    const val CMD_UPLOAD_FILE = 0x05
    const val CMD_DELETE_FILE = 0x06
    const val CMD_LIST_FILES = 0x07

    // 文件类型
    const val FILE_WATCHFACE = 0x01
    const val FILE_EBK = 0x07
    const val FILE_NORMAL = 0x06
}

// 扫描到的设备
data class BandDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

// 手环文件信息
data class BandFile(
    val name: String,
    val size: Long,
    val type: Int,
    val path: String
)

// 连接状态
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val device: BandDevice) : ConnectionState()
    data class Connected(val device: BandDevice) : ConnectionState()
    data class Authenticated(val device: BandDevice) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
