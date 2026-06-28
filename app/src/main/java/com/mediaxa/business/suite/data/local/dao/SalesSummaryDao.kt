package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesSummaryDao {

    // 1. Get total revenue, total profit, and average ticket size for a date range
    @Query("""
        SELECT 
            COALESCE(SUM(total), 0.0) AS totalRevenue,
            COALESCE(SUM(grossProfit), 0.0) AS totalProfit,
            COUNT(uuid) AS transactionCount,
            COALESCE(AVG(total), 0.0) AS averageTicketValue
        FROM transactions 
        WHERE storeId = :storeId 
          AND status = 'PAID' 
          AND isDeleted = 0
          AND createdAt >= :startDate 
          AND createdAt <= :endDate
    """)
    suspend fun getSalesSummary(storeId: Long, startDate: Long, endDate: Long): SalesSummaryResult?

    // 2. Get total items sold for a date range
    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM transaction_items
        WHERE storeId = :storeId
          AND isDeleted = 0
          AND createdAt >= :startDate
          AND createdAt <= :endDate
    """)
    suspend fun getItemsSoldCount(storeId: Long, startDate: Long, endDate: Long): Int

    // 3. Get top selling products (Top 10)
    @Query("""
        SELECT 
            menuUuid,
            menuName,
            SUM(quantity) AS totalQty,
            SUM(subtotal) AS totalValue
        FROM transaction_items
        WHERE storeId = :storeId
          AND isDeleted = 0
          AND createdAt >= :startDate
          AND createdAt <= :endDate
        GROUP BY menuUuid
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    suspend fun getTopSellingProducts(storeId: Long, startDate: Long, endDate: Long, limit: Int): List<ProductSalesResult>

    // 4. Get bottom selling products (Bottom 10)
    @Query("""
        SELECT 
            menuUuid,
            menuName,
            SUM(quantity) AS totalQty,
            SUM(subtotal) AS totalValue
        FROM transaction_items
        WHERE storeId = :storeId
          AND isDeleted = 0
          AND createdAt >= :startDate
          AND createdAt <= :endDate
        GROUP BY menuUuid
        ORDER BY totalQty ASC
        LIMIT :limit
    """)
    suspend fun getBottomSellingProducts(storeId: Long, startDate: Long, endDate: Long, limit: Int): List<ProductSalesResult>

    // 5. Get sales per cashier (Kasir score)
    @Query("""
        SELECT 
            cashierUuid,
            cashierName,
            COUNT(uuid) AS transactionCount,
            SUM(total) AS totalSales,
            AVG(total) AS averageTicketValue
        FROM transactions
        WHERE storeId = :storeId
          AND status = 'PAID'
          AND isDeleted = 0
          AND createdAt >= :startDate
          AND createdAt <= :endDate
        GROUP BY cashierUuid
    """)
    suspend fun getCashierPerformance(storeId: Long, startDate: Long, endDate: Long): List<CashierPerformanceResult>

    // 6. Get breakdown of payment methods
    @Query("""
        SELECT 
            method AS paymentMethod,
            SUM(amount) AS totalAmount,
            COUNT(uuid) AS paymentCount
        FROM payments
        WHERE storeId = :storeId
          AND isDeleted = 0
          AND createdAt >= :startDate
          AND createdAt <= :endDate
        GROUP BY method
    """)
    suspend fun getPaymentMethodDistribution(storeId: Long, startDate: Long, endDate: Long): List<PaymentDistributionResult>

    // 7. Get hourly sales distribution (Peak Hours)
    @Query("""
        SELECT 
            CAST(strftime('%H', datetime(createdAt / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hourOfDay,
            COUNT(uuid) AS transactionCount,
            SUM(total) AS totalAmount
        FROM transactions
        WHERE storeId = :storeId 
          AND status = 'PAID'
          AND isDeleted = 0
          AND createdAt >= :startDate 
          AND createdAt <= :endDate
        GROUP BY hourOfDay
        ORDER BY hourOfDay ASC
    """)
    suspend fun getHourlySalesDistribution(storeId: Long, startDate: Long, endDate: Long): List<HourlySalesResult>

    // 8. Get sales grouped by menu category
    @Query("""
        SELECT 
            m.categoryUuid,
            c.name AS categoryName,
            SUM(ti.quantity) AS totalQty,
            SUM(ti.subtotal) AS totalValue
        FROM transaction_items ti
        INNER JOIN menus m ON ti.menuUuid = m.uuid
        INNER JOIN categories c ON m.categoryUuid = c.uuid
        WHERE ti.storeId = :storeId
          AND ti.isDeleted = 0
          AND ti.createdAt >= :startDate
          AND ti.createdAt <= :endDate
        GROUP BY m.categoryUuid
    """)
    suspend fun getCategorySalesDistribution(storeId: Long, startDate: Long, endDate: Long): List<CategorySalesResult>
}

data class SalesSummaryResult(
    val totalRevenue: Double,
    val totalProfit: Double,
    val transactionCount: Int,
    val averageTicketValue: Double
)

data class ProductSalesResult(
    val menuUuid: String,
    val menuName: String,
    val totalQty: Int,
    val totalValue: Double
)

data class CashierPerformanceResult(
    val cashierUuid: String,
    val cashierName: String,
    val transactionCount: Int,
    val totalSales: Double,
    val averageTicketValue: Double
)

data class PaymentDistributionResult(
    val paymentMethod: String,
    val totalAmount: Double,
    val paymentCount: Int
)

data class HourlySalesResult(
    val hourOfDay: Int,
    val transactionCount: Int,
    val totalAmount: Double
)

data class CategorySalesResult(
    val categoryUuid: String,
    val categoryName: String,
    val totalQty: Int,
    val totalValue: Double
)
