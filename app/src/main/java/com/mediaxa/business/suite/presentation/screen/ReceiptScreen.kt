package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.StoreSetting
import com.mediaxa.business.suite.data.local.entity.Transaction
import com.mediaxa.business.suite.data.local.entity.TransactionItem
import com.mediaxa.business.suite.data.repository.StoreSettingRepository
import com.mediaxa.business.suite.data.repository.TransactionRepository
import com.mediaxa.business.suite.data.printing.PrinterService
import com.mediaxa.business.suite.data.printing.PrintResult
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptScreen(
    transactionUuid: String,
    transactionRepository: TransactionRepository,
    storeSettingRepository: StoreSettingRepository,
    onFinishClick: () -> Unit
) {
    val context = LocalContext.current
    val rpFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var items by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var storeSetting by remember { mutableStateOf<StoreSetting?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val printerService = remember { PrinterService() }
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }

    LaunchedEffect(transactionUuid) {
        isLoading = true
        transaction = transactionRepository.getTransactionByUuid(transactionUuid)
        items = transactionRepository.getItemsForTransaction(transactionUuid)
        storeSetting = storeSettingRepository.getSettings()
        isLoading = false
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Transaksi tidak ditemukan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFinishClick) {
                        Text("Kembali")
                    }
                }
            }
        } else {
            val tx = transaction!!
            val setting = storeSetting

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Success Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transaksi Berhasil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Receipt Ticket Layout
                Card(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Info
                        Text(
                            text = setting?.storeName ?: "POS UMKM",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = setting?.address ?: "Alamat Toko UMKM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Telp: ${setting?.phoneNumber ?: "-"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Meta Info
                        ReceiptMetaLine(label = "No. Transaksi", value = tx.transactionNumber)
                        ReceiptMetaLine(label = "Tanggal", value = dateFormat.format(Date(tx.timestamp)))
                        ReceiptMetaLine(label = "Kasir", value = tx.cashierName)
                        ReceiptMetaLine(label = "Status", value = tx.status)

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Items Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Menu / Item", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(2f))
                            Text("Qty", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                            Text("Total", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Items list
                        items.forEach { item ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.menuName,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        modifier = Modifier.weight(2f)
                                    )
                                    Text(
                                        text = "${item.quantity}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        modifier = Modifier.weight(0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = rpFormatter.format(item.subtotal),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1.5f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Text(
                                    text = "  @${rpFormatter.format(item.price)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Totals Breakdown
                        ReceiptPriceLine(label = "Subtotal", amount = tx.subtotal, rpFormatter = rpFormatter)
                        if (tx.discount > 0) {
                            ReceiptPriceLine(label = "Diskon", amount = -tx.discount, rpFormatter = rpFormatter)
                        }
                        
                        // Calculate tax and service in receipt based on settings if active at the time
                        val subtotalAfterDisc = (tx.subtotal - tx.discount).coerceAtLeast(0.0)
                        // Verify if total implies tax or service charge
                        // In PosViewModel, total = subtotalAfterDisc + tax + serviceCharge.
                        // Since we didn't save separate columns for tax/service in Transaction entity,
                        // we can determine if they were applied by checking if isTaxEnabled or isServiceChargeEnabled is true.
                        // We can also calculate what they would be to match the total.
                        val isTaxEnabled = setting?.isTaxEnabled == true
                        val isServiceChargeEnabled = setting?.isServiceChargeEnabled == true
                        val tax = if (isTaxEnabled) subtotalAfterDisc * 0.10 else 0.0
                        val serviceCharge = if (isServiceChargeEnabled) subtotalAfterDisc * 0.05 else 0.0

                        if (tax > 0) {
                            ReceiptPriceLine(label = "Pajak (10%)", amount = tax, rpFormatter = rpFormatter)
                        }
                        if (serviceCharge > 0) {
                            ReceiptPriceLine(label = "Service Charge (5%)", amount = serviceCharge, rpFormatter = rpFormatter)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "TOTAL",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                rpFormatter.format(tx.total),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Payment Details
                        ReceiptMetaLine(label = "Metode Bayar", value = tx.paymentMethod)
                        ReceiptPriceLine(label = "Diterima", amount = tx.amountReceived, rpFormatter = rpFormatter)
                        ReceiptPriceLine(label = "Kembalian", amount = tx.changeAmount, rpFormatter = rpFormatter)

                        if (tx.paymentMethod == "TRANSFER") {
                            val bankNameStr = setting?.bankName ?: "-"
                            val acctNoStr = setting?.bankAccountNumber ?: "-"
                            val holderNameStr = setting?.bankAccountHolderName ?: "-"
                            Text(
                                text = "------------------------------------------",
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Transfer Ke Rekening:\n$bankNameStr - $acctNoStr\nA/N: $holderNameStr",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "------------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Footer
                        Text(
                            text = setting?.receiptFooter ?: "Terima Kasih atas Kunjungan Anda!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Panel
                Row(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPrinting) return@OutlinedButton
                            scope.launch {
                                isPrinting = true
                                Toast.makeText(context, "Menghubungkan ke printer...", Toast.LENGTH_SHORT).show()
                                val result = printerService.printReceipt(tx, items, setting ?: StoreSetting(
                                    storeName = "Mediaxa Business Suite",
                                    address = "Jl. Raya UMKM No. 1",
                                    phoneNumber = "081234567890",
                                    receiptFooter = "Terima Kasih Atas Kunjungan Anda"
                                ))
                                isPrinting = false
                                when (result) {
                                    is PrintResult.Success -> {
                                        Toast.makeText(context, "Struk berhasil dicetak!", Toast.LENGTH_SHORT).show()
                                    }
                                    is PrintResult.Failure -> {
                                        Toast.makeText(context, "Gagal mencetak: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cetak Struk")
                        }
                    }

                    Button(
                        onClick = onFinishClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transaksi Baru")
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun ReceiptMetaLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.DarkGray)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun ReceiptPriceLine(label: String, amount: Double, rpFormatter: NumberFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.DarkGray)
        Text(rpFormatter.format(amount), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
    }
}
