package com.kasirinaja.admin.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.MultiFormatWriter

object BarcodeUtils {
    fun generateBarcode(text: String, width: Int, height: Int): Bitmap? {
        if (text.isEmpty()) return null
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.CODE_128,
                width,
                height
            )
            val barcodeHeight = height - 40 // Leave space for text
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 32f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            for (x in 0 until width) {
                for (y in 0 until barcodeHeight) {
                    if (bitMatrix[x, y]) {
                        bmp.setPixel(x, y, Color.BLACK)
                    }
                }
            }

            canvas.drawText(text, width / 2f, height - 10f, paint)
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBarcodeToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Barcode_$fileName.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/KasirinAja")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(it, null, null)
                return null
            }
        }
        return uri
    }
}
