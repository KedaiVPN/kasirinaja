package com.poskedai.store.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.poskedai.store.data.local.ReportItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExportUtil {

    suspend fun exportToPdf(context: Context, items: List<ReportItem>, startDate: Long, endDate: Long, totalRevenue: Double, totalProfit: Double, cashierName: String = ""): Boolean = withContext(Dispatchers.IO) {
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
            if (cashierName.isNotEmpty()) {
                canvas.drawText("Kasir: $cashierName", 50f, yPosition, paint)
                yPosition += 20f
            }
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
            val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.xlsx"
            val outputStream = getOutputStream(context, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ?: return@withContext false

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startStr = dateFormat.format(Date(startDate))
            val endStr = dateFormat.format(Date(endDate))

            ZipOutputStream(outputStream).use { zip ->
                putZipEntry(zip, "[Content_Types].xml", buildContentTypesXml())
                putZipEntry(zip, "_rels/.rels", buildRootRelsXml())
                putZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
                putZipEntry(zip, "xl/workbook.xml", buildWorkbookXml())
                putZipEntry(zip, "xl/worksheets/sheet1.xml", buildSheetXml(items, startStr, endStr, totalRevenue, totalProfit))
                zip.finish()
            }

            outputStream.close()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Laporan Penjualan" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
    }

    private fun buildSheetXml(
        items: List<ReportItem>,
        startStr: String,
        endStr: String,
        totalRevenue: Double,
        totalProfit: Double
    ): String {
        val sb = StringBuilder(8192)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        sb.append("  <sheetData>\n")

        // Title & summary rows
        sb.append("    <row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Laporan Penjualan POS Kedai</t></is></c></row>\n")
        sb.append("    <row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>${escapeXml("Periode: $startStr - $endStr")}</t></is></c></row>\n")
        sb.append("    <row r=\"3\"><c r=\"A3\" t=\"inlineStr\"><is><t>Total Pendapatan Kotor</t></is></c><c r=\"B3\"><v>${formatNumber(totalRevenue)}</v></c></row>\n")
        sb.append("    <row r=\"4\"><c r=\"A4\" t=\"inlineStr\"><is><t>Total Pendapatan Bersih</t></is></c><c r=\"B4\"><v>${formatNumber(totalProfit)}</v></c></row>\n")

        // Header row
        val headers = listOf("No", "Nama Produk", "Qty", "Harga Beli", "Harga Jual", "Total Kotor", "Total Bersih")
        sb.append("    <row r=\"6\">")
        headers.forEachIndexed { index, header ->
            val col = columnLetter(index)
            sb.append("<c r=\"${col}6\" t=\"inlineStr\"><is><t>${escapeXml(header)}</t></is></c>")
        }
        sb.append("</row>\n")

        // Data rows
        items.forEachIndexed { index, item ->
            val row = index + 7
            sb.append("    <row r=\"$row\">")
            sb.append("<c r=\"A$row\"><v>${index + 1}</v></c>")
            sb.append("<c r=\"B$row\" t=\"inlineStr\"><is><t>${escapeXml(item.productName)}</t></is></c>")
            sb.append("<c r=\"C$row\"><v>${item.quantitySold}</v></c>")
            sb.append("<c r=\"D$row\"><v>${formatNumber(item.buyPrice)}</v></c>")
            sb.append("<c r=\"E$row\"><v>${formatNumber(item.sellPrice)}</v></c>")
            sb.append("<c r=\"F$row\"><v>${formatNumber(item.productTotalRevenue)}</v></c>")
            sb.append("<c r=\"G$row\"><v>${formatNumber(item.productTotalProfit)}</v></c>")
            sb.append("</row>\n")
        }

        sb.append("  </sheetData>\n")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun columnLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A'.code + (i % 26)).toChar())
            i = i / 26 - 1
        }
        return sb.toString()
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatNumber(value: Double): String {
        val bd = BigDecimal.valueOf(value)
        return if (bd.stripTrailingZeros().scale() <= 0) {
            bd.toBigInteger().toString()
        } else {
            bd.stripTrailingZeros().toPlainString()
        }
    }

    private fun putZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
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
