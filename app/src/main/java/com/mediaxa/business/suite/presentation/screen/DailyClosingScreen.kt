package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import com.mediaxa.business.suite.data.export.*
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyClosingScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val storeId = 1L // Default store isolation

    // Load active closings and today's summary metrics
    val profitToday by viewModel.profitSummaryToday.collectAsState()
    val cashFlowToday by viewModel.cashFlowToday.collectAsState()
    val uiMessage by viewModel.uiStateMessage.collectAsState()

    val lastClosingBalance by viewModel.latestClosingBalance.collectAsState()
    val todayTransactionCount by viewModel.todayTransactionCount.collectAsState()

    var openingBalanceStr by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    
    // Synchronize openingBalanceStr when lastClosingBalance changes
    LaunchedEffect(lastClosingBalance) {
        openingBalanceStr = lastClosingBalance.toString()
    }

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }

    // Retrieve previous closing balance to pre-populate opening balance
    LaunchedEffect(Unit) {
        viewModel.loadFinanceData(storeId)
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUiMessage()
        }
    }

    val openingBalance = openingBalanceStr.toDoubleOrNull() ?: 0.0
    val cashIn = cashFlowToday.cashBalances["CASH"]?.inflow ?: 0.0
    val cashOut = cashFlowToday.cashBalances["CASH"]?.outflow ?: 0.0
    val closingBalance = openingBalance + cashIn - cashOut

    val cashRevenue = cashFlowToday.cashBalances["CASH"]?.inflow ?: 0.0
    val qrisRevenue = cashFlowToday.cashBalances["QRIS"]?.inflow ?: 0.0
    val transferRevenue = cashFlowToday.cashBalances["TRANSFER"]?.inflow ?: 0.0

    val totalTransactions = todayTransactionCount
    val avgTicket = if (totalTransactions > 0) profitToday.revenue / totalTransactions else 0.0

    if (showExportSuccessDialog && exportResult != null) {
        ExportSuccessDialog(
            exportResult = exportResult!!,
            onDismissRequest = { showExportSuccessDialog = false },
            onOpenFileClick = {
                FileOpenHelper.openFile(context, exportResult!!.filePath, exportResult!!.format)
            },
            onShareFileClick = {
                FileShareHelper.shareFile(context, exportResult!!.filePath, exportResult!!.format)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutup Buku Harian") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE65100), // Dark Orange
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Opening Balance Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Saldo Awal & Kasir (CASH)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = openingBalanceStr,
                        onValueChange = { openingBalanceStr = it },
                        label = { Text("Saldo Awal Hari Ini (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // P&L Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Metrik Keuangan Hari Ini (${todayStr})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    MetricRow("Omzet / Pendapatan", rpFormatter.format(profitToday.revenue))
                    MetricRow("Harga Pokok Penjualan (HPP)", rpFormatter.format(profitToday.hpp))
                    Divider()
                    MetricRow("Laba Kotor (Gross Profit)", rpFormatter.format(profitToday.grossProfit), isBold = true)
                    MetricRow("Biaya Operasional", rpFormatter.format(profitToday.operationalExpense))
                    MetricRow("Biaya Waste (Spillage)", rpFormatter.format(profitToday.wasteCost))
                    Divider()
                    MetricRow(
                        "Laba Bersih (Net Profit)", 
                        rpFormatter.format(profitToday.netProfit), 
                        isBold = true,
                        color = if (profitToday.netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            // Payment Split & Orders Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ringkasan Transaksi & Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    MetricRow("Pembayaran Tunai (CASH)", rpFormatter.format(cashRevenue))
                    MetricRow("Pembayaran QRIS", rpFormatter.format(qrisRevenue))
                    MetricRow("Pembayaran Transfer Bank", rpFormatter.format(transferRevenue))
                    Divider()
                    MetricRow("Total Transaksi Sukses", "$totalTransactions Trx")
                    MetricRow("Rata-rata Keranjang (Average Ticket)", rpFormatter.format(avgTicket))
                }
            }

            // Closing Balance Formula Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) // Light orange
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Perhitungan Saldo Akhir Kas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    MetricRow("Saldo Awal (Cash):", rpFormatter.format(openingBalance))
                    MetricRow("Total Kas Masuk (Sales):", "+ " + rpFormatter.format(cashIn))
                    MetricRow("Total Kas Keluar (Expense):", "- " + rpFormatter.format(cashOut))
                    Divider()
                    MetricRow("Saldo Akhir (Closing Balance):", rpFormatter.format(closingBalance), isBold = true, color = Color(0xFFE65100))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showExportMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ekspor Laporan")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export PDF") },
                            onClick = {
                                showExportMenu = false
                                val reportText = buildReportText(todayStr, openingBalance, profitToday, cashFlowToday, totalTransactions, avgTicket, closingBalance)
                                val result = ReportExportService.exportReport(context, "Tutup Buku", todayStr, reportText, ExportFormat.PDF)
                                exportResult = result
                                showExportSuccessDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Excel (.xlsx)") },
                            onClick = {
                                showExportMenu = false
                                val reportText = buildReportText(todayStr, openingBalance, profitToday, cashFlowToday, totalTransactions, avgTicket, closingBalance)
                                val result = ReportExportService.exportReport(context, "Tutup Buku", todayStr, reportText, ExportFormat.EXCEL)
                                exportResult = result
                                showExportSuccessDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export CSV") },
                            onClick = {
                                showExportMenu = false
                                val reportText = buildReportText(todayStr, openingBalance, profitToday, cashFlowToday, totalTransactions, avgTicket, closingBalance)
                                val result = ReportExportService.exportReport(context, "Tutup Buku", todayStr, reportText, ExportFormat.CSV)
                                exportResult = result
                                showExportSuccessDialog = true
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Mencetak Ringkasan Tutup Buku Hari Ini...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Print Summary")
                }
            }

            Button(
                onClick = {
                    viewModel.performDailyClosing(
                        storeId = storeId,
                        openingBalance = openingBalance,
                        userUuid = "user-admin",
                        onSuccess = {
                            onBackClick()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan & Tutup Buku Harian", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    val isDark = isSystemInDarkTheme()
    val displayColor = remember(color, isDark) {
        if (color == Color.Unspecified) {
            Color.Unspecified
        } else {
            when (color) {
                Color(0xFF2E7D32) -> if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32)
                Color(0xFFC62828) -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFC62828)
                Color(0xFFE65100) -> if (isDark) Color(0xFFFB923C) else Color(0xFFE65100)
                else -> color
            }
        }
    }

    val finalColor = if (displayColor != Color.Unspecified) {
        displayColor
    } else {
        if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = finalColor
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

private fun getStartOfDay(timeMs: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMs
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun getEndOfDay(timeMs: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMs
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}

private fun buildReportText(
    dateStr: String,
    opening: Double,
    pSummary: com.mediaxa.business.suite.data.repository.ProfitSummary,
    cFlow: com.mediaxa.business.suite.data.repository.CashFlowReport,
    trxCount: Int,
    avgTicket: Double,
    closing: Double
): String {
    return """
        ==================================================
                 LAPORAN TUTUP BUKU HARIAN (DAILY CLOSING)
        ==================================================
        Tanggal Laporan    : $dateStr
        
        FINANSIAL P&L:
        - Omzet            : Rp ${pSummary.revenue}
        - HPP              : Rp ${pSummary.hpp}
        - Laba Kotor       : Rp ${pSummary.grossProfit}
        - Biaya Operasional: Rp ${pSummary.operationalExpense}
        - Biaya Waste      : Rp ${pSummary.wasteCost}
        - Laba Bersih      : Rp ${pSummary.netProfit}
        
        KASIR & ARUS KAS:
        - Saldo Awal (Cash): Rp $opening
        - Kas Masuk (Tunai): Rp ${cFlow.cashBalances["CASH"]?.inflow ?: 0.0}
        - Kas Keluar (Tunai): Rp ${cFlow.cashBalances["CASH"]?.outflow ?: 0.0}
        - Saldo Akhir      : Rp $closing
        
        METODE PEMBAYARAN LAIN:
        - QRIS             : Rp ${cFlow.cashBalances["QRIS"]?.inflow ?: 0.0}
        - Transfer Bank    : Rp ${cFlow.cashBalances["TRANSFER"]?.inflow ?: 0.0}
        
        STATISTIK TRANSAKSI:
        - Total Transaksi  : $trxCount
        - Rata-rata Keranjang: Rp $avgTicket
        ==================================================
    """.trimIndent()
}
