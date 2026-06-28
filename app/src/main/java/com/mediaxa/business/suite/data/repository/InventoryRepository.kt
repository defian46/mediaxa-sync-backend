package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.Ingredient
import com.mediaxa.business.suite.data.local.entity.StockMovement
import com.mediaxa.business.suite.data.local.entity.SyncStatus
import com.mediaxa.business.suite.data.local.entity.AuditLog
import com.mediaxa.business.suite.data.local.dao.IngredientVelocityResult
import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val localDataSource: LocalDataSource) {
    fun getAllIngredientsFlow(): Flow<List<Ingredient>> = localDataSource.ingredientDao.getAllIngredientsFlow()
    fun getActiveIngredientsFlow(): Flow<List<Ingredient>> = localDataSource.ingredientDao.getActiveIngredientsFlow()
    suspend fun getIngredientByUuid(uuid: String): Ingredient? = localDataSource.ingredientDao.getIngredientByUuid(uuid)
    suspend fun insertIngredient(ingredient: Ingredient): Long = localDataSource.ingredientDao.insertIngredient(ingredient)
    suspend fun updateIngredient(ingredient: Ingredient) = localDataSource.ingredientDao.updateIngredient(ingredient)
    suspend fun softDeleteIngredient(ingredient: Ingredient) {
        val updated = ingredient.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_DELETE.name,
            updatedAt = System.currentTimeMillis()
        )
        localDataSource.ingredientDao.updateIngredient(updated)
    }

    fun getAllStockMovementsFlow(): Flow<List<StockMovement>> = localDataSource.stockMovementDao.getAllStockMovementsFlow()
    fun getStockMovementsByIngredientFlow(ingredientUuid: String): Flow<List<StockMovement>> = localDataSource.stockMovementDao.getStockMovementsByIngredientFlow(ingredientUuid)

    suspend fun addStockMovement(movement: StockMovement) {
        val ingredient = localDataSource.ingredientDao.getIngredientByUuid(movement.ingredientUuid)
        
        localDataSource.stockMovementDao.insertMovement(movement)
        localDataSource.ingredientDao.adjustStock(
            movement.ingredientUuid, 
            movement.quantity, 
            System.currentTimeMillis(), 
            SyncStatus.PENDING_UPDATE.name
        )

        // Record manual adjustment to AuditLog
        val user = localDataSource.userDao.getUserByUuid(movement.userUuid)
        val username = user?.username ?: "System"
        val ingredientName = ingredient?.name ?: movement.ingredientUuid
        
        val auditLog = AuditLog(
            userUuid = movement.userUuid,
            username = username,
            action = "STOCK_ADJUSTMENT",
            entity = "Ingredient",
            entityId = movement.ingredientUuid,
            oldValue = ingredient?.availableStock?.toString() ?: "0.0",
            newValue = "Ingredient: $ingredientName, Adjusted: ${movement.quantity}, Type: ${movement.type}, Note: ${movement.note}"
        )
        localDataSource.auditLogDao.insertLog(auditLog)
    }

    suspend fun getIngredientVelocities(storeId: Long, thirtyDaysAgo: Long): List<IngredientVelocityResult> {
        return localDataSource.ingredientDao.getIngredientVelocities(storeId, thirtyDaysAgo)
    }
}
