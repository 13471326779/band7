package com.band7.tool.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

object ImageConverter {

    /**
     * 将图片缩放到手环屏幕尺寸
     */
    fun resizeForBand(imagePath: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            val targetWidth = 192
            val targetHeight = 490
            val scale = maxOf(
                options.outWidth.toFloat() / targetWidth,
                options.outHeight.toFloat() / targetHeight
            )

            val sampleSize = (if (scale > 1) scale.toInt() else 1).coerceAtLeast(1)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            val original = BitmapFactory.decodeFile(imagePath, decodeOptions) ?: return null
            // 缩放
            val scaled = Bitmap.createScaledBitmap(
                original,
                targetWidth, targetHeight, true
            )
            if (scaled != original) original.recycle()
            scaled
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 转换为 ZeppOS 兼容的 TGA 格式
     * 米环7 使用 24位 BGR TGA
     */
    fun bitmapToTGA(bitmap: Bitmap): ByteArray {
        val width = 192
        val height = 490
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = ByteArrayOutputStream()
        // TGA header (18 bytes)
        output.write(0)   // ID length
        output.write(0)   // color map type
        output.write(2)   // image type (uncompressed RGB)
        // color map spec (5 bytes, unused)
        repeat(5) { output.write(0) }
        // x-origin
        output.write(0); output.write(0)
        // y-origin
        output.write(0); output.write(0)
        // width
        output.write(width and 0xFF)
        output.write((width shr 8) and 0xFF)
        // height
        output.write(height and 0xFF)
        output.write((height shr 8) and 0xFF)
        // pixel depth
        output.write(24)
        // image descriptor (bit 5 set = top-left origin)
        output.write(0x20)

        // 像素数据 (BGR format, bottom-up)
        for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val b = pixel and 0xFF
                val g = (pixel shr 8) and 0xFF
                val r = (pixel shr 16) and 0xFF
                output.write(b)
                output.write(g)
                output.write(r)
            }
        }

        return output.toByteArray()
    }

    /**
     * 生成手环表盘 bin 包
     */
    fun buildWatchfaceBin(imagePath: String): ByteArray? {
        val bitmap = resizeForBand(imagePath) ?: return null
        val tgaData = bitmapToTGA(bitmap)
        bitmap.recycle()

        val manifest = """
        {"watchface":{"version":1,"width":192,"height":490,"type":"image"}}
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val output = ByteArrayOutputStream()
        // manifest length (4 bytes)
        val manifestLen = manifest.size
        output.write(manifestLen and 0xFF)
        output.write((manifestLen shr 8) and 0xFF)
        output.write((manifestLen shr 16) and 0xFF)
        output.write((manifestLen shr 24) and 0xFF)
        output.write(manifest)
        // tga data length
        val tgaLen = tgaData.size
        output.write(tgaLen and 0xFF)
        output.write((tgaLen shr 8) and 0xFF)
        output.write((tgaLen shr 16) and 0xFF)
        output.write((tgaLen shr 24) and 0xFF)
        output.write(tgaData)

        return output.toByteArray()
    }

    /**
     * 生成预览缩略图
     */
    fun createPreview(imagePath: String): Bitmap? {
        return resizeForBand(imagePath)
    }
}
