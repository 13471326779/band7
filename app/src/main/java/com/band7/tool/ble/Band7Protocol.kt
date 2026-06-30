package com.band7.tool.ble

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec

/**
 * 小米手环7 加密协议实现
 * 基于 Xiaomi Encrypted BLE V1
 */
object Band7Protocol {

    private val random = SecureRandom()

    fun generateNonce(): ByteArray {
        val nonce = ByteArray(16)
        random.nextBytes(nonce)
        return nonce
    }

    fun aesEncrypt(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    fun aesDecrypt(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    fun deriveSessionKeys(authKey: ByteArray, serverNonce: ByteArray, clientNonce: ByteArray): ByteArray {
        val combined = (serverNonce + clientNonce.copyOf(4)).copyOf(16)
        val tmpKey = aesEncrypt(authKey, combined)
        val combined2 = (clientNonce + serverNonce.copyOf(4)).copyOf(16)
        return aesEncrypt(tmpKey, combined2)
    }

    fun createAuthPacket(command: Int, subcmd: Int, data: ByteArray = byteArrayOf()): ByteArray {
        return byteArrayOf(command.toByte(), subcmd.toByte()) + data
    }

    fun buildFilePath(cmdType: Int, path: String): ByteArray {
        val pathBytes = path.toByteArray(Charsets.UTF_8)
        return byteArrayOf(cmdType.toByte()) +
                (pathBytes.size and 0xFF).toByte() +
                ((pathBytes.size shr 8) and 0xFF).toByte() +
                pathBytes
    }

    fun buildUploadPacket(fileType: Int, chunkIndex: Int, data: ByteArray): ByteArray {
        val packet = byteArrayOf(
            CMD_UPLOAD.toByte(),
            fileType.toByte(),
            (chunkIndex and 0xFF).toByte(),
            ((chunkIndex shr 8) and 0xFF).toByte(),
            ((chunkIndex shr 16) and 0xFF).toByte(),
            ((chunkIndex shr 24) and 0xFF).toByte()
        )
        return packet + data
    }

    private const val CMD_UPLOAD = 0x05
}

// ByteArray 扩展
operator fun ByteArray.plus(other: ByteArray): ByteArray {
    val result = ByteArray(this.size + other.size)
    System.arraycopy(this, 0, result, 0, this.size)
    System.arraycopy(other, 0, result, this.size, other.size)
    return result
}
