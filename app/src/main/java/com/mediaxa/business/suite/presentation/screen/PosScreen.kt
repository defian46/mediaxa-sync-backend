package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import android.graphics.BitmapFactory
import com.mediaxa.business.suite.presentation.util.ImageUtils
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.Menu
import com.mediaxa.business.suite.data.repository.StockValidationResult
import com.mediaxa.business.suite.presentation.viewmodel.PosViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onBackClick: () -> Unit,
    onNavigateToPayment: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryUuid by viewModel.selectedCategoryUuid.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredMenus by viewModel.filteredMenus.collectAsState()
    
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val total by viewModel.total.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val selectedCustomerPoints by viewModel.selectedCustomerPoints.collectAsState()

    val context = LocalContext.current
    val rpFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    var showDiscountDialog by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf("") }

    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedCartItemMenu by remember { mutableStateOf<Menu?>(null) }
    var noteInput by remember { mutableStateOf("") }
    
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showMobileCartSheet by remember { mutableStateOf(false) }

    if (showDiscountDialog) {
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Input Diskon Nominal") },
            text = {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("Diskon (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val disc = discountInput.toDoubleOrNull() ?: 0.0
                        if (disc <= subtotal) {
                            viewModel.setDiscount(disc)
                            showDiscountDialog = false
                        } else {
                            Toast.makeText(context, "Diskon tidak boleh melebihi subtotal", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showNoteDialog && selectedCartItemMenu != null) {
        AlertDialog(
            onDismissRequest = { 
                showNoteDialog = false 
                selectedCartItemMenu = null
            },
            title = { Text("Tambah Catatan Item - ${selectedCartItemMenu!!.name}") },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCartItemNote(selectedCartItemMenu!!, noteInput.ifEmpty { null })
                        showNoteDialog = false
                        selectedCartItemMenu = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNoteDialog = false 
                    selectedCartItemMenu = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showCustomerDialog) {
        val posCustomers by viewModel.posCustomers.collectAsState()
        val customerSearchQuery by viewModel.customerSearchQuery.collectAsState()

        AlertDialog(
            onDismissRequest = { 
                showCustomerDialog = false 
                viewModel.setCustomerSearchQuery("")
            },
            title = { Text("Pilih Pelanggan") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = { viewModel.setCustomerSearchQuery(it) },
                        placeholder = { Text("Cari nama/kode/hp...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (posCustomers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1.0f), contentAlignment = Alignment.Center) {
                            Text("Tidak ada pelanggan ditemukan", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().weight(1.0f)
                        ) {
                            items(posCustomers) { customer ->
                                Card(
                                    onClick = {
                                        viewModel.selectCustomer(customer)
                                        showCustomerDialog = false
                                        viewModel.setCustomerSearchQuery("")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(customer.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Kode: ${customer.customerCode} | Tier: ${customer.membershipLevel}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomerDialog = false
                        viewModel.setCustomerSearchQuery("")
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showMobileCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMobileCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CartContent(
                viewModel = viewModel,
                rpFormatter = rpFormatter,
                onNavigateToPayment = {
                    showMobileCartSheet = false
                    onNavigateToPayment()
                },
                showCustomerDialog = { showCustomerDialog = true },
                showNoteDialog = { menu, note ->
                    selectedCartItemMenu = menu
                    noteInput = note
                    showNoteDialog = true
                },
                showDiscountDialog = {
                    discountInput = discountAmount.toString()
                    showDiscountDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir / POS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearCart() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Bersihkan Keranjang", tint = Color.White)
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isTablet = maxWidth >= 720.dp

            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Column (Menu selection)
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Cari menu makanan / minuman...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryUuid == null,
                                    onClick = { viewModel.selectCategory(null) },
                                    label = { Text("Semua", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategoryUuid == cat.uuid,
                                    onClick = { viewModel.selectCategory(cat.uuid) },
                                    label = { Text(cat.name, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredMenus.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Tidak ada menu aktif yang ditemukan", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredMenus) { menu ->
                                    val imageBitmap = remember(menu.imagePath) {
                                        if (!menu.imagePath.isNullOrEmpty()) {
                                            val file = File(menu.imagePath)
                                            if (file.exists()) {
                                                ImageUtils.decodeSampledBitmapFromFile(file.absolutePath, 200, 200)?.asImageBitmap()
                                            } else {
                                                null
                                            }
                                        } else {
                                            null
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.addToCart(menu) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.LightGray),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (imageBitmap != null) {
                                                    Image(
                                                        bitmap = imageBitmap,
                                                        contentDescription = menu.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = menu.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = rpFormatter.format(menu.price),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column (Cart details)
                    CartContent(
                        viewModel = viewModel,
                        rpFormatter = rpFormatter,
                        onNavigateToPayment = onNavigateToPayment,
                        showCustomerDialog = { showCustomerDialog = true },
                        showNoteDialog = { menu, note ->
                            selectedCartItemMenu = menu
                            noteInput = note
                            showNoteDialog = true
                        },
                        showDiscountDialog = {
                            discountInput = discountAmount.toString()
                            showDiscountDialog = true
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    )
                }
            } else {
                // Mobile layout
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Cari menu...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryUuid == null,
                                    onClick = { viewModel.selectCategory(null) },
                                    label = { Text("Semua", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategoryUuid == cat.uuid,
                                    onClick = { viewModel.selectCategory(cat.uuid) },
                                    label = { Text(cat.name, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredMenus.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Tidak ada menu aktif yang ditemukan", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(130.dp),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredMenus) { menu ->
                                    val imageBitmap = remember(menu.imagePath) {
                                        if (!menu.imagePath.isNullOrEmpty()) {
                                            val file = File(menu.imagePath)
                                            if (file.exists()) {
                                                ImageUtils.decodeSampledBitmapFromFile(file.absolutePath, 150, 150)?.asImageBitmap()
                                            } else {
                                                null
                                            }
                                        } else {
                                            null
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.addToCart(menu)
                                                Toast.makeText(context, "${menu.name} ditambahkan", Toast.LENGTH_SHORT).show()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(85.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.LightGray),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (imageBitmap != null) {
                                                    Image(
                                                        bitmap = imageBitmap,
                                                        contentDescription = menu.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = menu.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = rpFormatter.format(menu.price),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (cartItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Bayar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text(
                                        rpFormatter.format(total),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = { showMobileCartSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Keranjang (${cartItems.sumOf { it.quantity }})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
fun CartContent(
    viewModel: PosViewModel,
    rpFormatter: java.text.NumberFormat,
    onNavigateToPayment: () -> Unit,
    showCustomerDialog: () -> Unit,
    showNoteDialog: (Menu, String) -> Unit,
    showDiscountDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val total by viewModel.total.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val selectedCustomerPoints by viewModel.selectedCustomerPoints.collectAsState()
    val promotionDiscount by viewModel.promotionDiscount.collectAsState()
    val pointsDiscount by viewModel.pointsDiscount.collectAsState()
    val context = LocalContext.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Keranjang Belanja",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = { viewModel.clearCart() }) {
                Text("Hapus Semua", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCustomer == null) {
                OutlinedButton(
                    onClick = showCustomerDialog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hubungkan Pelanggan (CRM)")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = selectedCustomer!!.customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tier: ${selectedCustomer!!.membershipLevel} | Poin: $selectedCustomerPoints",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = { viewModel.selectCustomer(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        if (cartItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Keranjang belanja kosong", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(cartItems) { item ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(item.menu.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    rpFormatter.format(item.menu.price * item.quantity),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (item.note != null) {
                                    Text(
                                        "Catatan: ${item.note}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { viewModel.decreaseQty(item.menu) },
                                    modifier = Modifier.size(38.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(18.dp))
                                }
                                Text(item.quantity.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                FilledTonalIconButton(
                                    onClick = { viewModel.increaseQty(item.menu) },
                                    modifier = Modifier.size(38.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(18.dp))
                                }
                            }
                            
                            IconButton(
                                onClick = { showNoteDialog(item.menu, item.note ?: "") },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.NoteAdd, contentDescription = "Catatan", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(rpFormatter.format(subtotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDiscountDialog() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Diskon Manual (-)", 
                    fontSize = 13.sp, 
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(rpFormatter.format(discountAmount), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            if (promotionDiscount > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Diskon Promo (-)", fontSize = 13.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    Text(rpFormatter.format(promotionDiscount), fontSize = 13.sp, color = Color(0xFF4CAF50))
                }
            }
            if (pointsDiscount > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Redeem Poin (-)", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(rpFormatter.format(pointsDiscount), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Bayar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    rpFormatter.format(total),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (cartItems.isEmpty()) {
                        Toast.makeText(context, "Keranjang belanja kosong", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.checkStock { result ->
                        when (result) {
                            is StockValidationResult.Valid -> {
                                onNavigateToPayment()
                            }
                            is StockValidationResult.LackingIngredients -> {
                                val lackDetails = result.list.joinToString("\n") {
                                    "• ${it.ingredientName}: Butuh ${it.required} ${it.unit}, Tersedia ${it.available} ${it.unit}"
                                }
                                AlertDialog(
                                    context = context,
                                    title = "Stok Bahan Baku Tidak Cukup",
                                    message = "Checkout dibatalkan karena persediaan bahan baku kurang:\n\n$lackDetails"
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Bayar / Checkout", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun AlertDialog(context: android.content.Context, title: String, message: String) {
    android.app.AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("OK", null)
        .show()
}
