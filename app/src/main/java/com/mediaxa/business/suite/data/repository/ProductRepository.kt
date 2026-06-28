package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.Category
import com.mediaxa.business.suite.data.local.entity.Menu
import com.mediaxa.business.suite.data.local.entity.MenuRecipe
import com.mediaxa.business.suite.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val localDataSource: LocalDataSource) {
    // Categories
    fun getAllCategoriesFlow(): Flow<List<Category>> = localDataSource.categoryDao.getAllCategoriesFlow()
    fun getActiveCategoriesFlow(): Flow<List<Category>> = localDataSource.categoryDao.getActiveCategoriesFlow()
    suspend fun insertCategory(category: Category): Long = localDataSource.categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = localDataSource.categoryDao.updateCategory(category)
    suspend fun softDeleteCategory(category: Category) {
        val updated = category.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_DELETE.name,
            updatedAt = System.currentTimeMillis()
        )
        localDataSource.categoryDao.updateCategory(updated)
    }

    // Menus
    fun getAllMenusFlow(): Flow<List<Menu>> = localDataSource.menuDao.getAllMenusFlow()
    fun getActiveMenusFlow(): Flow<List<Menu>> = localDataSource.menuDao.getActiveMenusFlow()
    fun getMenusByCategoryFlow(categoryUuid: String): Flow<List<Menu>> = localDataSource.menuDao.getMenusByCategoryFlow(categoryUuid)
    suspend fun getMenuByUuid(uuid: String): Menu? = localDataSource.menuDao.getMenuByUuid(uuid)
    suspend fun insertMenu(menu: Menu): Long = localDataSource.menuDao.insertMenu(menu)
    suspend fun updateMenu(menu: Menu) = localDataSource.menuDao.updateMenu(menu)
    suspend fun softDeleteMenu(menu: Menu) {
        val updated = menu.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_DELETE.name,
            updatedAt = System.currentTimeMillis()
        )
        localDataSource.menuDao.updateMenu(updated)
    }

    // Recipe / BOM
    fun getRecipeForMenuFlow(menuUuid: String): Flow<List<MenuRecipe>> = localDataSource.menuRecipeDao.getRecipeForMenuFlow(menuUuid)
    suspend fun getRecipeForMenu(menuUuid: String): List<MenuRecipe> = localDataSource.menuRecipeDao.getRecipeForMenu(menuUuid)
    suspend fun saveRecipe(menuUuid: String, recipes: List<MenuRecipe>) {
        localDataSource.menuRecipeDao.softDeleteRecipeForMenu(menuUuid, System.currentTimeMillis(), SyncStatus.PENDING_DELETE.name)
        localDataSource.menuRecipeDao.insertRecipeItems(recipes)
    }
}
