package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashShiftScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val storeId = 1L

    val activeShift by viewModel.activeShift.collectAsState()
    val shiftList by viewModel.shiftList.collectAsState()
    val expectedCash by viewModel.expectedCash.collectAsState()
    val uiMessage by viewModel.uiStateMessage.collectAsState()

    var openingCashStr by remember { mutableStateOf("") }
    var actualCashStr by remember { mutableStateOf("") }

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LaunchedEffect(Unit) {
        viewModel.loadShiftData(storeId)
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUiMessage()
        }
    }

    val isDark = isSystemInDarkTheme()
    val greenColor = if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32)
    val redColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFC62828)
    val primaryTealColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF00796B)
    val buttonTealColor = if (isDark) Color(0xFF0F766E) else Color(0xFF00796B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shift Kasir (Cash Shift)") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00796B), // Teal
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val shift = activeShift
            if (shift == null) {
                // START NEW SHIFT
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Mulai Shift Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Masukkan modal awal kasir untuk uang kembalian di laci kasir.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = openingCashStr,
                            onValueChange = { openingCashStr = it },
                            label = { Text("Modal Awal Laci Kasir (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val opening = openingCashStr.toDoubleOrNull() ?: 0.0
                                viewModel.startShift(storeId, opening) {
                                    openingCashStr = ""
                                }
                            },
                            enabled = openingCashStr.isNotEmpty() && (openingCashStr.toDoubleOrNull() ?: 0.0) >= 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = buttonTealColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Buka Shift Kasir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // CLOSE ACTIVE SHIFT
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Shift Aktif Kasir", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryTealColor)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Waktu Mulai:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeFormat.format(Date(shift.startTime)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Modal Awal (Opening):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(rpFormatter.format(shift.openingCash), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Perkiraan Laci (Expected):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(rpFormatter.format(expectedCash), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTealColor)
                        }

                        Divider()

                        OutlinedTextField(
                            value = actualCashStr,
                            onValueChange = { actualCashStr = it },
                            label = { Text("Jumlah Uang Fisik Aktual (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val actual = actualCashStr.toDoubleOrNull() ?: 0.0
                        if (actualCashStr.isNotEmpty()) {
                            val diff = actual - expectedCash
                            val diffColor = if (diff >= 0) greenColor else redColor
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Selisih Kas (Difference):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = rpFormatter.format(diff),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = diffColor
                                )
                            }
                        }
 
                        Button(
                            onClick = {
                                viewModel.closeShift(storeId, actual) {
                                    actualCashStr = ""
                                }
                            },
                            enabled = actualCashStr.isNotEmpty() && (actualCashStr.toDoubleOrNull() ?: 0.0) >= 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = buttonTealColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tutup Shift Kasir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SHIFT LOGS HISTORICAL LIST
            Text("Riwayat Shift Kasir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shiftList) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Waktu: " + timeFormat.format(Date(s.startTime)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val statusColor = if (s.status == "ACTIVE") primaryTealColor else (if (isDark) Color(0xFF94A3B8) else Color.DarkGray)
                                Text(s.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Modal Awal: " + rpFormatter.format(s.openingCash), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (s.status == "CLOSED") {
                                    Text("Fisik: " + rpFormatter.format(s.closingCash ?: 0.0), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
 
                            if (s.status == "CLOSED") {
                                val diff = s.cashDifference ?: 0.0
                                val diffColor = if (diff >= 0) greenColor else redColor
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Perkiraan: " + rpFormatter.format(s.expectedCash ?: 0.0), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Selisih: " + rpFormatter.format(diff), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = diffColor)
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
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
