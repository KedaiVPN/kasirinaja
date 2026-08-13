import re

with open('kasir-android/app-store/src/main/java/com/kasirinaja/store/utils/ReportExportUtil.kt', 'r') as f:
    content = f.read()

import_str = """import jxl.Workbook
import jxl.write.Label
import jxl.write.WritableWorkbook"""

new_import_str = """import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellType"""

content = content.replace(import_str, new_import_str)

xlsx_func = """    suspend fun exportToXlsx(context: Context, items: List<ReportItem>, startDate: Long, endDate: Long, totalRevenue: Double, totalProfit: Double): Boolean = withContext(Dispatchers.IO) {
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
    }"""

new_xlsx_func = """    suspend fun exportToXlsx(context: Context, items: List<ReportItem>, startDate: Long, endDate: Long, totalRevenue: Double, totalProfit: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.xlsx"
            val outputStream = getOutputStream(context, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ?: return@withContext false

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Laporan Penjualan")

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startStr = dateFormat.format(Date(startDate))
            val endStr = dateFormat.format(Date(endDate))

            var rowIndex = 0
            sheet.createRow(rowIndex++).createCell(0, CellType.STRING).setCellValue("Laporan Penjualan POS Kedai")
            sheet.createRow(rowIndex++).createCell(0, CellType.STRING).setCellValue("Periode: $startStr - $endStr")

            val r3 = sheet.createRow(rowIndex++)
            r3.createCell(0, CellType.STRING).setCellValue("Total Pendapatan Kotor")
            r3.createCell(1, CellType.NUMERIC).setCellValue(totalRevenue)

            val r4 = sheet.createRow(rowIndex++)
            r4.createCell(0, CellType.STRING).setCellValue("Total Pendapatan Bersih")
            r4.createCell(1, CellType.NUMERIC).setCellValue(totalProfit)

            rowIndex++ // Empty row

            val headers = listOf("No", "Nama Produk", "Qty", "Harga Beli", "Harga Jual", "Total Kotor", "Total Bersih")
            val headerRow = sheet.createRow(rowIndex++)
            headers.forEachIndexed { index, header ->
                headerRow.createCell(index, CellType.STRING).setCellValue(header)
            }

            items.forEachIndexed { index, item ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0, CellType.NUMERIC).setCellValue((index + 1).toDouble())
                row.createCell(1, CellType.STRING).setCellValue(item.productName)
                row.createCell(2, CellType.NUMERIC).setCellValue(item.quantitySold.toDouble())
                row.createCell(3, CellType.NUMERIC).setCellValue(item.buyPrice)
                row.createCell(4, CellType.NUMERIC).setCellValue(item.sellPrice)
                row.createCell(5, CellType.NUMERIC).setCellValue(item.productTotalRevenue)
                row.createCell(6, CellType.NUMERIC).setCellValue(item.productTotalProfit)
            }

            workbook.write(outputStream)
            workbook.close()
            outputStream.close()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }"""

content = content.replace(xlsx_func, new_xlsx_func)


with open('kasir-android/app-store/src/main/java/com/kasirinaja/store/utils/ReportExportUtil.kt', 'w') as f:
    f.write(content)
