package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.dao.IngredientUsageResult
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class InventoryDashboardMetrics(
    val totalStockValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val monthlyPurchases: Double = 0.0,
    val monthlyWaste: Double = 0.0,
    val topConsumed: List<IngredientUsageResult> = emptyList(),
    val topPurchased: List<IngredientUsageResult> = emptyList()
)

class InventoryLiteViewModel(
    private val inventoryRepository: InventoryRepository,
    private val purchaseExpenseRepository: PurchaseExpenseRepository,
    private val expenseRepository: ExpenseRepository,
    private val stockOpnameRepository: StockOpnameRepository,
    private val wasteRepository: WasteRepository,
    private val inventoryLiteRepository: InventoryLiteRepository
) : ViewModel() {

    // Ingredients for selector dropdowns
    val activeIngredients: StateFlow<List<Ingredient>> = inventoryRepository.getActiveIngredientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All expenses for list view
    val allExpenses: StateFlow<List<Expense>> = expenseRepository.getAllExpensesFlow(storeId = 1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All purchase expenses for list view
    val allPurchaseExpenses: StateFlow<List<PurchaseExpense>> = purchaseExpenseRepository.getAllPurchaseExpensesFlow(storeId = 1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All stock opnames for list view
    val allStockOpnames: StateFlow<List<StockOpname>> = stockOpnameRepository.getAllStockOpnamesFlow(storeId = 1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All waste logs for list view
    val allWasteLogs: StateFlow<List<WasteLog>> = wasteRepository.getAllWasteLogsFlow(storeId = 1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dashboardMetrics = MutableStateFlow(InventoryDashboardMetrics())
    val dashboardMetrics: StateFlow<InventoryDashboardMetrics> = _dashboardMetrics.asStateFlow()

    private val _uiStateMessage = MutableStateFlow<String?>(null)
    val uiStateMessage: StateFlow<String?> = _uiStateMessage.asStateFlow()

    init {
        loadDashboardMetrics()
    }

    fun clearUiMessage() {
        _uiStateMessage.value = null
    }

    fun loadDashboardMetrics() {
        viewModelScope.launch {
            try {
                val storeId = 1L
                val calendar = Calendar.getInstance()
                
                // Start of current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                // End of current month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfMonth = calendar.timeInMillis

                val stockValue = inventoryLiteRepository.getTotalStockAssetValue(storeId)
                val lowCount = inventoryLiteRepository.getLowStockCount(storeId)
                val outCount = inventoryLiteRepository.getOutOfStockCount(storeId)
                val purchasesTotal = inventoryLiteRepository.getMonthlyPurchasesTotal(storeId, startOfMonth, endOfMonth)
                val wasteTotal = inventoryLiteRepository.getMonthlyWasteTotal(storeId, startOfMonth, endOfMonth)
                val topCon = inventoryLiteRepository.getTopConsumedIngredients(storeId, startOfMonth, endOfMonth)
                val topPur = inventoryLiteRepository.getTopPurchasedIngredients(storeId, startOfMonth, endOfMonth)

                _dashboardMetrics.value = InventoryDashboardMetrics(
                    totalStockValue = stockValue,
                    lowStockCount = lowCount,
                    outOfStockCount = outCount,
                    monthlyPurchases = purchasesTotal,
                    monthlyWaste = wasteTotal,
                    topConsumed = topCon,
                    topPurchased = topPur
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- ACTIONS & VALIDATIONS ---

    fun addPurchaseExpense(
        purchaseDate: Long,
        purchasePlaceName: String?,
        notes: String?,
        paymentMethod: String,
        items: List<PurchaseExpenseItem>,
        onSuccess: () -> Unit
    ) {
        if (items.isEmpty()) {
            _uiStateMessage.value = "Daftar item belanja tidak boleh kosong"
            return
        }

        // Validations
        for (item in items) {
            if (item.quantity <= 0) {
                _uiStateMessage.value = "Jumlah beli item harus lebih besar dari 0"
                return
            }
            if (item.totalPrice < 0) {
                _uiStateMessage.value = "Harga total item tidak boleh minus"
                return
            }
        }

        viewModelScope.launch {
            val totalAmount = items.sumOf { it.totalPrice }
            val purchaseExpense = PurchaseExpense(
                purchaseDate = purchaseDate,
                purchasePlaceName = purchasePlaceName,
                paymentMethod = paymentMethod,
                notes = notes,
                totalAmount = totalAmount,
                storeId = 1L
            )

            // Associate items with header uuid
            val updatedItems = items.map { it.copy(purchaseExpenseUuid = purchaseExpense.uuid) }

            val success = purchaseExpenseRepository.recordPurchaseExpense(
                purchaseExpense = purchaseExpense,
                items = updatedItems,
                userUuid = "user-admin", // Default mock admin user
                paymentMethod = paymentMethod
            )

            if (success) {
                _uiStateMessage.value = "Pembelanjaan berhasil dicatat!"
                loadDashboardMetrics()
                onSuccess()
            } else {
                _uiStateMessage.value = "Gagal mencatat pembelanjaan"
            }
        }
    }

    fun addExpense(
        category: String,
        amount: Double,
        paymentMethod: String,
        notes: String?,
        expenseDate: Long,
        attachmentPath: String? = null,
        onSuccess: () -> Unit
    ) {
        if (amount < 0) {
            _uiStateMessage.value = "Nominal pengeluaran tidak boleh minus"
            return
        }

        val resolvedCategory = ExpenseCategory.entries.firstOrNull { 
            it.displayName.equals(category, ignoreCase = true) || it.name.equals(category, ignoreCase = true)
        }?.name ?: category

        viewModelScope.launch {
            val expense = Expense(
                expenseDate = expenseDate,
                category = resolvedCategory,
                amount = amount,
                notes = notes,
                userUuid = "user-admin",
                paymentMethod = paymentMethod,
                storeId = 1L,
                attachmentPath = attachmentPath
            )

            val id = expenseRepository.insertExpense(expense)
            if (id > 0) {
                _uiStateMessage.value = "Pengeluaran berhasil dicatat!"
                loadDashboardMetrics()
                onSuccess()
            } else {
                _uiStateMessage.value = "Gagal mencatat pengeluaran"
            }
        }
    }

    fun addStockOpname(
        opnameDate: Long,
        notes: String?,
        items: List<StockOpnameItem>,
        onSuccess: () -> Unit
    ) {
        if (items.isEmpty()) {
            _uiStateMessage.value = "Item opname tidak boleh kosong"
            return
        }

        // Validations
        for (item in items) {
            if (item.diffStock != 0.0 && item.notes.trim().isEmpty()) {
                _uiStateMessage.value = "Catatan wajib diisi jika terdapat selisih stok fisik"
                return
            }
        }

        viewModelScope.launch {
            val opname = StockOpname(
                opnameDate = opnameDate,
                userUuid = "user-admin",
                notes = notes,
                storeId = 1L
            )

            val updatedItems = items.map { it.copy(opnameUuid = opname.uuid) }

            val success = stockOpnameRepository.recordStockOpname(
                opname = opname,
                items = updatedItems,
                userUuid = "user-admin"
            )

            if (success) {
                _uiStateMessage.value = "Stock opname berhasil disimpan!"
                loadDashboardMetrics()
                onSuccess()
            } else {
                _uiStateMessage.value = "Gagal menyimpan stock opname"
            }
        }
    }

    fun addWasteLog(
        ingredientUuid: String,
        quantity: Double,
        reason: String,
        notes: String?,
        wasteDate: Long,
        onSuccess: () -> Unit
    ) {
        if (quantity <= 0) {
            _uiStateMessage.value = "Quantity terbuang harus lebih besar dari 0"
            return
        }

        viewModelScope.launch {
            // Check available stock
            val ingredient = inventoryRepository.getIngredientByUuid(ingredientUuid)
            if (ingredient == null) {
                _uiStateMessage.value = "Bahan baku tidak ditemukan"
                return@launch
            }

            if (quantity > ingredient.availableStock) {
                _uiStateMessage.value = "Waste quantity tidak boleh lebih besar dari stok tersedia (${ingredient.availableStock})"
                return@launch
            }

            val waste = WasteLog(
                wasteDate = wasteDate,
                ingredientUuid = ingredientUuid,
                quantity = quantity,
                reason = reason,
                calculatedCost = 0.0, // calculated dynamically in repo
                userUuid = "user-admin",
                notes = notes,
                storeId = 1L
            )

            val success = wasteRepository.recordWaste(waste, "user-admin")
            if (success) {
                _uiStateMessage.value = "Waste berhasil dicatat!"
                loadDashboardMetrics()
                onSuccess()
            } else {
                _uiStateMessage.value = "Gagal mencatat waste"
            }
        }
    }

    class Factory(
        private val inventoryRepository: InventoryRepository,
        private val purchaseExpenseRepository: PurchaseExpenseRepository,
        private val expenseRepository: ExpenseRepository,
        private val stockOpnameRepository: StockOpnameRepository,
        private val wasteRepository: WasteRepository,
        private val inventoryLiteRepository: InventoryLiteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryLiteViewModel::class.java)) {
                return InventoryLiteViewModel(
                    inventoryRepository,
                    purchaseExpenseRepository,
                    expenseRepository,
                    stockOpnameRepository,
                    wasteRepository,
                    inventoryLiteRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
