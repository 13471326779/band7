package com.band7.tool.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 手环相册管理器
 * 管理上传到手环的图片，以及本地缓存的相册
 */
object PhotoAlbumManager {
    
    private const val ALBUM_DIR = "band_album"
    private const val MAX_PHOTO_WIDTH = 192
    private const val MAX_PHOTO_HEIGHT = 490
    
    data class AlbumPhoto(
        val id: String,
        val name: String,
        val size: Long,
        val uploadedAt: Long,
        val localPath: String? = null
    )
    
    /**
     * 将图片缩放到手环尺寸并转为上传格式
     */
    fun prepareForUpload(context: Context, imageUri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (original == null) return null
            
            // 缩放到手环屏幕
            val scaled = Bitmap.createScaledBitmap(
                original, MAX_PHOTO_WIDTH, MAX_PHOTO_HEIGHT, true
            )
            if (scaled != original) original.recycle()
            
            // 转为 TGA 格式
            val tgaData = bitmapToTGA(scaled)
            scaled.recycle()
            
            tgaData
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 保存到本地相册缓存
     */
    fun saveToLocalAlbum(context: Context, imageUri: Uri, photoName: String): String? {
        return try {
            val albumDir = File(context.filesDir, ALBUM_DIR)
            if (!albumDir.exists()) albumDir.mkdirs()
            
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val file = File(albumDir, photoName)
            val outputStream = FileOutputStream(file)
            
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream!!.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            inputStream?.close()
            outputStream.close()
            
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取本地相册列表
     */
    fun getLocalPhotos(context: Context): List<AlbumPhoto> {
        val albumDir = File(context.filesDir, ALBUM_DIR)
        if (!albumDir.exists()) return emptyList()
        
        return albumDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".tga") }
            ?.map { file ->
                AlbumPhoto(
                    id = file.nameWithoutExtension,
                    name = file.name,
                    size = file.length(),
                    uploadedAt = file.lastModified(),
                    localPath = file.absolutePath
                )
            }
            ?.sortedByDescending { it.uploadedAt }
            ?: emptyList()
    }
    
    private fun bitmapToTGA(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val output = ByteArrayOutputStream()
        // TGA header
        output.write(0); output.write(0); output.write(2)
        repeat(5) { output.write(0) }
        output.write(0); output.write(0) // x
        output.write(0); output.write(0) // y
        output.write(width and 0xFF); output.write((width shr 8) and 0xFF)
        output.write(height and 0xFF); output.write((height shr 8) and 0xFF)
        output.write(24) // 24-bit
        output.write(0x20) // top-left
        
        // BGR pixel data
        for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                output.write(pixel and 0xFF)          // B
                output.write((pixel shr 8) and 0xFF)  // G
                output.write((pixel shr 16) and 0xFF) // R
            }
        }
        
        return output.toByteArray()
    }
}
