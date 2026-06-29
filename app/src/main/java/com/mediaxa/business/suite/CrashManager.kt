package com.mediaxa.business.suite

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object CrashManager {
    private const val TAG = "CrashManager"
    private const val CRASH_FILE_NAME = "crash_logs.txt"
    private var isInitialized = false

    fun init(context: Context, backendUrl: String = "http://localhost:3000/api/v1/crashes") {
        if (isInitialized) return
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            
            val logMessage = """
                Timestamp: ${System.currentTimeMillis()}
                Thread: ${thread.name}
                Stacktrace:
                $stackTrace
                ========================================
                
            """.trimIndent()

            // Save exception locally
            saveCrashLocal(context, logMessage)

            // Attempt synchronous upload to cloud endpoint
            uploadCrashSync(backendUrl, logMessage)

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLocal(context: Context, log: String) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            FileWriter(file, true).use { writer ->
                writer.append(log)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write local crash log", e)
        }
    }

    private fun uploadCrashSync(backendUrl: String, log: String) {
        // Trigger synchronous HTTP connection
        val thread = thread(start = true) {
            try {
                val url = URL(backendUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonPayload = """{"log": ${escapeJsonString(log)}}"""
                conn.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray())
                    os.flush()
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload crash log sync", e)
            }
        }
        // Give connection up to 1500ms to complete before OS terminates process
        try {
            thread.join(1500)
        } catch (_: Exception) {}
    }

    fun logException(context: Context, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        
        val logMessage = """
            Timestamp: ${System.currentTimeMillis()}
            Type: LOGGED_EXCEPTION
            Stacktrace:
            $stackTrace
            ========================================
            
        """.trimIndent()

        saveCrashLocal(context, logMessage)
        
        val backendUrl = com.mediaxa.business.suite.data.remote.NetworkClient.baseUrl + "/crashes"
        uploadCrashSync(backendUrl, logMessage)
    }

    private fun escapeJsonString(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .let { "\"$it\"" }
    }
}
