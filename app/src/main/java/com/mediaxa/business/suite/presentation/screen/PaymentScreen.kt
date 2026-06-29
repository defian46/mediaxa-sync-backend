package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.repository.CheckoutResult
import com.mediaxa.business.suite.data.local.entity.User
import com.mediaxa.business.suite.presentation.viewmodel.PosViewModel
import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp " + formatter.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PosViewModel,
    currentUser: User,
    onBackClick: () -> Unit,
    onCheckoutSuccess: (String) -> Unit
) {
    val subtotal by viewModel.subtotal.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val taxAmount by viewModel.taxAmount.collectAsState()
    val serviceChargeAmount by viewModel.serviceChargeAmount.collectAsState()
    val total by viewModel.total.collectAsState()

    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val selectedCustomerPoints by viewModel.selectedCustomerPoints.collectAsState()
    val redeemedPoints by viewModel.redeemedPoints.collectAsState()
    val storeSetting by viewModel.storeSetting.collectAsState()
    val pointsDiscount by viewModel.pointsDiscount.collectAsState()

    val context = LocalContext.current

    var selectedMethod by remember { mutableStateOf("CASH") } // "CASH", "QRIS", "TRANSFER"
    var cashReceivedInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val cashReceivedAmount = if (selectedMethod == "CASH") {
        cashReceivedInput.toDoubleOrNull() ?: 0.0
    } else {
        total
    }

    val changeAmount = (cashReceivedAmount - total).coerceAtLeast(0.0)
    val isCashSufficient = cashReceivedAmount >= total

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pembayaran", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(0.4f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kembali", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (selectedMethod == "CASH" && !isCashSufficient) {
                                Toast.makeText(context, "Uang pembayaran kurang!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isProcessing = true
                            viewModel.checkout(
                                context = context,
                                cashierUuid = currentUser.uuid,
                                cashierName = currentUser.username,
                                paymentMethod = selectedMethod,
                                amountReceived = cashReceivedAmount
                            ) { result ->
                                isProcessing = false
                                when (result) {
                                    is CheckoutResult.Success -> {
                                        Toast.makeText(context, "Pembayaran berhasil", Toast.LENGTH_SHORT).show()
                                        onCheckoutSuccess(result.transactionUuid)
                                    }
                                    is CheckoutResult.Failure -> {
                                        showErrorDialog(context, result.errorMsg)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(0.6f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isProcessing && (selectedMethod != "CASH" || isCashSufficient)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Konfirmasi Pembayaran", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Ringkasan Transaksi (Top)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Transaksi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    DetailRow(label = "Subtotal", amount = subtotal)
                    if (discountAmount > 0) {
                        DetailRow(label = "Diskon Manual", amount = -discountAmount, isDiscount = true)
                    }
                    val promotionDiscount by viewModel.promotionDiscount.collectAsState()
                    if (promotionDiscount > 0) {
                        DetailRow(label = "Diskon Promo", amount = -promotionDiscount, isDiscount = true)
                    }
                    if (pointsDiscount > 0) {
                        DetailRow(label = "Redeem Poin", amount = -pointsDiscount, isDiscount = true)
                    }
                    if (taxAmount > 0) {
                        DetailRow(label = "Pajak (10%)", amount = taxAmount)
                    }
                    if (serviceChargeAmount > 0) {
                        DetailRow(label = "Service Charge (5%)", amount = serviceChargeAmount)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Bayar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = formatRupiah(total),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Optional Loyalty Point Section
            if (selectedCustomer != null) {
                val pointsValue = storeSetting?.loyaltyPointsValue ?: 100.0
                val maxRedeemablePoints = selectedCustomerPoints
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Program Loyalitas - Poin Member",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${selectedCustomer!!.customerName} memiliki $selectedCustomerPoints Poin (1 Poin = Rp ${String.format("%.0f", pointsValue)})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        
                        if (maxRedeemablePoints > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                var pointsInputStr by remember { mutableStateOf(if (redeemedPoints > 0) redeemedPoints.toString() else "") }
                                
                                OutlinedTextField(
                                    value = pointsInputStr,
                                    onValueChange = { input ->
                                        pointsInputStr = input
                                        val parsedPts = input.toIntOrNull() ?: 0
                                        val cappedPts = parsedPts.coerceIn(0, maxRedeemablePoints)
                                        viewModel.setRedeemedPoints(cappedPts)
                                    },
                                    label = { Text("Redeem Poin (Maks $maxRedeemablePoints)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(180.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Button(
                                    onClick = {
                                        viewModel.setRedeemedPoints(maxRedeemablePoints)
                                        pointsInputStr = maxRedeemablePoints.toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Gunakan Semua", fontSize = 12.sp)
                                }
                            }
                            
                            if (redeemedPoints > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Diskon Poin Terpasang: -Rp ${String.format("%,.0f", pointsDiscount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada poin untuk di-redeem.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Section 2: Pilihan Metode Pembayaran (Middle)
            Text(
                text = "Pilihan Metode Pembayaran",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val methods = listOf(
                    Triple("CASH", "Tunai", Icons.Default.Money),
                    Triple("QRIS", "QRIS", Icons.Default.QrCode),
                    Triple("TRANSFER", "Transfer", Icons.Default.AccountBalance)
                )

                methods.forEach { (code, label, icon) ->
                    val isSelected = selectedMethod == code
                    Card(
                        onClick = {
                            selectedMethod = code
                            if (code != "CASH") {
                                cashReceivedInput = ""
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                        border = if (isSelected) {
                            borderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            borderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Section 3: Conditional content based on selectedMethod
            if (selectedMethod == "CASH") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Pembayaran Tunai",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        OutlinedTextField(
                            value = cashReceivedInput,
                            onValueChange = { cashReceivedInput = it },
                            label = { Text("Uang diterima (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Quick cash options
                        val cashOptions = listOf(10000.0, 20000.0, 50000.0, 100000.0)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cashOptions.take(2).forEach { amount ->
                                    Button(
                                        onClick = { cashReceivedInput = amount.toInt().toString() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(formatRupiah(amount), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cashOptions.drop(2).forEach { amount ->
                                    Button(
                                        onClick = { cashReceivedInput = amount.toInt().toString() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(formatRupiah(amount), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = { cashReceivedInput = total.toInt().toString() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Uang Pas", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Kembalian
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kembalian",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isCashSufficient) formatRupiah(changeAmount) else "Uang Kurang",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = if (isCashSufficient) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }

                        if (!isCashSufficient) {
                            Text(
                                text = "Peringatan: Uang yang diterima kurang dari total bayar!",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (selectedMethod == "QRIS") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Instruksi Pembayaran QRIS:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Minta pelanggan scan QRIS toko.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (selectedMethod == "TRANSFER") {
                val hasBankAccount = !storeSetting?.bankAccountNumber.isNullOrBlank()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Informasi Rekening Toko",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        if (hasBankAccount) {
                            val bankNameStr = storeSetting?.bankName ?: ""
                            val acctNoStr = storeSetting?.bankAccountNumber ?: ""
                            val holderNameStr = storeSetting?.bankAccountHolderName ?: ""

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Nama Bank: $bankNameStr", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Nomor Rekening: $acctNoStr", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    IconButton(
                                        onClick = {
                                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("No. Rekening", acctNoStr)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, "Nomor rekening disalin", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin Nomor Rekening",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(text = "Atas Nama: $holderNameStr", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Total Bayar: ${formatRupiah(total)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("No. Rekening", acctNoStr)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Nomor rekening disalin", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Salin Nomor Rekening", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Nomor rekening toko belum diatur",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
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

@Composable
fun DetailRow(
    label: String,
    amount: Double,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        Text(
            text = formatRupiah(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDiscount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)

private fun showErrorDialog(context: android.content.Context, message: String) {
    android.app.AlertDialog.Builder(context)
        .setTitle("Transaksi Gagal")
        .setMessage(message)
        .setPositiveButton("OK", null)
        .show()
}
