package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.export.*
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossReportScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val profitMonth by viewModel.profitSummaryMonth.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val greenColor = if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32)
    val redColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFC62828)
    val blueColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1565C0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Laba Rugi (P&L)") },
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
                                    val content = generateProfitLossText(profitMonth, categoryExpenses, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Laba Rugi", "laba_rugi", content, ExportFormat.PDF)
                                    exportResult = result
                                    showSuccessDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Excel (.xlsx)") },
                                onClick = {
                                    showMenu = false
                                    val content = generateProfitLossText(profitMonth, categoryExpenses, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Laba Rugi", "laba_rugi", content, ExportFormat.EXCEL)
                                    exportResult = result
                                    showSuccessDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    showMenu = false
                                    val content = generateProfitLossText(profitMonth, categoryExpenses, rpFormatter)
                                    val result = ReportExportService.exportReport(context, "Laba Rugi", "laba_rugi", content, ExportFormat.CSV)
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
                "Laporan Laba Rugi - Bulan Ini",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // PL Table Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // REVENUE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1. Pendapatan (Omzet)", fontWeight = FontWeight.Bold)
                        Text(rpFormatter.format(profitMonth.revenue), fontWeight = FontWeight.Bold)
                    }

                    // HPP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2. Harga Pokok Penjualan (HPP)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- ${rpFormatter.format(profitMonth.hpp)}", color = redColor)
                    }
 
                    Divider()
 
                    // GROSS PROFIT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Laba Kotor (Gross Profit)", fontWeight = FontWeight.Bold, color = greenColor)
                        Text(rpFormatter.format(profitMonth.grossProfit), fontWeight = FontWeight.Bold, color = greenColor)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // OPERATIONAL EXPENSES HEADER
                    Text("3. Biaya Operasional", fontWeight = FontWeight.Bold)

                    // Operational Breakdown
                    val opExpenses = categoryExpenses.filter { it.category != "INVENTORY_PURCHASE" }
                    if (opExpenses.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tidak ada biaya operasional", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rp0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        opExpenses.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("- ${rpFormatter.format(cat.totalAmount)}", fontSize = 12.sp, color = redColor)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Biaya Operasional", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("- ${rpFormatter.format(profitMonth.operationalExpense)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = redColor)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // WASTE COST
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("4. Biaya Kerusakan Bahan (Waste)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- ${rpFormatter.format(profitMonth.wasteCost)}", color = redColor)
                    }

                    Divider(thickness = 2.dp)

                    // NET PROFIT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Laba Bersih (Net Profit)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = blueColor)
                        Text(rpFormatter.format(profitMonth.netProfit), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = blueColor)
                    }
                }
            }

            // Explainer Card for Double-Count Prevention
            val explainerBg = if (isDark) Color(0xFFEAB308).copy(alpha = 0.15f) else Color(0xFFFFF9C4)
            val infoTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            val textBrown = if (isDark) Color(0xFFFBBF24) else Color(0xFF5D4037)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = explainerBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = infoTint,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Catatan Perhitungan Laba:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textBrown)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Belanja Bahan Baku sebesar ${rpFormatter.format(profitMonth.inventoryPurchase)} tidak dimasukkan langsung sebagai pengurang Laba Bersih di atas. " +
                            "Biaya bahan baku hanya dihitung saat terjual (sebagai HPP) atau dibuang (sebagai Waste). Hal ini mencegah double-counting pengeluaran.",
                            fontSize = 11.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else textBrown,
                            lineHeight = 16.sp
                        )
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

private fun generateProfitLossText(
    profitMonth: com.mediaxa.business.suite.data.repository.ProfitSummary,
    categoryExpenses: List<com.mediaxa.business.suite.data.local.dao.CategoryExpenseResult>,
    rpFormatter: java.text.NumberFormat
): String {
    val sb = StringBuilder()
    sb.append("Laporan Laba Rugi (P&L) - Bulan Ini\n")
    sb.append("=========================================\n")
    sb.append("Pendapatan (Omzet)  : ${rpFormatter.format(profitMonth.revenue)}\n")
    sb.append("Harga Pokok Penjualan: -${rpFormatter.format(profitMonth.hpp)}\n")
    sb.append("Laba Kotor          : ${rpFormatter.format(profitMonth.grossProfit)}\n")
    sb.append("Biaya Operasional   : -${rpFormatter.format(profitMonth.operationalExpense)}\n")
    sb.append("Biaya Waste         : -${rpFormatter.format(profitMonth.wasteCost)}\n")
    sb.append("Laba Bersih         : ${rpFormatter.format(profitMonth.netProfit)}\n")
    sb.append("=========================================\n\n")
    sb.append("Rincian Pengeluaran Operasional:\n")
    for (exp in categoryExpenses) {
        sb.append("- ${exp.category} : ${rpFormatter.format(exp.totalAmount)}\n")
    }
    return sb.toString()
}
