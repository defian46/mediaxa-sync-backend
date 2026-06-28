package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.MenuRecipe
import com.mediaxa.business.suite.presentation.theme.TealSuccess
import com.mediaxa.business.suite.presentation.viewmodel.InventoryViewModel
import com.mediaxa.business.suite.presentation.viewmodel.ProductViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBomScreen(
    productViewModel: ProductViewModel,
    inventoryViewModel: InventoryViewModel,
    menuUuid: String,
    onBackClick: () -> Unit
) {
    val menus by productViewModel.menus.collectAsState()
    val ingredients by inventoryViewModel.ingredients.collectAsState()

    val menu = remember(menuUuid, menus) {
        menus.find { it.uuid == menuUuid }
    }

    var recipeItems by remember { mutableStateOf(listOf<MenuRecipe>()) }

    LaunchedEffect(menuUuid) {
        productViewModel.recalculateMenuCosts(menuUuid)
        val savedRecipes = productViewModel.getRecipeForMenu(menuUuid)
        recipeItems = savedRecipes
    }

    var showAddIngredientDialog by remember { mutableStateOf(false) }
    var selectedIngredientUuid by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("") }

    val rpFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    val estimatedHpp = remember(recipeItems, ingredients) {
        recipeItems.sumOf { item ->
            val ing = ingredients.find { it.uuid == item.ingredientUuid }
            (ing?.unitPrice ?: 0.0) * item.quantityNeeded
        }
    }

    val margin = remember(estimatedHpp, menu) {
        val price = menu?.price ?: 0.0
        if (price > 0.0) ((price - estimatedHpp) / price) * 100 else 0.0
    }

    if (showAddIngredientDialog) {
        AlertDialog(
            onDismissRequest = { showAddIngredientDialog = false },
            title = { Text("Tambah Bahan Baku Resep") },
            text = {
                Column {
                    var expandedDropdown by remember { mutableStateOf(false) }
                    val selectedIng = ingredients.find { it.uuid == selectedIngredientUuid }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedIng?.name ?: "Pilih Bahan Baku")
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ingredients.forEach { ing ->
                                if (recipeItems.none { it.ingredientUuid == ing.uuid }) {
                                    DropdownMenuItem(
                                        text = { Text("${ing.name} (${ing.unit})") },
                                        onClick = {
                                            selectedIngredientUuid = ing.uuid
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Jumlah Kebutuhan (${selectedIng?.unit ?: "satuan"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantityInput.toDoubleOrNull() ?: 0.0
                        if (selectedIngredientUuid.isNotEmpty() && qty > 0.0) {
                            val newItem = MenuRecipe(
                                menuUuid = menuUuid,
                                ingredientUuid = selectedIngredientUuid,
                                quantityNeeded = qty
                            )
                            recipeItems = recipeItems + newItem
                        }
                        showAddIngredientDialog = false
                        selectedIngredientUuid = ""
                        quantityInput = ""
                    }
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddIngredientDialog = false
                    selectedIngredientUuid = ""
                    quantityInput = ""
                }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resep / BOM - ${menu?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddIngredientDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Bahan Baku", tint = Color.White)
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
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Harga Jual", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(rpFormatter.format(menu?.price ?: 0.0), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HPP Estimasi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(rpFormatter.format(estimatedHpp), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Margin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text("${"%.1f".format(margin)}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealSuccess)
                    }
                }
            }

            Text(
                text = "Bahan Baku Yang Digunakan:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (recipeItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada bahan baku dalam resep menu ini.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recipeItems) { item ->
                        val ingredient = ingredients.find { it.uuid == item.ingredientUuid }
                        if (ingredient != null) {
                            val cost = ingredient.unitPrice * item.quantityNeeded
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(ingredient.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Kebutuhan: ${item.quantityNeeded} ${ingredient.unit} (Biaya: ${rpFormatter.format(cost)})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            recipeItems = recipeItems.filterNot { it.ingredientUuid == item.ingredientUuid }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        productViewModel.saveRecipe(menuUuid, recipeItems)
                        onBackClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan Resep & Hitung HPP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
