package com.kasirinaja.store.utils

import android.graphics.Bitmap
import android.graphics.Color
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
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
