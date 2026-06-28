package com.mediaxa.business.suite.presentation.screen

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.dao.DailySalesTrendResult
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit,
    onViewPLClick: () -> Unit,
    onViewCashFlowClick: () -> Unit,
    onViewExpenseClick: () -> Unit,
    onViewDailyClosingClick: () -> Unit,
    onViewCashShiftClick: () -> Unit
) {
    val profitToday by viewModel.profitSummaryToday.collectAsState()
    val profitMonth by viewModel.profitSummaryMonth.collectAsState()
    val cashFlowMonth by viewModel.cashFlowMonth.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val dailyTrend by viewModel.dailyTrend.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    LaunchedEffect(Unit) {
        viewModel.loadFinanceData(storeId = 1L)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Keuangan & Laba") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            val isDark = isSystemInDarkTheme()
            val greenCardBg = if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
            val greenTextColor = if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32)
            
            val blueCardBg = if (isDark) Color(0xFF0D47A1).copy(alpha = 0.2f) else Color(0xFFE3F2FD)
            val blueTextColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1565C0)

            val redTextColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFC62828)
            val orangeTextColor = if (isDark) Color(0xFFFDBA74) else Color(0xFFD97706)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = greenCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Laba Bersih Hari Ini", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            rpFormatter.format(profitToday.netProfit),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = greenTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Omzet: ${rpFormatter.format(profitToday.revenue)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = blueCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Laba Bersih Bulan Ini", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            rpFormatter.format(profitMonth.netProfit),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = blueTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val margin = if (profitMonth.revenue > 0) (profitMonth.netProfit / profitMonth.revenue) * 100 else 0.0
                        Text("Margin Bersih: ${String.format("%.1f", margin)}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

                // KPI RATIOS CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("KPI Finansial (Bulan Ini)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Gross Margin %
                            val grossMargin = if (profitMonth.revenue > 0) (profitMonth.grossProfit / profitMonth.revenue) * 100 else 0.0
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Gross Margin", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", grossMargin)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = greenTextColor)
                            }
                            // Net Margin %
                            val netMargin = if (profitMonth.revenue > 0) (profitMonth.netProfit / profitMonth.revenue) * 100 else 0.0
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Net Margin", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", netMargin)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = blueTextColor)
                            }
                            // Expense Ratio %
                            val expRatio = if (profitMonth.revenue > 0) (profitMonth.operationalExpense / profitMonth.revenue) * 100 else 0.0
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Expense Ratio", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", expRatio)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = redTextColor)
                            }
                            // Waste Ratio %
                            val wasteRatio = if (profitMonth.revenue > 0) (profitMonth.wasteCost / profitMonth.revenue) * 100 else 0.0
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Waste Ratio", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", wasteRatio)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = orangeTextColor)
                            }
                        }
                    }
                }

                // PROYEKSI & BEP WIDGET
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Proyeksi & Analisis Laba (Bulan Ini)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val estimatedProfit = viewModel.getEstimatedMonthlyProfit()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimasi Laba Akhir Bulan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = rpFormatter.format(estimatedProfit),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (estimatedProfit >= 0) greenTextColor else redTextColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val bepSales = viewModel.getBreakEvenSales()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Break-Even Target Omzet:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = rpFormatter.format(bepSales),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (bepSales > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val progress = (profitMonth.revenue / bepSales).coerceIn(0.0, 1.0).toFloat()
                            Column {
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = if (progress >= 1.0f) greenTextColor else blueTextColor,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Progress BEP Omzet", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format("%.1f", progress * 100)}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // CASH FLOW SUMMARY WIDGET
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
                            Text("Ringkasan Arus Kas (Bulan Ini)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "Detail Kas ➔",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onViewCashFlowClick() }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Cash In", tint = greenTextColor, modifier = Modifier.size(16.dp))
                                    Text(" Cash In (Masuk)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(rpFormatter.format(cashFlowMonth.totalInflow), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Cash Out", tint = redTextColor, modifier = Modifier.size(16.dp))
                                    Text(" Cash Out (Keluar)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(rpFormatter.format(cashFlowMonth.totalOutflow), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // PROFIT CHART 30 DAYS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tren Omzet vs Laba Bersih (30 Hari Terakhir)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        DailyProfitTrendChart(
                            trendData = dailyTrend,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(blueTextColor, RoundedCornerShape(4.dp)))
                                Text(" Omzet", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(greenTextColor, RoundedCornerShape(4.dp)))
                                Text(" Laba Kotor (Revenue - HPP)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // EXPENSE SUMMARY SUMMARY
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
                            Text("Pengeluaran Operasional Terbesar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "Laporan Pengeluaran ➔",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onViewExpenseClick() }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (categoryExpenses.isEmpty()) {
                            Text("Tidak ada transaksi pengeluaran bulan ini", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            categoryExpenses.take(3).forEach { cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(rpFormatter.format(cat.totalAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // MAIN NAVIGATION BUTTONS FOR FINANCE
                Button(
                    onClick = onViewPLClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Laporan Laba Rugi (P&L)")
                }

                Button(
                    onClick = onViewDailyClosingClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFFC2410C) else Color(0xFFE65100)), // Orange-red
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup Buku Harian (Daily Closing)", color = Color.White)
                }

                Button(
                    onClick = onViewCashShiftClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF0F766E) else Color(0xFF00796B)), // Teal
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Kelola Shift Kasir (Cash Shift)", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DailyProfitTrendChart(
    trendData: List<DailySalesTrendResult>,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tidak ada data tren", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = trendData.maxOf { it.dailyRevenue }.coerceAtLeast(1.0)
    val isDark = isSystemInDarkTheme()
    val blueColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1565C0)
    val greenColor = if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32)
    val paintColor = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.GRAY

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 50.dp.toPx()
        val paddingRight = 20.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingBottom = 30.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Draw horizontal grid lines (Y-axis splits: 3 lines)
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = paddingTop + chartHeight * (i.toFloat() / gridCount)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw Revenue Line & Area
        val revenuePath = Path()
        val grossProfitPath = Path()
        val stepX = if (trendData.size > 1) chartWidth / (trendData.size - 1) else chartWidth

        trendData.forEachIndexed { index, data ->
            val x = paddingLeft + index * stepX
            val yRev = paddingTop + chartHeight * (1f - (data.dailyRevenue.toFloat() / maxVal.toFloat()))
            val yGProfit = paddingTop + chartHeight * (1f - ((data.dailyRevenue - data.dailyHpp).toFloat() / maxVal.toFloat()))

            if (index == 0) {
                revenuePath.moveTo(x, yRev)
                grossProfitPath.moveTo(x, yGProfit)
            } else {
                revenuePath.lineTo(x, yRev)
                grossProfitPath.lineTo(x, yGProfit)
            }
        }

        // Draw paths
        drawPath(
            path = revenuePath,
            color = blueColor, // Blue
            style = Stroke(width = 3.dp.toPx())
        )
        drawPath(
            path = grossProfitPath,
            color = greenColor, // Green
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw x-axis labels (max 5)
        val paint = Paint().apply {
            color = paintColor
            textSize = 8.dp.toPx()
            textAlign = Paint.Align.CENTER
        }
        val stepLabel = (trendData.size / 4).coerceAtLeast(1)
        trendData.forEachIndexed { index, data ->
            if (index % stepLabel == 0 || index == trendData.size - 1) {
                val x = paddingLeft + index * stepX
                val label = data.dateStr.substringAfter("-") // MM-DD format usually
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    height - 5.dp.toPx(),
                    paint
                )
            }
        }
    }
}
