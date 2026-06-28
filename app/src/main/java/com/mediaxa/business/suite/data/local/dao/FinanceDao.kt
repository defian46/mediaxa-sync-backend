package com.mediaxa.business.suite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mediaxa.business.suite.data.local.entity.CashShift
import com.mediaxa.business.suite.data.local.entity.DailyClosing

@Dao
interface FinanceDao {

    @Query("""
        SELECT 
            COALESCE(SUM(total), 0.0) AS totalRevenue,
            COALESCE(SUM(transactionHpp), 0.0) AS totalHpp
        FROM transactions
        WHERE storeId = :storeId 
          AND status = 'SUCCESS' 
          AND createdAt >= :startDate 
          AND createdAt <= :endDate
    """)
    suspend fun getSalesSummary(storeId: Long, startDate: Long, endDate: Long): FinanceSalesSummaryResult

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN category = 'INVENTORY_PURCHASE' THEN amount ELSE 0.0 END), 0.0) AS inventoryPurchase,
            COALESCE(SUM(CASE WHEN category != 'INVENTORY_PURCHASE' THEN amount ELSE 0.0 END), 0.0) AS operationalExpense
        FROM expenses
        WHERE storeId = :storeId 
          AND isDeleted = 0 
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate
    """)
    suspend fun getExpenseSummary(storeId: Long, startDate: Long, endDate: Long): ExpenseSummaryResult

    @Query("""
        SELECT COALESCE(SUM(calculatedCost), 0.0)
        FROM waste_logs
        WHERE storeId = :storeId 
          AND isDeleted = 0 
          AND wasteDate >= :startDate 
          AND wasteDate <= :endDate
    """)
    suspend fun getWasteCostSummary(storeId: Long, startDate: Long, endDate: Long): Double

    @Query("""
        SELECT 
            paymentMethod AS method,
            COALESCE(SUM(total), 0.0) AS amount
        FROM transactions
        WHERE storeId = :storeId 
          AND status = 'SUCCESS' 
          AND createdAt >= :startDate 
          AND createdAt <= :endDate
        GROUP BY paymentMethod
    """)
    suspend fun getSalesInflowByMethod(storeId: Long, startDate: Long, endDate: Long): List<PaymentMethodSummary>

    @Query("""
        SELECT 
            paymentMethod AS method,
            COALESCE(SUM(amount), 0.0) AS amount
        FROM expenses
        WHERE storeId = :storeId 
          AND isDeleted = 0 
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate
        GROUP BY paymentMethod
    """)
    suspend fun getExpenseOutflowByMethod(storeId: Long, startDate: Long, endDate: Long): List<PaymentMethodSummary>

    @Query("""
        SELECT 
            category,
            COALESCE(SUM(amount), 0.0) AS totalAmount
        FROM expenses
        WHERE storeId = :storeId 
          AND isDeleted = 0 
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate
        GROUP BY category
        ORDER BY totalAmount DESC
    """)
    suspend fun getExpensesByCategory(storeId: Long, startDate: Long, endDate: Long): List<CategoryExpenseResult>

    @Query("""
        SELECT 
            strftime('%Y-%m-%d', datetime(createdAt / 1000, 'unixepoch', 'localtime')) AS dateStr,
            COALESCE(SUM(total), 0.0) AS dailyRevenue,
            COALESCE(SUM(transactionHpp), 0.0) AS dailyHpp
        FROM transactions
        WHERE storeId = :storeId 
          AND status = 'SUCCESS' 
          AND createdAt >= :startDate 
          AND createdAt <= :endDate
        GROUP BY dateStr
        ORDER BY dateStr ASC
    """)
    suspend fun getDailySalesTrend(storeId: Long, startDate: Long, endDate: Long): List<DailySalesTrendResult>

    // --- DAILY CLOSING ---
    @Insert
    suspend fun insertDailyClosing(dailyClosing: DailyClosing): Long

    @Query("SELECT * FROM daily_closings WHERE storeId = :storeId AND isDeleted = 0 ORDER BY dateStr DESC")
    suspend fun getDailyClosings(storeId: Long): List<DailyClosing>

    @Query("SELECT * FROM daily_closings WHERE storeId = :storeId AND dateStr = :dateStr AND isDeleted = 0 LIMIT 1")
    suspend fun getDailyClosingByDate(storeId: Long, dateStr: String): DailyClosing?

    @Query("SELECT closingBalance FROM daily_closings WHERE storeId = :storeId AND isDeleted = 0 ORDER BY dateStr DESC LIMIT 1")
    suspend fun getLatestClosingBalance(storeId: Long): Double?

    // --- CASH SHIFT ---
    @Insert
    suspend fun insertCashShift(cashShift: CashShift): Long

    @Update
    suspend fun updateCashShift(cashShift: CashShift): Int

    @Query("SELECT * FROM cash_shifts WHERE storeId = :storeId AND status = 'ACTIVE' AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveCashShift(storeId: Long): CashShift?

    @Query("SELECT * FROM cash_shifts WHERE storeId = :storeId AND isDeleted = 0 ORDER BY startTime DESC")
    suspend fun getCashShifts(storeId: Long): List<CashShift>

    @Query("""
        SELECT COALESCE(SUM(total), 0.0) 
        FROM transactions 
        WHERE storeId = :storeId 
          AND status = 'SUCCESS' 
          AND UPPER(paymentMethod) = 'CASH' 
          AND createdAt >= :startTime 
          AND createdAt <= :endTime
    """)
    suspend fun getCashSalesDuringShift(storeId: Long, startTime: Long, endTime: Long): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) 
        FROM expenses 
        WHERE storeId = :storeId 
          AND isDeleted = 0 
          AND UPPER(paymentMethod) = 'CASH' 
          AND expenseDate >= :startTime 
          AND expenseDate <= :endTime
    """)
    suspend fun getCashExpensesDuringShift(storeId: Long, startTime: Long, endTime: Long): Double

    @Query("""
        SELECT COUNT(*)
        FROM transactions
        WHERE storeId = :storeId
          AND status = 'SUCCESS'
          AND createdAt >= :startDate
          AND createdAt <= :endDate
    """)
    suspend fun getTransactionCountInRange(storeId: Long, startDate: Long, endDate: Long): Int
}

data class FinanceSalesSummaryResult(
    val totalRevenue: Double,
    val totalHpp: Double
)

data class ExpenseSummaryResult(
    val inventoryPurchase: Double,
    val operationalExpense: Double
)

data class PaymentMethodSummary(
    val method: String,
    val amount: Double
)

data class CategoryExpenseResult(
    val category: String,
    val totalAmount: Double
)

data class DailySalesTrendResult(
    val dateStr: String,
    val dailyRevenue: Double,
    val dailyHpp: Double
)
