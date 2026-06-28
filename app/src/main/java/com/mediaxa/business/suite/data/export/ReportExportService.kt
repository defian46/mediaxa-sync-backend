package com.mediaxa.business.suite.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File

object ReportExportService {
    
    fun exportReport(
        context: Context,
        reportName: String,
        dateStr: String,
        content: String,
        format: ExportFormat
    ): ExportResult {
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()

        val extension = when (format) {
            ExportFormat.PDF -> "pdf"
            ExportFormat.EXCEL -> "csv" // Fallback to CSV for Excel
            ExportFormat.CSV -> "csv"
        }

        val cleanReportName = reportName.lowercase().replace(" ", "_")
        val fileName = "${cleanReportName}_$dateStr.$extension"
        val file = File(dir, fileName)

        when (format) {
            ExportFormat.PDF -> {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                val paint = Paint().apply {
                    textSize = 12f
                    color = Color.BLACK
                }
                
                var y = 40f
                val lines = content.split("\n")
                for (line in lines) {
                    canvas.drawText(line, 40f, y, paint)
                    y += 18f
                    if (y > 800f) break // Simple limit
                }
                
                pdfDocument.finishPage(page)
                file.outputStream().use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()
            }
            ExportFormat.CSV, ExportFormat.EXCEL -> {
                val csvBuilder = StringBuilder()
                val lines = content.split("\n")
                for (line in lines) {
                    if (line.contains(":")) {
                        val parts = line.split(":", limit = 2)
                        val key = parts[0].trim().replace("\"", "\"\"")
                        val value = parts[1].trim().replace("\"", "\"\"")
                        csvBuilder.append("\"$key\",\"$value\"\n")
                    } else {
                        val cleanLine = line.trim().replace("\"", "\"\"")
                        if (cleanLine.isNotEmpty()) {
                            csvBuilder.append("\"$cleanLine\"\n")
                        }
                    }
                }
                file.writeText(csvBuilder.toString())
            }
        }

        return ExportResult(
            fileName = file.name,
            filePath = file.absolutePath,
            format = format.name,
            sizeBytes = file.length()
        )
    }
}
