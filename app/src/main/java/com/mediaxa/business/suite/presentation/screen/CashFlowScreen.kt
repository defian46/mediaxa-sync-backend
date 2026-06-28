package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.export.*
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val cashFlowMonth by viewModel.cashFlowMonth.collectAsState()
    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Arus Kas (Cash Flow)") },
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
                                    val content = generateCashFlowText(cashFlowMonth, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Arus Kas", "arus_kas", content, ExportFormat.PDF)
                                    exportResult = result
                                    showSuccessDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Excel (.xlsx)") },
                                onClick = {
                                    showMenu = false
                                    val content = generateCashFlowText(cashFlowMonth, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Arus Kas", "arus_kas", content, ExportFormat.EXCEL)
                                    exportResult = result
                                    showSuccessDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    showMenu = false
                                    val content = generateCashFlowText(cashFlowMonth, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Arus Kas", "arus_kas", content, ExportFormat.CSV)
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
                "Ringkasan Arus Kas Masuk & Keluar",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // General Cash Flow Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Arus Kas Masuk (Cash In)")
                        Text(rpFormatter.format(cashFlowMonth.totalInflow), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Arus Kas Keluar (Cash Out)")
                        Text("- ${rpFormatter.format(cashFlowMonth.totalOutflow)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }

                    Divider(thickness = 2.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Cash Flow", fontWeight = FontWeight.Bold)
                        val color = if (cashFlowMonth.netCashFlow >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        Text(rpFormatter.format(cashFlowMonth.netCashFlow), color = color, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Saldo Berdasarkan Metode Pembayaran",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Balance by payment method details
            cashFlowMonth.cashBalances.values.forEach { balance ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(balance.paymentMethod, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            val isDark = isSystemInDarkTheme()
                            val balanceColor = if (balance.balance >= 0) {
                                if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32)
                            } else {
                                if (isDark) Color(0xFFFCA5A5) else Color(0xFFC62828)
                            }
                            Text(
                                "Saldo: ${rpFormatter.format(balance.balance)}",
                                fontWeight = FontWeight.Bold,
                                color = balanceColor,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Masuk (Inflow): ${rpFormatter.format(balance.inflow)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Keluar (Outflow): ${rpFormatter.format(balance.outflow)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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

private fun generateCashFlowText(
    cashFlow: com.mediaxa.business.suite.data.repository.CashFlowReport,
    rpFormatter: java.text.NumberFormat
): String {
    val sb = StringBuilder()
    sb.append("Laporan Arus Kas (Cash Flow) - Bulan Ini\n")
    sb.append("=========================================\n")
    sb.append("Total Kas Masuk (Inflow) : ${rpFormatter.format(cashFlow.totalInflow)}\n")
    sb.append("Total Kas Keluar (Outflow): -${rpFormatter.format(cashFlow.totalOutflow)}\n")
    sb.append("Net Cash Flow            : ${rpFormatter.format(cashFlow.netCashFlow)}\n")
    sb.append("=========================================\n\n")
    sb.append("Rincian per Metode Pembayaran:\n")
    for ((method, balance) in cashFlow.cashBalances) {
        sb.append("- $method : Masuk ${rpFormatter.format(balance.inflow)} | Keluar ${rpFormatter.format(balance.outflow)} | Saldo ${rpFormatter.format(balance.balance)}\n")
    }
    return sb.toString()
}
