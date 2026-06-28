package com.mediaxa.business.suite.presentation.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
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
import com.mediaxa.business.suite.data.local.entity.Ingredient
import com.mediaxa.business.suite.data.local.entity.PurchaseExpenseItem
import com.mediaxa.business.suite.presentation.viewmodel.InventoryLiteViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseExpenseScreen(
    viewModel: InventoryLiteViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activeIngredients by viewModel.activeIngredients.collectAsState()
    val uiMessage by viewModel.uiStateMessage.collectAsState()

    var purchaseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var purchasePlaceName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") } // CASH, TRANSFER, DEBIT, dll.

    // Temporary list of items being purchased
    val itemsList = remember { mutableStateListOf<PurchaseExpenseItem>() }

    // Dialog state for adding an item
    var showAddItemDialog by remember { mutableStateOf(false) }

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    // Show Toast/Snackbar if there is a message from VM
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Input Pembelanjaan Stok") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Header Form (Date, Supplier, Notes)
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
                        Text(
                            text = "Tanggal: ${dateFormat.format(Date(purchaseDate))}",
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = purchaseDate
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCalendar = Calendar.getInstance()
                                    newCalendar.set(year, month, day)
                                    purchaseDate = newCalendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = purchasePlaceName,
                        onValueChange = { purchasePlaceName = it },
                        label = { Text("Nama Toko Pembelian (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Belanja (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Payment Method Choice
                    Text("Metode Pembayaran", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CASH", "TRANSFER", "QRIS").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Item Belanja",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddItemDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Item", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (itemsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada item belanja. Klik Tambah Item.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    itemsIndexed(itemsList) { index, item ->
                        val ingName = activeIngredients.firstOrNull { it.uuid == item.ingredientUuid }?.name ?: "Bahan Baku"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ingName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${item.quantity} ${item.unit} @ ${rpFormatter.format(item.unitPrice)}", fontSize = 12.sp, color = Color.Gray)
                                    if (!item.batchNumber.isNullOrEmpty()) {
                                        Text("Batch: ${item.batchNumber}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(rpFormatter.format(item.totalPrice), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { itemsList.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total & Save
            val totalBelanja = itemsList.sumOf { it.totalPrice }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Belanja", fontSize = 12.sp)
                        Text(rpFormatter.format(totalBelanja), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.addPurchaseExpense(
                                purchaseDate = purchaseDate,
                                purchasePlaceName = purchasePlaceName.takeIf { it.isNotEmpty() },
                                notes = notes.takeIf { it.isNotEmpty() },
                                paymentMethod = paymentMethod,
                                items = itemsList.toList(),
                                onSuccess = {
                                    onBackClick()
                                }
                            )
                        },
                        enabled = itemsList.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        var selectedIng by remember { mutableStateOf<Ingredient?>(null) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        var quantityStr by remember { mutableStateOf("") }
        var totalPriceStr by remember { mutableStateOf("") }
        var batchNumber by remember { mutableStateOf("") }
        var expiredDate by remember { mutableStateOf<Long?>(null) }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Tambah Item Belanja", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Ingredient Selector (Dropdown Menu)
                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedIng?.name ?: "Pilih Bahan Baku")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            activeIngredients.forEach { ing ->
                                DropdownMenuItem(
                                    text = { Text(ing.name) },
                                    onClick = {
                                        selectedIng = ing
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Qty Field
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Qty Beli (${selectedIng?.unit ?: ""})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Total Price Field
                    OutlinedTextField(
                        value = totalPriceStr,
                        onValueChange = { totalPriceStr = it },
                        label = { Text("Harga Total Item (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Batch Number Field
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Number (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Expired Date Choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expiredDate?.let { "Expired: ${dateFormat.format(Date(it))}" } ?: "Expired Date (Opsional)",
                            fontSize = 13.sp
                        )
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCalendar = Calendar.getInstance()
                                    newCalendar.set(year, month, day)
                                    expiredDate = newCalendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Expired")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ingredient = selectedIng
                        val qty = quantityStr.toDoubleOrNull() ?: 0.0
                        val price = totalPriceStr.toDoubleOrNull() ?: 0.0
                        if (ingredient != null && qty > 0.0 && price >= 0.0) {
                            val unitPrice = price / qty
                            val newItem = PurchaseExpenseItem(
                                purchaseExpenseUuid = "", // Filled before save
                                ingredientUuid = ingredient.uuid,
                                quantity = qty,
                                unit = ingredient.unit,
                                totalPrice = price,
                                unitPrice = unitPrice,
                                batchNumber = batchNumber.takeIf { it.isNotEmpty() },
                                expiredDate = expiredDate,
                                storeId = 1L
                            )
                            itemsList.add(newItem)
                            showAddItemDialog = false
                        }
                    },
                    enabled = selectedIng != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0.0 && (totalPriceStr.toDoubleOrNull() ?: 0.0) >= 0.0
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
