package com.mediaxa.business.suite.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
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
import com.mediaxa.business.suite.presentation.viewmodel.InventoryLiteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: InventoryLiteViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiMessage by viewModel.uiStateMessage.collectAsState()

    var expenseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var category by remember { mutableStateOf("Belanja Bahan Baku") }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Belanja Bahan Baku", "Sewa", "Listrik", "Air", "Gaji",
        "Marketing", "Transport", "Maintenance", "Kebersihan", "Lain-lain"
    )

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
                title = { Text("Catat Pengeluaran Operasional") },
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
                        text = "Tanggal Pengeluaran: ${dateFormat.format(Date(expenseDate))}",
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = expenseDate
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCalendar = Calendar.getInstance()
                                newCalendar.set(year, month, day)
                                expenseDate = newCalendar.timeInMillis
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

            // Category Dropdown
            Box {
                OutlinedButton(
                    onClick = { categoryDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kategori: $category")
                        Text("▼")
                    }
                }
                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Nominal Pengeluaran (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Payment Method Choice
            Column {
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

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan / Keterangan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Photo Attachment Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { photoLauncher.launch("image/*") }) {
                    Text("Pilih Foto Nota (Opsional)")
                }
                if (attachmentUri != null) {
                    Text("Foto Nota Terpilih", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("Belum ada foto", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val localPath = attachmentUri?.let { uri ->
                        ImageStorageHelper.saveExpenseAttachment(context, uri)
                    }
                    viewModel.addExpense(
                        category = category,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        notes = notes.takeIf { it.isNotEmpty() },
                        expenseDate = expenseDate,
                        attachmentPath = localPath,
                        onSuccess = {
                            onBackClick()
                        }
                    )
                },
                enabled = amountStr.isNotEmpty() && (amountStr.toDoubleOrNull() ?: 0.0) >= 0.0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan Pengeluaran", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
