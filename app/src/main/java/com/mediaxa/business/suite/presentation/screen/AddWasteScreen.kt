package com.mediaxa.business.suite.presentation.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
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
import com.mediaxa.business.suite.presentation.viewmodel.InventoryLiteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWasteScreen(
    viewModel: InventoryLiteViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activeIngredients by viewModel.activeIngredients.collectAsState()
    val uiMessage by viewModel.uiStateMessage.collectAsState()

    var wasteDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedIng by remember { mutableStateOf<Ingredient?>(null) }
    var quantityStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Rusak") } // Rusak, Kadaluarsa, Tumpah, Sampling, Lain-lain
    var notes by remember { mutableStateOf("") }

    var ingredientDropdownExpanded by remember { mutableStateOf(false) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val reasons = listOf("Rusak", "Kadaluarsa", "Tumpah", "Sampling", "Lain-lain")

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
                title = { Text("Catat Barang Rusak / Waste") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Picker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tanggal Waste: ${dateFormat.format(Date(wasteDate))}",
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = wasteDate
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCalendar = Calendar.getInstance()
                                newCalendar.set(year, month, day)
                                wasteDate = newCalendar.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                    }
                }
            }

            // Ingredient Selector (Dropdown)
            Box {
                OutlinedButton(
                    onClick = { ingredientDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedIng?.let { "${it.name} (Stok: ${it.availableStock} ${it.unit})" } ?: "Pilih Bahan Baku *")
                        Text("▼")
                    }
                }
                DropdownMenu(
                    expanded = ingredientDropdownExpanded,
                    onDismissRequest = { ingredientDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    activeIngredients.forEach { ing ->
                        DropdownMenuItem(
                            text = { Text("${ing.name} (Stok: ${ing.availableStock} ${ing.unit})") },
                            onClick = {
                                selectedIng = ing
                                ingredientDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Qty Field
            OutlinedTextField(
                value = quantityStr,
                onValueChange = { quantityStr = it },
                label = { Text("Qty Terbuang / Rusak (${selectedIng?.unit ?: ""})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Reason Dropdown
            Box {
                OutlinedButton(
                    onClick = { reasonDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alasan: $reason")
                        Text("▼")
                    }
                }
                DropdownMenu(
                    expanded = reasonDropdownExpanded,
                    onDismissRequest = { reasonDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    reasons.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = {
                                reason = r
                                reasonDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Detail (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull() ?: 0.0
                    val ingredient = selectedIng
                    if (ingredient != null) {
                        viewModel.addWasteLog(
                            ingredientUuid = ingredient.uuid,
                            quantity = qty,
                            reason = reason,
                            notes = notes.takeIf { it.isNotEmpty() },
                            wasteDate = wasteDate,
                            onSuccess = {
                                onBackClick()
                            }
                        )
                    }
                },
                enabled = selectedIng != null && quantityStr.isNotEmpty() && (quantityStr.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan Waste Log", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
