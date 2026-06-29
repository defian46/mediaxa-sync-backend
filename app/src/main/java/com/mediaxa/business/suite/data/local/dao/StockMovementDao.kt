package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllStockMovementsFlow(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE ingredientUuid = :ingredientUuid AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getStockMovementsByIngredientFlow(ingredientUuid: String): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedStockMovements(): List<StockMovement>

    @Query("SELECT * FROM stock_movements WHERE uuid = :uuid LIMIT 1")
    suspend fun getStockMovementByUuid(uuid: String): StockMovement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    @Query("SELECT COUNT(*) FROM stock_movements WHERE isDeleted = 0")
    suspend fun getMovementCount(): Int
}
