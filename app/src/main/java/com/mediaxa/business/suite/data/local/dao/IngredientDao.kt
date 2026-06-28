package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.Ingredient
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllIngredientsFlow(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getActiveIngredientsFlow(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE uuid = :uuid AND isDeleted = 0 LIMIT 1")
    suspend fun getIngredientByUuid(uuid: String): Ingredient?

    @Query("SELECT * FROM ingredients WHERE uuid IN (:uuids) AND isDeleted = 0")
    suspend fun getIngredientsByUuids(uuids: List<String>): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedIngredients(): List<Ingredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient): Long

    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Query("UPDATE ingredients SET availableStock = availableStock + :amount, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE uuid = :uuid")
    suspend fun adjustStock(uuid: String, amount: Double, updatedAt: Long, syncStatus: String)

    @Query("""
        SELECT 
            i.uuid AS ingredientUuid,
            i.name AS ingredientName,
            i.availableStock AS availableStock,
            i.unit AS unit,
            COALESCE(ABS(SUM(sm.quantity)) / 30.0, 0.0) AS dailyVelocity
        FROM ingredients i
        LEFT JOIN stock_movements sm ON i.uuid = sm.ingredientUuid 
            AND sm.type = 'SALES_USAGE' 
            AND sm.createdAt >= :thirtyDaysAgo
        WHERE i.storeId = :storeId AND i.isDeleted = 0
        GROUP BY i.uuid
    """)
    suspend fun getIngredientVelocities(storeId: Long, thirtyDaysAgo: Long): List<IngredientVelocityResult>
}

data class IngredientVelocityResult(
    val ingredientUuid: String,
    val ingredientName: String,
    val availableStock: Double,
    val unit: String,
    val dailyVelocity: Double
)
