package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.Customer
import com.mediaxa.business.suite.presentation.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAnalyticsScreen(
    viewModel: CustomerViewModel,
    onBackClick: () -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val topCustomers by viewModel.topCustomers.collectAsState()
    val newCustomersCount by viewModel.newCustomersCount.collectAsState()
    val activeCustomersCount by viewModel.activeCustomersCount.collectAsState()

    // Calculate dynamic analytics on the client side
    val totalCount = customers.size
    val repeatCustomerRate = if (totalCount > 0) {
        (activeCustomersCount.toDouble() / totalCount.toDouble()) * 100.0
    } else {
        0.0
    }

    val sixtyDaysAgo = System.currentTimeMillis() - (60L * 24L * 60L * 60L * 1000L)
    val lostCustomers = customers.filter { it.lastVisit != null && it.lastVisit!! < sixtyDaysAgo }

    // Membership levels distribution
    val tierCounts = customers.groupBy { it.membershipLevel }
    val bronzeCount = tierCounts["BRONZE"]?.size ?: 0
    val silverCount = tierCounts["SILVER"]?.size ?: 0
    val goldCount = tierCounts["GOLD"]?.size ?: 0
    val platinumCount = tierCounts["PLATINUM"]?.size ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsMiniCard(
                        title = "Total Pelanggan",
                        value = totalCount.toString(),
                        icon = Icons.Default.Group,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMiniCard(
                        title = "Pelanggan Baru (30H)",
                        value = newCustomersCount.toString(),
                        icon = Icons.Default.PersonAdd,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsMiniCard(
                        title = "Pelanggan Aktif (30H)",
                        value = activeCustomersCount.toString(),
                        icon = Icons.Default.TrendingUp,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMiniCard(
                        title = "Repeat Customer %",
                        value = "${String.format("%.1f", repeatCustomerRate)}%",
                        icon = Icons.Default.TrendingUp,
                        iconTint = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Membership Level Distribution
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Distribusi Membership Level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TierProgressRow(label = "PLATINUM", count = platinumCount, total = totalCount, color = Color(0xFF6200EE))
                        Spacer(modifier = Modifier.height(8.dp))
                        TierProgressRow(label = "GOLD", count = goldCount, total = totalCount, color = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.height(8.dp))
                        TierProgressRow(label = "SILVER", count = silverCount, total = totalCount, color = Color(0xFF757575))
                        Spacer(modifier = Modifier.height(8.dp))
                        TierProgressRow(label = "BRONZE", count = bronzeCount, total = totalCount, color = Color(0xFF8D6E63))
                    }
                }
            }

            // Top Customer Leaderboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Top 5 Pelanggan Terloyal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (topCustomers.isEmpty()) {
                            Text(
                                "Tidak ada data transaksi pelanggan",
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            topCustomers.take(5).forEachIndexed { index, customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        Column {
                                            Text(customer.customerName, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "Tier: ${customer.membershipLevel}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Rp ${String.format("%,.0f", customer.totalSpending)}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (index < topCustomers.size - 1 && index < 4) {
                                    HorizontalDivider(color = Color(0xFFEEEEEE))
                                }
                            }
                        }
                    }
                }
            }

            // Lost Customer Alert (Inactive > 60 days)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pelanggan Pasif (>60 Hari)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (lostCustomers.isEmpty()) {
                            Text(
                                "Semua pelanggan aktif bertransaksi baru-baru ini.",
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            lostCustomers.forEachIndexed { index, customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(customer.customerName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Kode: ${customer.customerCode} | HP: ${customer.phone ?: "-"}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    val lastVisitText = customer.lastVisit?.let {
                                        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                                    } ?: "Belum berkunjung"
                                    Text(
                                        text = lastVisitText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                }
                                if (index < lostCustomers.size - 1) {
                                    HorizontalDivider(color = Color(0xFFEEEEEE))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.Gray, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
        }
    }
}

@Composable
fun TierProgressRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("$count (${String.format("%.0f", progress * 100)}%)", fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.Transparent
        )
    }
}
