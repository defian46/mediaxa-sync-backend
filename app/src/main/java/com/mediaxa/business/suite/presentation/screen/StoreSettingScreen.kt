package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.StoreSetting
import com.mediaxa.business.suite.presentation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val storeSettings by viewModel.storeSettings.collectAsState()
    val context = LocalContext.current

    var storeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var receiptFooter by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccountNumber by remember { mutableStateOf("") }
    var bankAccountHolderName by remember { mutableStateOf("") }

    LaunchedEffect(storeSettings) {
        storeSettings?.let {
            storeName = it.storeName
            address = it.address
            phoneNumber = it.phoneNumber
            receiptFooter = it.receiptFooter
            bankName = it.bankName ?: ""
            bankAccountNumber = it.bankAccountNumber ?: ""
            bankAccountHolderName = it.bankAccountHolderName ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Toko & Printer", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Informasi Struk Toko", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Toko") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Nomor HP Toko") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Footer Struk") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Pengaturan Rekening Bank (Untuk Pembayaran Transfer)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Nama Bank") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Contoh: BCA, Mandiri, BRI") }
                    )

                    OutlinedTextField(
                        value = bankAccountNumber,
                        onValueChange = { bankAccountNumber = it },
                        label = { Text("Nomor Rekening") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankAccountHolderName,
                        onValueChange = { bankAccountHolderName = it },
                        label = { Text("Atas Nama Rekening") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (storeName.isEmpty() || address.isEmpty() || phoneNumber.isEmpty() || receiptFooter.isEmpty()) {
                        Toast.makeText(context, "Nama, Alamat, No HP, dan Footer struk wajib diisi!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val current = storeSettings ?: StoreSetting(
                        storeName = storeName,
                        address = address,
                        phoneNumber = phoneNumber,
                        receiptFooter = receiptFooter
                    )
                    val updated = current.copy(
                        storeName = storeName,
                        address = address,
                        phoneNumber = phoneNumber,
                        receiptFooter = receiptFooter,
                        bankName = bankName.ifEmpty { null },
                        bankAccountNumber = bankAccountNumber.ifEmpty { null },
                        bankAccountHolderName = bankAccountHolderName.ifEmpty { null },
                        updatedAt = System.currentTimeMillis()
                    )
                    viewModel.updateStoreSettings(updated)
                    Toast.makeText(context, "Pengaturan toko berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Pengaturan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
