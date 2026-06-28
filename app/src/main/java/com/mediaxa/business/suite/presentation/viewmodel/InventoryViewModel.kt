package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.entity.Ingredient
import com.mediaxa.business.suite.data.local.entity.StockMovement
import com.mediaxa.business.suite.data.local.entity.SyncStatus
import com.mediaxa.business.suite.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    val ingredients: StateFlow<List<Ingredient>> = inventoryRepository.getAllIngredientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeIngredients: StateFlow<List<Ingredient>> = inventoryRepository.getActiveIngredientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addIngredient(
        name: String,
        unit: String,
        purchasePrice: Double,
        packageSize: Double,
        availableStock: Double,
        minStock: Double,
        supplier: String?
    ) {
        viewModelScope.launch {
            val calculatedUnitPrice = if (packageSize > 0) purchasePrice / packageSize else 0.0
            val ingredient = Ingredient(
                name = name,
                unit = unit,
                purchasePrice = purchasePrice,
                packageSize = packageSize,
                unitPrice = calculatedUnitPrice,
                availableStock = availableStock,
                minStock = minStock,
                supplier = supplier,
                syncStatus = SyncStatus.PENDING_CREATE.name
            )
            inventoryRepository.insertIngredient(ingredient)
        }
    }

    fun updateIngredient(
        ingredient: Ingredient,
        name: String,
        unit: String,
        purchasePrice: Double,
        packageSize: Double,
        minStock: Double,
        supplier: String?
    ) {
        viewModelScope.launch {
            val calculatedUnitPrice = if (packageSize > 0) purchasePrice / packageSize else 0.0
            val updated = ingredient.copy(
                name = name,
                unit = unit,
                purchasePrice = purchasePrice,
                packageSize = packageSize,
                unitPrice = calculatedUnitPrice,
                minStock = minStock,
                supplier = supplier,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            inventoryRepository.updateIngredient(updated)
        }
    }

    fun deleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            inventoryRepository.softDeleteIngredient(ingredient)
        }
    }

    fun adjustStock(
        ingredient: Ingredient,
        quantity: Double,
        type: String, // "ADJUSTMENT_PLUS", "ADJUSTMENT_MINUS", "STOCK_IN", "STOCK_OUT", "WASTE"
        note: String?,
        userUuid: String
    ) {
        viewModelScope.launch {
            val movement = StockMovement(
                ingredientUuid = ingredient.uuid,
                quantity = quantity,
                type = type,
                note = note,
                userUuid = userUuid,
                syncStatus = SyncStatus.PENDING_CREATE.name
            )
            inventoryRepository.addStockMovement(movement)
        }
    }

    class Factory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
                return InventoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
