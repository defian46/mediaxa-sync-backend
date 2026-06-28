package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaxa.business.suite.presentation.component.*
import com.mediaxa.business.suite.presentation.viewmodel.DashboardPeriod
import com.mediaxa.business.suite.presentation.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Analytics", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedCorner(16.dp)
            ) {
                // Period tabs selector
                PeriodSelector(
                    selectedPeriod = uiState.period,
                    onPeriodSelected = { viewModel.setPeriod(it) }
                )

                // 1. Ringkasan Hari Ini Cards
                SummaryGrid(
                    revenue = uiState.totalRevenue,
                    profit = uiState.totalProfit,
                    transactions = uiState.transactionCount,
                    avgTicket = uiState.averageTicket,
                    revenueGrowth = uiState.revenueGrowthPercent,
                    profitGrowth = uiState.profitGrowthPercent,
                    format = numberFormat
                )

                // 2. Target Tracker Section
                TargetTrackerCard(
                    progressPercent = uiState.targetProgressPercent,
                    targetSales = uiState.targetSales,
                    currentRevenue = uiState.totalRevenue,
                    estDays = uiState.targetEstimationDays,
                    format = numberFormat
                )

                // 3. Sales Area Chart Card
                val trendPoints = uiState.hourlyTrends.map { "${it.hourOfDay}:00" to it.totalAmount }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Trend Penjualan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SalesAreaChart(
                            dataPoints = trendPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                // 4. Peak Hours & Payment Methods (Stacked)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedCorner(16.dp)
                ) {
                    // Peak hours
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Jam Sibuk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val hourPoints = uiState.hourlyTrends.map { it.hourOfDay to it.transactionCount }
                            HourlyPeakBarChart(
                                hourlyData = hourPoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                        }
                    }

                    // Payments
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Metode Pembayaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val segments = uiState.paymentBreakdown.map { it.paymentMethod to it.totalAmount }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PaymentsDonutChart(
                                    paymentSegments = segments,
                                    modifier = Modifier.size(120.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Top & Bottom Products
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedCorner(16.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Top 5 Produk", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.topProducts.forEachIndexed { index, prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${index + 1}. ${prod.menuName}", maxLines = 1, modifier = Modifier.weight(1f))
                                    Text("${prod.totalQty} pcs", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (uiState.topProducts.isEmpty()) {
                                Text("Tidak ada data", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bottom 5 Produk", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.bottomProducts.forEachIndexed { index, prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${index + 1}. ${prod.menuName}", maxLines = 1, modifier = Modifier.weight(1f))
                                    Text("${prod.totalQty} pcs", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (uiState.bottomProducts.isEmpty()) {
                                Text("Tidak ada data", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                // 6. Inventory Forecast Warnings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Estimasi Stok Bahan Baku Habis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val criticalAlerts = uiState.stockAlerts.filter { it.daysRemaining < 7.0 }
                        criticalAlerts.forEach { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = if (alert.daysRemaining <= 1.0) Color.Red else Color(0xFFFF9800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(alert.name, fontWeight = FontWeight.SemiBold)
                                }
                                val daysText = if (alert.daysRemaining <= 0.0) {
                                    "Stok Habis"
                                } else if (alert.daysRemaining <= 1.0) {
                                    "Habis Hari Ini"
                                } else {
                                    "Habis dlm ${String.format("%.1f", alert.daysRemaining)} hari"
                                }
                                Text(
                                    daysText,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alert.daysRemaining <= 1.0) Color.Red else Color(0xFFFF9800)
                                )
                            }
                        }
                        if (criticalAlerts.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "OK", tint = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seluruh stok bahan baku aman (> 7 hari)", color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }

                // 7. Quick Actions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Aksi Cepat", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.ShoppingCart,
                                label = "Buka POS",
                                onClick = onNavigateToPos
                            )
                            QuickActionButton(
                                icon = Icons.Default.Build,
                                label = "Kelola Stok",
                                onClick = onNavigateToInventory
                            )
                            QuickActionButton(
                                icon = Icons.Default.List,
                                label = "Riwayat Trx",
                                onClick = onNavigateToHistory
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: DashboardPeriod,
    onPeriodSelected: (DashboardPeriod) -> Unit
) {
    val periods = listOf(
        DashboardPeriod.TODAY to "Hari Ini",
        DashboardPeriod.YESTERDAY to "Kemarin",
        DashboardPeriod.WEEKLY to "Mingguan",
        DashboardPeriod.MONTHLY to "Bulanan"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        periods.forEach { (period, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedPeriod == period) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selectedPeriod == period) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SummaryGrid(
    revenue: Double,
    profit: Double,
    transactions: Int,
    avgTicket: Double,
    revenueGrowth: Double,
    profitGrowth: Double,
    format: NumberFormat
) {
    Column(verticalArrangement = Arrangement.spacedCorner(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedCorner(12.dp)) {
            SummaryCard(
                title = "Omzet",
                value = format.format(revenue),
                growth = revenueGrowth,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Profit Kotor",
                value = format.format(profit),
                growth = profitGrowth,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedCorner(12.dp)) {
            SummaryCard(
                title = "Transaksi",
                value = transactions.toString(),
                growth = 0.0,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Avg Ticket Size",
                value = format.format(avgTicket),
                growth = 0.0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    growth: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp, 
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            
            val subtitle = when(title) {
                "Omzet" -> "Total pendapatan kotor"
                "Profit Kotor" -> "Pendapatan setelah HPP"
                "Transaksi" -> "Jumlah transaksi selesai"
                "Avg Ticket Size" -> "Rata-rata per transaksi"
                else -> ""
            }
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (growth != 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (growth > 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Growth",
                        tint = if (growth > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (growth > 0) "+" else ""}${String.format("%.1f", growth)}% vs periode lalu",
                        color = if (growth > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (title == "Transaksi" || title == "Avg Ticket Size") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Trend",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Stabil",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TargetTrackerCard(
    progressPercent: Double,
    targetSales: Double,
    currentRevenue: Double,
    estDays: Double,
    format: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text("Target Penjualan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tercapai: ${format.format(currentRevenue)} / ${format.format(targetSales)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                val estText = if (estDays == Double.MAX_VALUE) {
                    "Estimasi tercapai: Tidak terhingga (sales = 0)"
                } else {
                    "Estimasi tercapai dalam: ${String.format("%.1f", estDays)} hari"
                }
                Text(
                    estText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicatorWidget(
                    progressPercent = progressPercent,
                    modifier = Modifier.size(70.dp)
                )
                Text(
                    "${String.format("%.1f", progressPercent)}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}

// Arrangement helper for older Gradle projects
private fun Arrangement.spacedCorner(space: androidx.compose.ui.unit.Dp) = Arrangement.spacedBy(space)
