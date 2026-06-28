package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mediaxa.business.suite.presentation.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    customerUuid: String,
    viewModel: CustomerViewModel,
    onBackClick: () -> Unit,
    onManageLoyalty: (String, String) -> Unit
) {
    LaunchedEffect(customerUuid) {
        viewModel.selectCustomer(customerUuid)
    }

    val customer by viewModel.selectedCustomer.collectAsState()
    val favMenuUuid by viewModel.favoriteMenuUuid.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        val currentCust = customer
        if (currentCust == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentCust.customerName,
                                style = MaterialTheme.typography.headlineMedium
                             )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(onClick = {}, label = { Text(currentCust.membershipLevel) })
                                SuggestionChip(onClick = {}, label = { Text(currentCust.customerCode) })
                            }
                        }
                    }
                }

                item {
                    Text("Detail Kontak & Alamat", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("No. HP: ${currentCust.phone ?: "-"}")
                            Text("Email: ${currentCust.email ?: "-"}")
                            Text("Alamat: ${currentCust.address ?: "-"}")
                            Text("Catatan: ${currentCust.notes ?: "-"}")
                        }
                    }
                }

                item {
                    Text("Statistik Pembelian", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Total Pengeluaran: Rp ${String.format("%,.0f", currentCust.totalSpending)}")
                            Text("Kunjungan Terakhir: ${currentCust.lastVisit?.let { 
                                java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) 
                            } ?: "-"}")
                            Text("Menu Terfavorit (UUID): ${favMenuUuid ?: "-"}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onManageLoyalty(currentCust.uuid, currentCust.customerName) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Kelola Poin Loyalitas")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && customer != null) {
        val activeCust = customer!!
        AddEditCustomerDialog(
            customer = activeCust,
            onDismiss = { showEditDialog = false },
            onSave = { code, name, phone, email, bday, gender, address, notes, tier ->
                viewModel.saveCustomer(
                    uuid = activeCust.uuid,
                    code = code,
                    name = name,
                    phone = phone,
                    email = email,
                    birthday = bday,
                    gender = gender,
                    address = address,
                    notes = notes,
                    membershipLevel = tier,
                    onSuccess = { showEditDialog = false },
                    onError = { }
                )
            }
        )
    }
}
