package com.kasirinaja.store.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kasirinaja.store.data.local.ReportItem
import jxl.Workbook
import jxl.write.Label
import jxl.write.WritableWorkbook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportUtil {

    suspend fun exportToPdf(context: Context, items: List<ReportItem>, startDate: Long, endDate: Long, totalRevenue: Double, totalProfit: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint()
            paint.color = Color.BLACK
            paint.textSize = 14f

            val titlePaint = Paint()
            titlePaint.color = Color.BLACK
            titlePaint.textSize = 18f
            titlePaint.isFakeBoldText = true

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startStr = dateFormat.format(Date(startDate))
            val endStr = dateFormat.format(Date(endDate))
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

            var yPosition = 50f
            canvas.drawText("Laporan Penjualan POS Kedai", 50f, yPosition, titlePaint)
            yPosition += 25f
            paint.textSize = 12f
            canvas.drawText("Periode: $startStr - $endStr", 50f, yPosition, paint)
            yPosition += 20f
            canvas.drawText("Total Pendapatan Kotor: ${currencyFormat.format(totalRevenue).replace("Rp", "Rp ")}", 50f, yPosition, paint)
            yPosition += 20f
            canvas.drawText("Total Pendapatan Bersih: ${currencyFormat.format(totalProfit).replace("Rp", "Rp ")}", 50f, yPosition, paint)
            yPosition += 30f

            // Table Header
            paint.isFakeBoldText = true
            canvas.drawText("No", 50f, yPosition, paint)
            canvas.drawText("Nama Produk", 80f, yPosition, paint)
            canvas.drawText("Qty", 250f, yPosition, paint)
            canvas.drawText("H. Beli", 290f, yPosition, paint)
            canvas.drawText("H. Jual", 360f, yPosition, paint)
            canvas.drawText("T. Kotor", 430f, yPosition, paint)
            canvas.drawText("T. Bersih", 510f, yPosition, paint)

            yPosition += 10f
            canvas.drawLine(50f, yPosition, 565f, yPosition, paint)
            yPosition += 20f

            paint.isFakeBoldText = false

            items.forEachIndexed { index, item ->
                if (yPosition > 800f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }

                // Substring name to prevent overflow
                val name = if (item.productName.length > 20) item.productName.substring(0, 18) + ".." else item.productName

                canvas.drawText("${index + 1}", 50f, yPosition, paint)
                canvas.drawText(name, 80f, yPosition, paint)
                canvas.drawText("${item.quantitySold}", 250f, yPosition, paint)
                canvas.drawText(currencyFormat.format(item.buyPrice).replace("Rp", ""), 290f, yPosition, paint)
                canvas.drawText(currencyFormat.format(item.sellPrice).replace("Rp", ""), 360f, yPosition, paint)
                canvas.drawText(currencyFormat.format(item.productTotalRevenue).replace("Rp", ""), 430f, yPosition, paint)
                canvas.drawText(currencyFormat.format(item.productTotalProfit).replace("Rp", ""), 510f, yPosition, paint)
                yPosition += 20f
            }

            document.finishPage(page)

            val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.pdf"
            val outputStream = getOutputStream(context, fileName, "application/pdf")
            if (outputStream != null) {
                document.writeTo(outputStream)
                document.close()
                outputStream.close()
                return@withContext true
            } else {
                document.close()
                return@withContext false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun exportToXlsx(context: Context, items: List<ReportItem>, startDate: Long, endDate: Long, totalRevenue: Double, totalProfit: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.xls"
            val outputStream = getOutputStream(context, fileName, "application/vnd.ms-excel") ?: return@withContext false

            val workbook: WritableWorkbook = Workbook.createWorkbook(outputStream)
            val sheet = workbook.createSheet("Laporan Penjualan", 0)

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startStr = dateFormat.format(Date(startDate))
            val endStr = dateFormat.format(Date(endDate))

            sheet.addCell(Label(0, 0, "Laporan Penjualan POS Kedai"))
            sheet.addCell(Label(0, 1, "Periode: $startStr - $endStr"))
            sheet.addCell(Label(0, 2, "Total Pendapatan Kotor"))
            sheet.addCell(jxl.write.Number(1, 2, totalRevenue))
            sheet.addCell(Label(0, 3, "Total Pendapatan Bersih"))
            sheet.addCell(jxl.write.Number(1, 3, totalProfit))

            val headers = listOf("No", "Nama Produk", "Qty", "Harga Beli", "Harga Jual", "Total Kotor", "Total Bersih")
            headers.forEachIndexed { index, header ->
                sheet.addCell(Label(index, 5, header))
            }

            items.forEachIndexed { index, item ->
                val row = index + 6
                sheet.addCell(jxl.write.Number(0, row, (index + 1).toDouble()))
                sheet.addCell(Label(1, row, item.productName))
                sheet.addCell(jxl.write.Number(2, row, item.quantitySold.toDouble()))
                sheet.addCell(jxl.write.Number(3, row, item.buyPrice))
                sheet.addCell(jxl.write.Number(4, row, item.sellPrice))
                sheet.addCell(jxl.write.Number(5, row, item.productTotalRevenue))
                sheet.addCell(jxl.write.Number(6, row, item.productTotalProfit))
            }

            workbook.write()
            workbook.close()
            outputStream.close()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun getOutputStream(context: Context, fileName: String, mimeType: String): OutputStream? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/pos kedai")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            return uri?.let { resolver.openOutputStream(it) }
        } else {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val posDir = java.io.File(downloadDir, "pos kedai")
            if (!posDir.exists()) {
                posDir.mkdirs()
            }
            val file = java.io.File(posDir, fileName)
            return java.io.FileOutputStream(file)
        }
    }
}
