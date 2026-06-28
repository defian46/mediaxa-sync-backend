package com.mediaxa.business.suite.presentation.screen

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.LocalContext
import com.mediaxa.business.suite.data.export.*
import com.mediaxa.business.suite.data.local.dao.CategoryExpenseResult
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceExpenseScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Pengeluaran Operasional") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Download, contentDescription = "Export Laporan", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export PDF") },
                                onClick = {
                                    showMenu = false
                                    val content = generateExpenseReportText(categoryExpenses, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Pengeluaran", "pengeluaran", content, ExportFormat.PDF)
                                    exportResult = result
                                    showSuccessDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Excel (.xlsx)") },
                                onClick = {
                                    showMenu = false
                                    val content = generateExpenseReportText(categoryExpenses, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Pengeluaran", "pengeluaran", content, ExportFormat.EXCEL)
                                    exportResult = result
                                    showSuccessDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    showMenu = false
                                    val content = generateExpenseReportText(categoryExpenses, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Pengeluaran", "pengeluaran", content, ExportFormat.CSV)
                                    exportResult = result
                                    showSuccessDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Distribusi Pengeluaran Per Kategori",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Canvas Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pengeluaran Terbesar Bulan Ini", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    ExpenseBarChart(
                        data = categoryExpenses,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Rincian Pengeluaran",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Category list table
            categoryExpenses.forEach { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // Map enum category code to user-friendly label
                            val label = when (cat.category) {
                                "INVENTORY_PURCHASE" -> "Belanja Bahan Baku (Stok)"
                                "RENT" -> "Sewa Ruko / Tempat"
                                "ELECTRICITY" -> "Listrik"
                                "WATER" -> "Air"
                                "SALARY" -> "Gaji Karyawan"
                                "MARKETING" -> "Pemasaran / Iklan"
                                "TRANSPORT" -> "Transportasi / Bensin"
                                "MAINTENANCE" -> "Pemeliharaan / Perbaikan"
                                "CLEANING" -> "Kebersihan / Sampah"
                                "OTHER" -> "Pengeluaran Lainnya"
                                else -> cat.category
                            }
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (cat.category == "INVENTORY_PURCHASE") {
                                Text("Aset Stok (Ditangguhkan dari P&L)", fontSize = 10.sp, color = Color.Gray)
                            } else {
                                Text("Biaya Operasional Langsung", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Text(rpFormatter.format(cat.totalAmount), fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }
        }
    }

    if (showSuccessDialog && exportResult != null) {
        ExportSuccessDialog(
            exportResult = exportResult!!,
            onDismissRequest = { showSuccessDialog = false },
            onOpenFileClick = {
                FileOpenHelper.openFile(context, exportResult!!.filePath, exportResult!!.format)
            },
            onShareFileClick = {
                FileShareHelper.shareFile(context, exportResult!!.filePath, exportResult!!.format)
            }
        )
    }
}

private fun generateExpenseReportText(
    categoryExpenses: List<CategoryExpenseResult>,
    rpFormatter: NumberFormat
): String {
    val sb = StringBuilder()
    sb.append("Laporan Pengeluaran Operasional\n")
    sb.append("=========================================\n")
    for (cat in categoryExpenses) {
        val label = when (cat.category) {
            "INVENTORY_PURCHASE" -> "Belanja Bahan Baku (Stok)"
            "RENT" -> "Sewa Ruko / Tempat"
            "ELECTRICITY" -> "Listrik"
            "WATER" -> "Air"
            "SALARY" -> "Gaji Karyawan"
            "MARKETING" -> "Pemasaran / Iklan"
            "TRANSPORT" -> "Transportasi / Bensin"
            "MAINTENANCE" -> "Pemeliharaan / Perbaikan"
            "CLEANING" -> "Kebersihan / Sampah"
            "OTHER" -> "Pengeluaran Lainnya"
            else -> cat.category
        }
        sb.append("- $label : ${rpFormatter.format(cat.totalAmount)}\n")
    }
    sb.append("=========================================\n")
    return sb.toString()
}

@Composable
fun ExpenseBarChart(
    data: List<CategoryExpenseResult>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tidak ada data pengeluaran", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val maxVal = data.maxOf { it.totalAmount }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 40.dp.toPx()
        val paddingRight = 10.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingBottom = 40.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Draw horizontal grid lines (3 Y-axis markers)
        val gridCount = 2
        for (i in 0..gridCount) {
            val y = paddingTop + chartHeight * (i.toFloat() / gridCount)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.2f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw Bars
        val barCount = data.size.coerceAtMost(5)
        val gap = 16.dp.toPx()
        val totalGaps = (barCount - 1) * gap
        val barWidth = (chartWidth - totalGaps) / barCount

        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 8.dp.toPx()
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until barCount) {
            val item = data[i]
            val barHeight = chartHeight * (item.totalAmount.toFloat() / maxVal.toFloat())
            val x = paddingLeft + i * (barWidth + gap)
            val y = paddingTop + chartHeight - barHeight

            drawRect(
                color = if (item.category == "INVENTORY_PURCHASE") Color(0xFF1565C0) else Color(0xFFC62828),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            // Draw category initials / shorthand label
            val initial = item.category.take(4).uppercase()
            drawContext.canvas.nativeCanvas.drawText(
                initial,
                x + barWidth / 2,
                height - 15.dp.toPx(),
                paint
            )
        }
    }
}
