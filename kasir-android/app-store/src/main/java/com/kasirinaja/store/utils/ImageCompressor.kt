package com.kasirinaja.store.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageCompressor {
    fun compressImageFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Scale down if image is too large (max 512x512)
            val maxSize = 512
            var width = bitmap.width
            var height = bitmap.height

            if (width > maxSize || height > maxSize) {
                val ratio = width.toFloat() / height.toFloat()
                if (ratio > 1) {
                    width = maxSize
                    height = (width / ratio).toInt()
                } else {
                    height = maxSize
                    width = (height * ratio).toInt()
                }
            }

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

            // Compress to JPEG 60%
            val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            outputStream.flush()
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
