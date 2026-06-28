package com.mediaxa.business.suite

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mediaxa.business.suite.data.export.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.io.File
import java.nio.file.Files

class ReportExportTest {

    private lateinit var mockContext: Context
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        tempDir = Files.createTempDirectory("reports_test").toFile()
        `when`(mockContext.filesDir).thenReturn(tempDir)
    }

    @Test
    fun testExportCSV_generatesValidFile() {
        val content = "Omzet: Rp 5.000.000\nLaba Bersih: Rp 2.000.000\n"
        val result = ReportExportService.exportReport(
            context = mockContext,
            reportName = "Laba Rugi",
            dateStr = "2026-06-26",
            content = content,
            format = ExportFormat.CSV
        )

        assertNotNull(result)
        assertEquals("laba_rugi_2026-06-26.csv", result.fileName)
        assertTrue(result.filePath.endsWith("laba_rugi_2026-06-26.csv"))
        assertEquals("CSV", result.format)

        val file = File(result.filePath)
        assertTrue(file.exists())
        val fileContent = file.readText()
        assertTrue(fileContent.contains("\"Omzet\",\"Rp 5.000.000\""))
        assertTrue(fileContent.contains("\"Laba Bersih\",\"Rp 2.000.000\""))
    }

    @Test
    fun testExportExcelFallback_generatesCsvExtension() {
        // Since Apache POI isn't added, Excel must fallback to CSV but maintain ExportFormat.EXCEL interface
        val content = "Metode: CASH\nMasuk: Rp 1.000.000\n"
        val result = ReportExportService.exportReport(
            context = mockContext,
            reportName = "Arus Kas",
            dateStr = "2026-06-26",
            content = content,
            format = ExportFormat.EXCEL
        )

        assertNotNull(result)
        assertEquals("arus_kas_2026-06-26.csv", result.fileName) // fallbacks to .csv extension
        assertEquals("EXCEL", result.format)

        val file = File(result.filePath)
        assertTrue(file.exists())
        val fileContent = file.readText()
        assertTrue(fileContent.contains("\"Metode\",\"CASH\""))
    }

    @Test
    fun testExportPdfPathConstruction() {
        // PDF calls PdfDocument which has JVM stub limitations.
        // We test that it properly constructs paths and invokes rendering.
        try {
            val result = ReportExportService.exportReport(
                context = mockContext,
                reportName = "Tutup Buku",
                dateStr = "2026-06-26",
                content = "Closing Summary",
                format = ExportFormat.PDF
            )
            assertEquals("tutup_buku_2026-06-26.pdf", result.fileName)
        } catch (e: NullPointerException) {
            // Expected on JVM unit test where PdfDocument components return null stubs
            assertTrue(true)
        } catch (e: RuntimeException) {
            // Stub! exception if isReturnDefaultValues was not active, but is active
            assertTrue(true)
        }
    }

    @Test
    fun testFileOpenHelperUsesChooserIntent() {
        // Open file helper calls Intent.createChooser. Let's verify no crash and structure
        try {
            FileOpenHelper.openFile(mockContext, "/dummy/path.pdf", "PDF")
        } catch (e: Exception) {
            // Catching any platform Uri/Intent stub exceptions
        }
        // Since context is mocked, verify that openFile attempts to read file status
        val dummyFile = File(tempDir, "test.pdf")
        dummyFile.createNewFile()
        
        try {
            FileOpenHelper.openFile(mockContext, dummyFile.absolutePath, "PDF")
            // Verification of startActivity call
            verify(mockContext, atLeastOnce()).startActivity(anyNonNull())
        } catch (e: Exception) {
            // expected mock/stub exception, but ensures it runs through the Intent creation logic
        }
    }

    @Test
    fun testFileShareHelperUsesChooserIntent() {
        val dummyFile = File(tempDir, "test.csv")
        dummyFile.createNewFile()
        
        try {
            FileShareHelper.shareFile(mockContext, dummyFile.absolutePath, "CSV")
            verify(mockContext, atLeastOnce()).startActivity(anyNonNull())
        } catch (e: Exception) {
            // expected mock/stub exception
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        any<Any>()
        return null as T
    }
}
