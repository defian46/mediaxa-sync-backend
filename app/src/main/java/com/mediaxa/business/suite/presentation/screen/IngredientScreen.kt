package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.Ingredient
import com.mediaxa.business.suite.presentation.viewmodel.InventoryViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientScreen(
    viewModel: InventoryViewModel,
    onBackClick: () -> Unit
) {
    val ingredients by viewModel.ingredients.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedIngredient by remember { mutableStateOf<Ingredient?>(null) }
    
    // CRUD state fields
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var packageSize by remember { mutableStateOf("") }
    var availableStock by remember { mutableStateOf("0") }
    var minStock by remember { mutableStateOf("0") }
    var supplier by remember { mutableStateOf("") }

    // Stock Adjust Dialog State
    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustAmount by remember { mutableStateOf("") }
    var adjustType by remember { mutableStateOf("ADJUSTMENT_PLUS") } // ADJUSTMENT_PLUS, ADJUSTMENT_MINUS
    var adjustNote by remember { mutableStateOf("") }

    val rpFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                selectedIngredient = null
            },
            title = {
                Text(if (selectedIngredient == null) "Tambah Bahan Baku" else "Ubah Bahan Baku")
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Bahan Baku") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan (gram, ml, pcs, dll.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("Harga Beli Per Dus/Pack (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = packageSize,
                        onValueChange = { packageSize = it },
                        label = { Text("Isi Per Dus/Pack (dalam satuan)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    if (selectedIngredient == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = availableStock,
                            onValueChange = { availableStock = it },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text("Stok Minimum") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Supplier (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = purchasePrice.toDoubleOrNull() ?: 0.0
                        val size = packageSize.toDoubleOrNull() ?: 0.0
                        val stock = availableStock.toDoubleOrNull() ?: 0.0
                        val minSt = minStock.toDoubleOrNull() ?: 0.0
                        val currentIng = selectedIngredient

                        if (currentIng == null) {
                            if (name.isNotEmpty()) {
                                viewModel.addIngredient(name, unit, price, size, stock, minSt, supplier.ifEmpty { null })
                            }
                        } else {
                            viewModel.updateIngredient(
                                currentIng, name, unit, price, size, minSt, supplier.ifEmpty { null }
                            )
                        }
                        showDialog = false
                        selectedIngredient = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        selectedIngredient = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showAdjustDialog && selectedIngredient != null) {
        val ingredient = selectedIngredient!!
        AlertDialog(
            onDismissRequest = {
                showAdjustDialog = false
                selectedIngredient = null
            },
            title = {
                Text("Adjustment Stok - ${ingredient.name}")
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { adjustType = "ADJUSTMENT_PLUS" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (adjustType == "ADJUSTMENT_PLUS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (adjustType == "ADJUSTMENT_PLUS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Stok Masuk (+)")
                        }
                        Button(
                            onClick = { adjustType = "ADJUSTMENT_MINUS" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (adjustType == "ADJUSTMENT_MINUS") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (adjustType == "ADJUSTMENT_MINUS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Stok Keluar (-)")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = adjustAmount,
                        onValueChange = { adjustAmount = it },
                        label = { Text("Jumlah") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustNote,
                        onValueChange = { adjustNote = it },
                        label = { Text("Catatan / Alasan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qtyRaw = adjustAmount.toDoubleOrNull() ?: 0.0
                        val qty = if (adjustType == "ADJUSTMENT_MINUS") -qtyRaw else qtyRaw
                        if (qty != 0.0) {
                            viewModel.adjustStock(
                                ingredient = ingredient,
                                quantity = qty,
                                type = adjustType,
                                note = adjustNote.ifEmpty { null },
                                userUuid = "DEV-USER"
                            )
                        }
                        showAdjustDialog = false
                        selectedIngredient = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdjustDialog = false
                        selectedIngredient = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Bahan Baku") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedIngredient = null
                            name = ""
                            unit = ""
                            purchasePrice = ""
                            packageSize = ""
                            availableStock = ""
                            minStock = ""
                            supplier = ""
                            showDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Bahan Baku", tint = Color.White)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (ingredients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada bahan baku. Tekan tombol + untuk menambahkan", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ingredients) { ingredient ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(ingredient.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Stok: ${ingredient.availableStock} ${ingredient.unit} (Min: ${ingredient.minStock} ${ingredient.unit})",
                                            fontSize = 13.sp,
                                            color = if (ingredient.availableStock <= ingredient.minStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                selectedIngredient = ingredient
                                                adjustAmount = ""
                                                adjustNote = ""
                                                adjustType = "ADJUSTMENT_PLUS"
                                                showAdjustDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Inventory, contentDescription = "Adjustment Stok", tint = MaterialTheme.colorScheme.tertiary)
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedIngredient = ingredient
                                                name = ingredient.name
                                                unit = ingredient.unit
                                                purchasePrice = ingredient.purchasePrice.toString()
                                                packageSize = ingredient.packageSize.toString()
                                                minStock = ingredient.minStock.toString()
                                                supplier = ingredient.supplier ?: ""
                                                showDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteIngredient(ingredient)
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Beli: ${rpFormatter.format(ingredient.purchasePrice)} / ${ingredient.packageSize} ${ingredient.unit}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Satuan: ${rpFormatter.format(ingredient.unitPrice)} / ${ingredient.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
