package com.band7.tool.util

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object EBookConverter {

    /**
     * 将 TXT 文件转换为手环电子书格式
     */
    fun convertToEBook(content: ByteArray, charset: Charset = Charsets.UTF_8): ByteArray {
        val text = try {
            content.toString(charset)
        } catch (e: Exception) {
            content.toString(Charsets.UTF_8)
        }

        val maxSegment = 4096
        val segments = mutableListOf<ByteArray>()

        var start = 0
        while (start < text.length) {
            val end = minOf(start + maxSegment, text.length)
            segments.add(text.substring(start, end).toByteArray(Charsets.UTF_8))
            start = end
        }

        val output = ByteArrayOutputStream()
        // 段数
        val segCount = segments.size
        output.write(segCount and 0xFF)
        output.write((segCount shr 8) and 0xFF)
        output.write((segCount shr 16) and 0xFF)
        output.write((segCount shr 24) and 0xFF)

        for (seg in segments) {
            output.write(seg.size and 0xFF)
            output.write((seg.size shr 8) and 0xFF)
            output.write((seg.size shr 16) and 0xFF)
            output.write((seg.size shr 24) and 0xFF)
            output.write(seg)
        }

        return output.toByteArray()
    }
}
