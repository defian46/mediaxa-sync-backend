package com.mediaxa.business.suite.data.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileShareHelper {
    fun shareFile(context: Context, filePath: String, formatStr: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "com.mediaxa.business.suite.fileprovider",
                file
            )

            val mimeType = when (formatStr.uppercase()) {
                "PDF" -> "application/pdf"
                "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "CSV" -> "text/csv"
                else -> "*/*"
            }

            val finalMimeType = if (file.extension == "csv") "text/csv" else mimeType

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = finalMimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Bagikan laporan")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
