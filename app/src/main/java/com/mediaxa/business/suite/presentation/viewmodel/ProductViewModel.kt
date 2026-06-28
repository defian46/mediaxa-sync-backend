package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.entity.Category
import com.mediaxa.business.suite.data.local.entity.Menu
import com.mediaxa.business.suite.data.local.entity.MenuRecipe
import com.mediaxa.business.suite.data.local.entity.SyncStatus
import com.mediaxa.business.suite.data.repository.ProductRepository
import com.mediaxa.business.suite.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = productRepository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCategories: StateFlow<List<Category>> = productRepository.getActiveCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val menus: StateFlow<List<Menu>> = productRepository.getAllMenusFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMenus: StateFlow<List<Menu>> = productRepository.getActiveMenusFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category actions
    fun addCategory(name: String, displayOrder: Int) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                displayOrder = displayOrder,
                syncStatus = SyncStatus.PENDING_CREATE.name
            )
            productRepository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            val updated = category.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            productRepository.updateCategory(updated)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            productRepository.softDeleteCategory(category)
        }
    }

    // Menu actions
    fun addMenu(
        name: String,
        categoryUuid: String,
        price: Double,
        promoPrice: Double?,
        imagePath: String?,
        description: String?
    ) {
        viewModelScope.launch {
            val menu = Menu(
                name = name,
                categoryUuid = categoryUuid,
                price = price,
                promoPrice = promoPrice,
                imagePath = imagePath,
                description = description,
                syncStatus = SyncStatus.PENDING_CREATE.name
            )
            productRepository.insertMenu(menu)
        }
    }

    fun updateMenu(menu: Menu) {
        viewModelScope.launch {
            val updated = menu.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            productRepository.updateMenu(updated)
        }
    }

    fun deleteMenu(menu: Menu) {
        viewModelScope.launch {
            productRepository.softDeleteMenu(menu)
        }
    }

    suspend fun getRecipeForMenu(menuUuid: String): List<MenuRecipe> {
        return productRepository.getRecipeForMenu(menuUuid)
    }

    // Recipe BOM calculation & save
    fun saveRecipe(menuUuid: String, recipes: List<MenuRecipe>) {
        viewModelScope.launch {
            // Save recipe list
            productRepository.saveRecipe(menuUuid, recipes)
            
            // Recalculate Menu HPP and margins
            recalculateMenuCosts(menuUuid)
        }
    }

    suspend fun recalculateMenuCosts(menuUuid: String) {
        val menu = productRepository.getMenuByUuid(menuUuid) ?: return
        val recipes = productRepository.getRecipeForMenu(menuUuid)
        
        var calculatedHpp = 0.0
        val ingredients = inventoryRepository.getAllIngredientsFlow().first()
        
        for (recipeItem in recipes) {
            val ing = ingredients.find { it.uuid == recipeItem.ingredientUuid }
            if (ing != null) {
                calculatedHpp += ing.unitPrice * recipeItem.quantityNeeded
            }
        }

        val margin = if (menu.price > 0) {
            ((menu.price - calculatedHpp) / menu.price) * 100
        } else {
            0.0
        }

        val updatedMenu = menu.copy(
            estimatedHpp = calculatedHpp,
            estimatedMargin = margin,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPDATE.name
        )
        productRepository.updateMenu(updatedMenu)
    }

    class Factory(
        private val productRepository: ProductRepository,
        private val inventoryRepository: InventoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
                return ProductViewModel(productRepository, inventoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
