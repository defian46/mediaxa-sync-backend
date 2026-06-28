package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import android.net.Uri
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.presentation.viewmodel.ProductViewModel
import com.mediaxa.business.suite.presentation.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMenuScreen(
    viewModel: ProductViewModel,
    menuUuid: String?,
    onBackClick: () -> Unit
) {
    val categories by viewModel.activeCategories.collectAsState()
    val menus by viewModel.menus.collectAsState()
    val context = LocalContext.current

    val existingMenu = remember(menuUuid, menus) {
        menus.find { it.uuid == menuUuid }
    }

    var name by remember { mutableStateOf("") }
    var categoryUuid by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var promoPrice by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf("") }

    var expandedCategoryMenu by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = ImageStorageHelper.saveImageToInternalStorage(context, it)
            if (localPath != null) {
                imagePath = localPath
                Toast.makeText(context, "Gambar berhasil dimuat", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(existingMenu) {
        existingMenu?.let {
            name = it.name
            categoryUuid = it.categoryUuid
            price = it.price.toString()
            promoPrice = it.promoPrice?.toString() ?: ""
            description = it.description ?: ""
            imagePath = it.imagePath ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingMenu == null) "Tambah Menu" else "Ubah Menu", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Menu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selectedCategoryName = categories.find { it.uuid == categoryUuid }?.name ?: "Pilih Kategori"
                        OutlinedButton(
                            onClick = { expandedCategoryMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedCategoryName)
                        }
                        DropdownMenu(
                            expanded = expandedCategoryMenu,
                            onDismissRequest = { expandedCategoryMenu = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        categoryUuid = category.uuid
                                        expandedCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Harga Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = promoPrice,
                        onValueChange = { promoPrice = it },
                        label = { Text("Harga Promo (Rp) - Opsional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Menu (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Foto Menu", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val imageBitmap = remember(imagePath) {
                        if (imagePath.isNotEmpty()) {
                            val file = File(imagePath)
                            if (file.exists()) {
                                ImageUtils.decodeSampledBitmapFromFile(file.absolutePath, 300, 300)?.asImageBitmap()
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Preview Foto Menu",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                              )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ketuk untuk Pilih Foto Menu", 
                                    fontSize = 12.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val pr = price.toDoubleOrNull() ?: 0.0
                    val prPromo = promoPrice.toDoubleOrNull()
                    if (name.isNotEmpty() && categoryUuid.isNotEmpty() && pr > 0.0) {
                        val currentMenu = existingMenu
                        if (currentMenu == null) {
                            viewModel.addMenu(
                                name = name,
                                categoryUuid = categoryUuid,
                                price = pr,
                                promoPrice = prPromo,
                                imagePath = imagePath.ifEmpty { null },
                                description = description.ifEmpty { null }
                            )
                        } else {
                            viewModel.updateMenu(
                                currentMenu.copy(
                                    name = name,
                                    categoryUuid = categoryUuid,
                                    price = pr,
                                    promoPrice = prPromo,
                                    imagePath = imagePath.ifEmpty { null },
                                    description = description.ifEmpty { null }
                                )
                            )
                        }
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
