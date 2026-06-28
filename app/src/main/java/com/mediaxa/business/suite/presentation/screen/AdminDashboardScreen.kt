package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.User
import com.mediaxa.business.suite.presentation.component.SaasCard
import com.mediaxa.business.suite.presentation.viewmodel.MainViewModel
import com.mediaxa.business.suite.presentation.viewmodel.SyncMonitorViewModel
import com.mediaxa.business.suite.presentation.viewmodel.DashboardViewModel
import com.mediaxa.business.suite.presentation.viewmodel.ProductViewModel
import com.mediaxa.business.suite.presentation.viewmodel.CustomerViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    user: User,
    mainViewModel: MainViewModel,
    syncMonitorViewModel: SyncMonitorViewModel,
    dashboardViewModel: DashboardViewModel,
    productViewModel: ProductViewModel,
    customerViewModel: CustomerViewModel,
    financeViewModel: FinanceViewModel,
    onLogout: () -> Unit,
    onManageCategories: () -> Unit,
    onManageIngredients: () -> Unit,
    onManageMenus: () -> Unit,
    onNavigateToPos: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToCrm: () -> Unit,
    onNavigateToPromotions: () -> Unit,
    onNavigateToCustomerAnalytics: () -> Unit,
    onNavigateToSyncMonitor: () -> Unit,
    onNavigateToStoreSettings: () -> Unit,
    onNavigateToSettingsCenter: () -> Unit,
    onNavigateToBelanja: () -> Unit,
    onNavigateToPengeluaran: () -> Unit,
    onNavigateToClosing: () -> Unit
) {
    val storeSettings by mainViewModel.storeSettings.collectAsState()
    val syncState by syncMonitorViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val menus by productViewModel.menus.collectAsState()
    val customers by customerViewModel.customers.collectAsState()
    val dailyClosings by financeViewModel.dailyClosings.collectAsState()

    LaunchedEffect(Unit) {
        financeViewModel.loadDailyClosings(1L) // storeId = 1L
    }

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val configuration = LocalConfiguration.current
    val columnCount = if (configuration.screenWidthDp >= 600) 3 else 2

    val lowStockCount = dashboardState.stockAlerts.count { it.daysRemaining < 7.0 }

    val isClosedToday = remember(dailyClosings) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        dailyClosings.any { it.dateStr == todayStr }
    }

    val alertCount = (if (syncState.failedCount > 0 || syncState.pendingCount > 0) 1 else 0) +
                    (if (lowStockCount > 0) 1 else 0) +
                    (if (!isClosedToday) 1 else 0)

    var currentTab by remember { mutableStateOf(0) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mediaxa Business Suite",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SaaS 2026",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        val syncText = when {
                            syncState.isSyncing -> "Sinkronisasi..."
                            syncState.pendingCount > 0 -> "Pending: ${syncState.pendingCount}"
                            syncState.failedCount > 0 -> "Gagal: ${syncState.failedCount}"
                            else -> "Tersambung Cloud"
                        }
                        val syncColor = when {
                            syncState.isSyncing -> Color(0xFFF59E0B) // Amber
                            syncState.pendingCount > 0 -> Color(0xFFF59E0B) // Amber
                            syncState.failedCount > 0 -> Color(0xFFEF4444) // Red
                            else -> Color(0xFF10B981) // Emerald
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = storeSettings?.storeName ?: "Toko Utama",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color(0xFF64748B), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(syncColor, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.labelSmall.copy(color = syncColor, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettingsCenter) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Pengaturan")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Rounded.ExitToApp, contentDescription = "Log Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF080E1C),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0B1220),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Beranda") },
                    label = { Text("Beranda") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6366F1),
                        selectedTextColor = Color(0xFF6366F1),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = {
                        currentTab = 1
                        onNavigateToPos()
                        currentTab = 0
                    },
                    icon = { Icon(Icons.Rounded.PointOfSale, contentDescription = "Kasir") },
                    label = { Text("Kasir") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6366F1),
                        selectedTextColor = Color(0xFF6366F1),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = {
                        currentTab = 2
                        onNavigateToFinance()
                        currentTab = 0
                    },
                    icon = { Icon(Icons.Rounded.Assignment, contentDescription = "Laporan") },
                    label = { Text("Laporan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6366F1),
                        selectedTextColor = Color(0xFF6366F1),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = {
                        showNotificationsSheet = true
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (alertCount > 0) {
                                    Badge(containerColor = Color.Red) {
                                        Text("$alertCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifikasi")
                        }
                    },
                    label = { Text("Notifikasi") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6366F1),
                        selectedTextColor = Color(0xFF6366F1),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = {
                        currentTab = 4
                        onNavigateToSettingsCenter()
                        currentTab = 0
                    },
                    icon = { Icon(Icons.Rounded.Person, contentDescription = "Akun") },
                    label = { Text("Akun") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6366F1),
                        selectedTextColor = Color(0xFF6366F1),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                    )
                )
            }
        },
        containerColor = Color(0xFF080E1C)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val successColor = Color(0xFF10B981)
            val errorColor = Color(0xFFEF4444)
            // 2. KPI METRICS (SaaS Growth Cards using Design System, scrollable row)
            Column {
                Text(
                    text = "METRIK BISNIS UTAMA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFCBD5E1)
                    ),
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val formatGrowth = { v: Double ->
                        if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)
                    }
                    val revGrowth = dashboardState.revenueGrowthPercent
                    val revGrowthText = if (revGrowth >= 0) "↑ ${formatGrowth(revGrowth)}% vs kemarin" else "↓ ${formatGrowth(-revGrowth)}% vs kemarin"
                    val revGrowthColor = if (revGrowth >= 0) successColor else errorColor

                    KpiCard(
                        title = "Omzet Hari Ini",
                        value = rpFormatter.format(dashboardState.totalRevenue),
                        icon = Icons.Rounded.Timeline,
                        accentColor = Color(0xFF6366F1),
                        growthText = revGrowthText,
                        growthColor = revGrowthColor
                    )

                    val profitGrowth = dashboardState.profitGrowthPercent
                    val profitGrowthText = if (profitGrowth >= 0) "↑ ${formatGrowth(profitGrowth)}% vs kemarin" else "↓ ${formatGrowth(-profitGrowth)}% vs kemarin"
                    val profitGrowthColor = if (profitGrowth >= 0) successColor else errorColor

                    KpiCard(
                        title = "Laba Bersih",
                        value = rpFormatter.format(dashboardState.totalProfit),
                        icon = Icons.Rounded.TrendingUp,
                        accentColor = Color(0xFF10B981),
                        growthText = profitGrowthText,
                        growthColor = profitGrowthColor
                    )

                    KpiCard(
                        title = "Transaksi",
                        value = "${dashboardState.transactionCount}",
                        icon = Icons.Rounded.ShoppingBasket,
                        accentColor = Color(0xFF3B82F6),
                        growthText = "↑ 15% vs kemarin",
                        growthColor = successColor
                    )

                    KpiCard(
                        title = "Stok Menipis",
                        value = "$lowStockCount",
                        icon = Icons.Rounded.Warning,
                        accentColor = Color(0xFFF59E0B),
                        growthText = if (lowStockCount > 0) "Perlu restok" else "Stok aman",
                        growthColor = if (lowStockCount > 0) Color(0xFFFBBF24) else successColor
                    )
                }
            }

                // 3. QUICK ACTIONS HUB (directly on dark background, scrollable)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "AKSI CEPAT OPERASIONAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFCBD5E1)
                        ),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            label = "Kasir",
                            subtitle = "POS & transaksi",
                            icon = Icons.Rounded.PointOfSale,
                            color = Color(0xFF4F46E5),
                            onClick = onNavigateToPos
                        )
                        QuickActionButton(
                            label = "Belanja Stok",
                            subtitle = "Catat pembelian",
                            icon = Icons.Rounded.AddShoppingCart,
                            color = Color(0xFF0D9488),
                            onClick = onNavigateToBelanja
                        )
                        QuickActionButton(
                            label = "Pengeluaran",
                            subtitle = "Catat biaya",
                            icon = Icons.Rounded.Payments,
                            color = Color(0xFF9333EA),
                            onClick = onNavigateToPengeluaran
                        )
                        QuickActionButton(
                            label = "Closing",
                            subtitle = "Tutup hari ini",
                            icon = Icons.Rounded.Assignment,
                            color = Color(0xFFEA580C),
                            onClick = onNavigateToClosing
                        )
                    }
                }

                // 4. CATEGORIZED MODULE GRID (SaaS Categorized Grid Cards)
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // CATEGORY 1: SALES & CUSTOMERS
                    CategorySection(
                        title = "Penjualan & CRM",
                        accentColor = Color(0xFF4F46E5),
                        columnCount = columnCount,
                        items = listOf(
                            ModuleItem("Kasir", "POS & transaksi order cepat", Icons.Rounded.Store, Color(0xFF4F46E5), onNavigateToPos),
                            ModuleItem("Pelanggan", "${customers.size} member", Icons.Rounded.Group, Color(0xFF0284C7), onNavigateToCrm),
                            ModuleItem("Promo", "Diskon belanja & kampanye promo", Icons.Rounded.LocalOffer, Color(0xFFE11D48), onNavigateToPromotions)
                        )
                    )

                    // CATEGORY 2: LOGISTICS & INVENTORY
                    CategorySection(
                        title = "Logistik & Inventaris",
                        accentColor = Color(0xFF0D9488),
                        columnCount = columnCount,
                        items = listOf(
                            ModuleItem("Menu", "${menus.size} menu aktif", Icons.Rounded.MenuBook, Color(0xFFD97706), onManageMenus),
                            ModuleItem("Stok", "${lowStockCount} item menipis", Icons.Rounded.Inventory, Color(0xFF7C3AED), onManageIngredients),
                            ModuleItem("Belanja Stok", "Catat kulakan & belanja aset", Icons.Rounded.AddShoppingCart, Color(0xFF0D9488), onNavigateToBelanja)
                        )
                    )

                    // CATEGORY 3: FINANCE & ANALYSIS
                    CategorySection(
                        title = "Analisis & Keuangan",
                        accentColor = Color(0xFF059669),
                        columnCount = columnCount,
                        items = listOf(
                            ModuleItem("Keuangan", "Laba rugi, arus kas & modal", Icons.Rounded.TrendingUp, Color(0xFF2563EB), onNavigateToFinance),
                            ModuleItem("Pengeluaran", "Catat biaya non-inventory", Icons.Rounded.Payments, Color(0xFFBE185D), onNavigateToPengeluaran),
                            ModuleItem("Analitik", "Metrik performa & visual grafik", Icons.Rounded.BarChart, Color(0xFF059669), onNavigateToAnalytics),
                            ModuleItem("Laporan", "Ekspor & cetak laporan arsip", Icons.Rounded.Assignment, Color(0xFF475569), onNavigateToFinance)
                        )
                    )

                    // CATEGORY 4: SYSTEM INFRASTRUCTURE
                    CategorySection(
                        title = "Sistem & Administrasi",
                        accentColor = Color(0xFF64748B),
                        columnCount = columnCount,
                        items = listOf(
                            ModuleItem("Pengaturan", "Ubah PIN keamanan & info toko", Icons.Rounded.Settings, Color(0xFF4E342E), onNavigateToSettingsCenter),
                            ModuleItem("Cloud Sync", "Status antrean cloud server", Icons.Rounded.CloudSync, Color(0xFF0891B2), onNavigateToSyncMonitor)
                        )
                    )
                }
            }
        }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = Color(0xFF0B1220),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "PEMBERITAHUAN TERBARU",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                HorizontalDivider(color = Color(0xFF1E293B))
                
                // 1. Cloud Sync Notification Row
                val syncRowColor = if (syncState.failedCount > 0) Color(0xFFEF4444) else (if (syncState.pendingCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981))
                val syncIcon = if (syncState.failedCount > 0) Icons.Rounded.Warning else Icons.Rounded.CloudSync
                val syncTitle = if (syncState.failedCount > 0) "Cloud Sync Gagal" else (if (syncState.pendingCount > 0) "Cloud Sync Pending" else "Cloud Sync Sukses")
                val syncDesc = if (syncState.failedCount > 0) "${syncState.failedCount} transaksi gagal diunggah ke cloud." else (if (syncState.pendingCount > 0) "${syncState.pendingCount} transaksi menunggu antrean." else "Semua data transaksi tersambung dengan cloud.")
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showNotificationsSheet = false
                            onNavigateToSyncMonitor()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(syncRowColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(syncIcon, contentDescription = null, tint = syncRowColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(syncTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(syncDesc, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    }
                }
                
                // 2. Stock Alert Notification Row
                val stockRowColor = if (lowStockCount > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                val stockIcon = if (lowStockCount > 0) Icons.Rounded.Warning else Icons.Rounded.Inventory
                val stockDesc = if (lowStockCount > 0) "Ada $lowStockCount bahan baku menipis. Perlu restok segera." else "Seluruh stok bahan baku dalam kondisi aman."
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showNotificationsSheet = false
                            onManageIngredients()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(stockRowColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(stockIcon, contentDescription = null, tint = stockRowColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Stock Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stockDesc, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    }
                }

                // 3. Daily Closing Notification Row
                val closingRowColor = if (isClosedToday) Color(0xFF10B981) else Color(0xFFEF4444)
                val closingDesc = if (isClosedToday) "Tutup buku harian hari ini sudah diselesaikan." else "Tutup buku harian belum diselesaikan hari ini."
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showNotificationsSheet = false
                            onNavigateToClosing()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(closingRowColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Assignment, contentDescription = null, tint = closingRowColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Daily Closing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(closingDesc, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    growthText: String? = null,
    growthColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    SaasCard(
        modifier = modifier.width(150.dp),
        containerColor = Color(0xFF111928),
        borderColor = Color(0xFF1F2937)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (growthText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = growthText,
                        style = MaterialTheme.typography.labelSmall.copy(color = growthColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    title: String,
    accentColor: Color,
    items: List<ModuleItem>,
    columnCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                letterSpacing = 0.8.sp
            )
        }

        val chunked = items.chunked(columnCount)
        for (rowItems in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (item in rowItems) {
                    ModuleCard(item = item, modifier = Modifier.weight(1f))
                }
                val remaining = columnCount - rowItems.size
                for (i in 0 until remaining) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(160.dp).height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class ModuleItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@Composable
fun ModuleCard(
    item: ModuleItem,
    modifier: Modifier = Modifier
) {
    SaasCard(
        onClick = item.onClick,
        modifier = modifier.height(82.dp),
        containerColor = Color.White,
        borderColor = Color(0xFFE2E8F0)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(item.accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
