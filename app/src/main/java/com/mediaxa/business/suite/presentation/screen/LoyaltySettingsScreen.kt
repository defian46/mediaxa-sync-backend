package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mediaxa.business.suite.presentation.viewmodel.LoyaltyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltySettingsScreen(
    customerUuid: String,
    customerName: String,
    viewModel: LoyaltyViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(customerUuid) {
        viewModel.selectCustomer(customerUuid)
    }

    val pointHistory by viewModel.pointHistory.collectAsState()
    val balance by viewModel.pointsBalance.collectAsState()
    
    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustPointsStr by remember { mutableStateOf("") }
    var adjustNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loyalty Program - $customerName") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Saldo Poin", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$balance Poin",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAdjustDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sesuaikan Poin Manual")
                    }
                }
            }

            Text("Riwayat Transaksi Poin", style = MaterialTheme.typography.titleMedium)

            if (pointHistory.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1.0f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada riwayat poin", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1.0f).fillMaxWidth()
                ) {
                    items(pointHistory) { history ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        text = history.notes ?: (if (history.points > 0) "Dapatkan Poin" else "Redeem Poin"),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(history.createdAt)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = (if (history.points > 0) "+" else "") + "${history.points}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (history.points > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Sesuaikan Poin Manual") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = adjustPointsStr,
                        onValueChange = { adjustPointsStr = it },
                        label = { Text("Jumlah Poin (e.g. 5 atau -10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adjustNotes,
                        onValueChange = { adjustNotes = it },
                        label = { Text("Alasan Penyesuaian") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pts = adjustPointsStr.toIntOrNull() ?: 0
                        viewModel.adjustPointsManually(
                            customerUuid = customerUuid,
                            points = pts,
                            notes = adjustNotes,
                            onSuccess = {
                                showAdjustDialog = false
                                adjustPointsStr = ""
                                adjustNotes = ""
                            },
                            onError = {}
                        )
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
