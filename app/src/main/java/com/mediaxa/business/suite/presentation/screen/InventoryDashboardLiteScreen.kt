package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.dao.IngredientUsageResult
import com.mediaxa.business.suite.presentation.viewmodel.InventoryDashboardMetrics
import com.mediaxa.business.suite.presentation.viewmodel.InventoryLiteViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDashboardLiteScreen(
    viewModel: InventoryLiteViewModel,
    onBackClick: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToOpname: () -> Unit,
    onNavigateToWaste: () -> Unit,
    onManageIngredients: () -> Unit
) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val activeIngredients by viewModel.activeIngredients.collectAsState()
    
    // Automatically reload dashboard when entering
    LaunchedEffect(Unit) {
        viewModel.loadDashboardMetrics()
    }

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory & Expense Lite") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardMetrics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
                .padding(16.dp)
        ) {
            // Quick Actions Section
            Text("Aksi Cepat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Beli Stok",
                    icon = Icons.Default.AddShoppingCart,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToPurchase
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Biaya Ops",
                    icon = Icons.Default.Payments,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToExpense
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Stock Opname",
                    icon = Icons.Default.Assignment,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToOpname
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Waste / Rusak",
                    icon = Icons.Default.DeleteOutline,
                    color = Color(0xFFC62828), // Sleek Crimson Red
                    onClick = onNavigateToWaste
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onManageIngredients,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kelola Daftar Bahan Baku")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Financial & Stock Value Summaries
            Text("Ringkasan Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))

            SummaryCard(
                title = "Total Nilai Aset Stok",
                value = rpFormatter.format(metrics.totalStockValue),
                icon = Icons.Default.MonetizationOn,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Belanja (Bulan Ini)",
                    value = rpFormatter.format(metrics.monthlyPurchases),
                    color = Color(0xFFE8F5E9)
                )
                SummaryMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Waste (Bulan Ini)",
                    value = rpFormatter.format(metrics.monthlyWaste),
                    color = Color(0xFFFFEBEE)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stock Alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlertMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Hampir Habis",
                    count = metrics.lowStockCount,
                    color = Color(0xFFFFF3E0),
                    textColor = Color(0xFFE65100)
                )
                AlertMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Stok Habis",
                    count = metrics.outOfStockCount,
                    color = Color(0xFFFFEBEE),
                    textColor = Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Warnings list
            val warningIngredients = activeIngredients.filter { it.availableStock <= it.minStock }
            if (warningIngredients.isNotEmpty()) {
                Text("Peringatan Stok Rendah", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        warningIngredients.forEach { ing ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ing.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${ing.availableStock} / Min ${ing.minStock} ${ing.unit}",
                                    color = if (ing.availableStock <= 0.0) Color(0xFFC62828) else Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Top Ingredients statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Top Terpakai", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TopList(metrics.topConsumed)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Top Dibeli", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TopList(metrics.topPurchased)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun SummaryMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun AlertMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    color: Color,
    textColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
            Text("$count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun TopList(items: List<IngredientUsageResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (items.isEmpty()) {
                Text("Tidak ada data", fontSize = 11.sp, color = Color.Gray)
            } else {
                items.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${idx+1}. ${item.ingredientName}", fontSize = 12.sp, maxLines = 1)
                        Text("${item.totalQty} ${item.unit}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
