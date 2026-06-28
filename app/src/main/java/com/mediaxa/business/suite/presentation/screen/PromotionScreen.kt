package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mediaxa.business.suite.data.local.entity.PromotionRule
import com.mediaxa.business.suite.presentation.viewmodel.PromotionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionScreen(
    viewModel: PromotionViewModel,
    onBackClick: () -> Unit
) {
    val allRules by viewModel.allPromotionRules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotion Engine Manager") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Promo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Semua Aturan Promo", style = MaterialTheme.typography.titleMedium)

            if (allRules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada aturan promo yang dibuat", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allRules) { rule ->
                        PromotionRuleCard(
                            rule = rule,
                            onToggleActive = { active -> viewModel.togglePromotionActive(rule, active) },
                            onDelete = { viewModel.deletePromotionRule(rule) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPromotionRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, type, value, buyMenu, buyQty, getMenu, getQty, minPurchase, startHr, endHr, days, code ->
                viewModel.savePromotionRule(
                    uuid = null,
                    name = name,
                    promoType = type,
                    value = value,
                    buyMenuUuid = buyMenu,
                    buyQuantity = buyQty,
                    getMenuUuid = getMenu,
                    getQuantity = getQty,
                    minPurchaseAmount = minPurchase,
                    isActive = true,
                    startDate = null,
                    endDate = null,
                    startHour = startHr,
                    endHour = endHr,
                    applicableDays = days,
                    targetMembershipLevels = null,
                    targetCategoryUuid = null,
                    targetMenuUuid = null,
                    promoCode = code,
                    onSuccess = { showAddDialog = false },
                    onError = {}
                )
            }
        )
    }
}

@Composable
fun PromotionRuleCard(
    rule: PromotionRule,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Tipe: ${rule.promoType} | Nilai: ${rule.value}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!rule.promoCode.isNullOrEmpty()) {
                    Text("Kode Klaim: ${rule.promoCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (rule.startHour != null && rule.endHour != null) {
                    Text("Jam Promo: ${rule.startHour}:00 - ${rule.endHour}:00", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isActive,
                    onCheckedChange = onToggleActive
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPromotionRuleDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: String,
        value: Double,
        buyMenuUuid: String?,
        buyQuantity: Int?,
        getMenuUuid: String?,
        getQuantity: Int?,
        minPurchase: Double?,
        startHour: Int?,
        endHour: Int?,
        applicableDays: String?,
        promoCode: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PERCENTAGE_DISCOUNT") }
    var valueStr by remember { mutableStateOf("") }
    var buyMenu by remember { mutableStateOf("") }
    var buyQtyStr by remember { mutableStateOf("") }
    var getMenu by remember { mutableStateOf("") }
    var getQtyStr by remember { mutableStateOf("") }
    var minPurchaseStr by remember { mutableStateOf("") }
    var startHourStr by remember { mutableStateOf("") }
    var endHourStr by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Aturan Promo Baru") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Promosi (Wajib)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Tipe Promo", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("PERCENTAGE_DISCOUNT", "NOMINAL_DISCOUNT", "BUY_X_GET_Y").forEach { pType ->
                            FilterChip(
                                selected = type == pType,
                                onClick = { type = pType },
                                label = { Text(pType.replace("_", " ")) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { valueStr = it },
                        label = { Text("Nilai Diskon (Persen / Nominal Rupiah)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (type == "BUY_X_GET_Y") {
                    item {
                        OutlinedTextField(
                            value = buyMenu,
                            onValueChange = { buyMenu = it },
                            label = { Text("Beli Menu UUID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = buyQtyStr,
                            onValueChange = { buyQtyStr = it },
                            label = { Text("Jumlah Beli") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = getMenu,
                            onValueChange = { getMenu = it },
                            label = { Text("Dapat Menu UUID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = getQtyStr,
                            onValueChange = { getQtyStr = it },
                            label = { Text("Jumlah Dapat Free") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = minPurchaseStr,
                        onValueChange = { minPurchaseStr = it },
                        label = { Text("Min Belanja (Rupiah)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = startHourStr,
                        onValueChange = { startHourStr = it },
                        label = { Text("Mulai Jam Happy Hour (0-23)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = endHourStr,
                        onValueChange = { endHourStr = it },
                        label = { Text("Selesai Jam Happy Hour (0-23)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = days,
                        onValueChange = { days = it },
                        label = { Text("Hari Aktif (e.g. SATURDAY,SUNDAY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Kode Voucher / Claim Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        type,
                        valueStr.toDoubleOrNull() ?: 0.0,
                        buyMenu.takeIf { it.isNotBlank() },
                        buyQtyStr.toIntOrNull(),
                        getMenu.takeIf { it.isNotBlank() },
                        getQtyStr.toIntOrNull(),
                        minPurchaseStr.toDoubleOrNull(),
                        startHourStr.toIntOrNull(),
                        endHourStr.toIntOrNull(),
                        days.takeIf { it.isNotBlank() },
                        code.takeIf { it.isNotBlank() }
                    )
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
