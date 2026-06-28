package com.mediaxa.business.suite.presentation.screen

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageHelper {

    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val directory = File(context.filesDir, "menu_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "menu_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveExpenseAttachment(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val directory = File(context.filesDir, "expense_attachments")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "expense_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
