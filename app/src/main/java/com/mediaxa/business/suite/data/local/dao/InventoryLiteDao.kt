package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryLiteDao {

    // --- WRITES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseExpense(expense: PurchaseExpense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseExpenseItems(items: List<PurchaseExpenseItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockOpname(opname: StockOpname): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockOpnameItems(items: List<StockOpnameItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWasteLog(log: WasteLog): Long

    // --- READS / QUERIES ---
    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND isDeleted = 0 ORDER BY expenseDate DESC")
    fun getAllExpensesFlow(storeId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND isDeleted = 0 AND expenseDate >= :startDate AND expenseDate <= :endDate ORDER BY expenseDate DESC")
    suspend fun getExpensesInPeriod(storeId: Long, startDate: Long, endDate: Long): List<Expense>

    @Query("SELECT * FROM purchase_expenses WHERE storeId = :storeId AND isDeleted = 0 ORDER BY purchaseDate DESC")
    fun getAllPurchaseExpensesFlow(storeId: Long): Flow<List<PurchaseExpense>>

    @Query("SELECT * FROM stock_opnames WHERE storeId = :storeId AND isDeleted = 0 ORDER BY opnameDate DESC")
    fun getAllStockOpnamesFlow(storeId: Long): Flow<List<StockOpname>>

    @Query("SELECT * FROM waste_logs WHERE storeId = :storeId AND isDeleted = 0 ORDER BY wasteDate DESC")
    fun getAllWasteLogsFlow(storeId: Long): Flow<List<WasteLog>>

    @Query("SELECT * FROM waste_logs WHERE storeId = :storeId AND isDeleted = 0 AND wasteDate >= :startDate AND wasteDate <= :endDate ORDER BY wasteDate DESC")
    suspend fun getWasteLogsInPeriod(storeId: Long, startDate: Long, endDate: Long): List<WasteLog>

    // 1. Total stock value calculation
    @Query("SELECT SUM(availableStock * unitPrice) FROM ingredients WHERE storeId = :storeId AND isDeleted = 0")
    suspend fun getTotalStockAssetValue(storeId: Long): Double?

    // 2. Ingredients low stock counts
    @Query("SELECT COUNT(uuid) FROM ingredients WHERE storeId = :storeId AND isDeleted = 0 AND availableStock <= minStock AND availableStock > 0")
    suspend fun getLowStockCount(storeId: Long): Int

    // 3. Ingredients out of stock counts
    @Query("SELECT COUNT(uuid) FROM ingredients WHERE storeId = :storeId AND isDeleted = 0 AND availableStock <= 0")
    suspend fun getOutOfStockCount(storeId: Long): Int

    // 4. Top 5 ingredients most consumed (by SALES_USAGE)
    @Query("""
        SELECT 
            ingredientUuid AS ingredientUuid,
            i.name AS ingredientName,
            ABS(SUM(sm.quantity)) AS totalQty,
            i.unit AS unit
        FROM stock_movements sm
        INNER JOIN ingredients i ON sm.ingredientUuid = i.uuid
        WHERE sm.storeId = :storeId
          AND sm.type = 'SALES_USAGE'
          AND sm.createdAt >= :startDate
          AND sm.createdAt <= :endDate
        GROUP BY ingredientUuid
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    suspend fun getTopConsumedIngredients(storeId: Long, startDate: Long, endDate: Long, limit: Int): List<IngredientUsageResult>

    // 5. Top 5 ingredients most frequently purchased
    @Query("""
        SELECT 
            pei.ingredientUuid AS ingredientUuid,
            i.name AS ingredientName,
            SUM(pei.quantity) AS totalQty,
            i.unit AS unit
        FROM purchase_expense_items pei
        INNER JOIN ingredients i ON pei.ingredientUuid = i.uuid
        WHERE pei.storeId = :storeId
          AND pei.isDeleted = 0
          AND pei.createdAt >= :startDate
          AND pei.createdAt <= :endDate
        GROUP BY pei.ingredientUuid
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    suspend fun getTopPurchasedIngredients(storeId: Long, startDate: Long, endDate: Long, limit: Int): List<IngredientUsageResult>
}

data class IngredientUsageResult(
    val ingredientUuid: String,
    val ingredientName: String,
    val totalQty: Double,
    val unit: String
)
