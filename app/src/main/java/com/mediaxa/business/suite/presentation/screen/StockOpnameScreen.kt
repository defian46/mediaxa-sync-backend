package com.mediaxa.business.suite.presentation.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
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
import com.mediaxa.business.suite.data.local.entity.StockOpnameItem
import com.mediaxa.business.suite.presentation.viewmodel.InventoryLiteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOpnameScreen(
    viewModel: InventoryLiteViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activeIngredients by viewModel.activeIngredients.collectAsState()
    val uiMessage by viewModel.uiStateMessage.collectAsState()

    var opnameDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }

    // Map to keep track of user inputted physical stock values and notes
    // key: ingredientUuid, value: Pair(physicalStockString, notesString)
    val opnameInputs = remember { mutableStateMapOf<String, Pair<String, String>>() }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    // Populate inputs once ingredients are loaded
    LaunchedEffect(activeIngredients) {
        activeIngredients.forEach { ing ->
            if (!opnameInputs.containsKey(ing.uuid)) {
                opnameInputs[ing.uuid] = Pair("", "")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stock Opname (Audit Stok)") },
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
            // General Date & Notes
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
                            text = "Tanggal Opname: ${dateFormat.format(Date(opnameDate))}",
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = opnameDate
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCalendar = Calendar.getInstance()
                                    newCalendar.set(year, month, day)
                                    opnameDate = newCalendar.timeInMillis
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
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Opname Utama (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Daftar Bahan Baku", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Ingredients audit list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(activeIngredients) { _, ing ->
                    val input = opnameInputs[ing.uuid] ?: Pair("", "")
                    val physicalStockStr = input.first
                    val itemNotes = input.second

                    val physicalStock = physicalStockStr.toDoubleOrNull()
                    val hasDiff = physicalStock != null && physicalStock != ing.availableStock
                    val diff = if (physicalStock != null) physicalStock - ing.availableStock else 0.0

                    val notesMandatoryAlert = hasDiff && itemNotes.trim().isEmpty()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notesMandatoryAlert) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (notesMandatoryAlert) Color(0xFFC62828) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header: Ingredient Name & System Stock
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ing.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Sistem: ${ing.availableStock} ${ing.unit}", color = Color.Gray, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Physical stock input
                                OutlinedTextField(
                                    value = physicalStockStr,
                                    onValueChange = {
                                        opnameInputs[ing.uuid] = Pair(it, itemNotes)
                                    },
                                    label = { Text("Stok Fisik") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                // Difference calculation display
                                Column(
                                    modifier = Modifier.width(100.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("Selisih", fontSize = 11.sp, color = Color.Gray)
                                    val prefix = if (diff > 0) "+" else ""
                                    Text(
                                        text = if (physicalStock != null) "$prefix$diff ${ing.unit}" else "-",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diff == 0.0) Color.Gray else if (diff < 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                }
                            }

                            // If there is a difference, show mandatory notes field
                            if (hasDiff) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = itemNotes,
                                    onValueChange = {
                                        opnameInputs[ing.uuid] = Pair(physicalStockStr, it)
                                    },
                                    label = { Text("Alasan Selisih *") },
                                    placeholder = { Text("Susu pecah, tumpah, expired, dll.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = notesMandatoryAlert
                                )
                                if (notesMandatoryAlert) {
                                    Text(
                                        text = "Catatan wajib diisi karena terdapat selisih",
                                        color = Color(0xFFC62828),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val opnameItems = mutableListOf<StockOpnameItem>()
                    for (ing in activeIngredients) {
                        val input = opnameInputs[ing.uuid] ?: Pair("", "")
                        val physicalStock = input.first.toDoubleOrNull()
                        if (physicalStock != null) {
                            val diff = physicalStock - ing.availableStock
                            opnameItems.add(
                                StockOpnameItem(
                                    opnameUuid = "", // Header filled in VM
                                    ingredientUuid = ing.uuid,
                                    systemStock = ing.availableStock,
                                    physicalStock = physicalStock,
                                    diffStock = diff,
                                    notes = input.second,
                                    storeId = 1L
                                )
                            )
                        }
                    }

                    viewModel.addStockOpname(
                        opnameDate = opnameDate,
                        notes = notes.takeIf { it.isNotEmpty() },
                        items = opnameItems,
                        onSuccess = {
                            onBackClick()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan Adjustment Opname", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
