package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE uuid = :uuid LIMIT 1")
    suspend fun getCustomerByUuid(uuid: String): Customer?

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND customerCode = :code AND isDeleted = 0 LIMIT 1")
    suspend fun getCustomerByCode(storeId: Long, code: String): Customer?

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND phone = :phone AND isDeleted = 0 LIMIT 1")
    suspend fun getCustomerByPhone(storeId: Long, phone: String): Customer?

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND isDeleted = 0 ORDER BY customerName ASC")
    fun getAllCustomersFlow(storeId: Long): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND isDeleted = 0 ORDER BY customerName ASC")
    suspend fun getAllCustomers(storeId: Long): List<Customer>

    @Query("""
        SELECT * FROM customers 
        WHERE storeId = :storeId 
          AND isDeleted = 0 
          AND (customerName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR customerCode LIKE '%' || :query || '%')
        ORDER BY customerName ASC
    """)
    fun searchCustomersFlow(storeId: Long, query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND isDeleted = 0 ORDER BY totalSpending DESC")
    fun getTopCustomersFlow(storeId: Long): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND isDeleted = 0 ORDER BY totalSpending DESC LIMIT :limit")
    suspend fun getTopCustomers(storeId: Long, limit: Int): List<Customer>

    // Metrics for analytics
    @Query("SELECT COUNT(uuid) FROM customers WHERE storeId = :storeId AND isDeleted = 0 AND joinDate >= :startDate")
    suspend fun getNewCustomersCount(storeId: Long, startDate: Long): Int

    @Query("""
        SELECT COUNT(DISTINCT customerUuid) 
        FROM transactions 
        WHERE storeId = :storeId 
          AND isDeleted = 0 
          AND status = 'PAID'
          AND customerUuid IS NOT NULL
          AND createdAt >= :startDate
    """)
    suspend fun getActiveCustomersCount(storeId: Long, startDate: Long): Int

    @Query("""
        SELECT menuUuid 
        FROM transaction_items 
        INNER JOIN transactions ON transaction_items.transactionUuid = transactions.uuid
        WHERE transactions.customerUuid = :customerUuid 
          AND transactions.storeId = :storeId 
          AND transactions.isDeleted = 0 
          AND transaction_items.isDeleted = 0
        GROUP BY menuUuid
        ORDER BY SUM(quantity) DESC
        LIMIT 1
    """)
    suspend fun getFavoriteMenuUuid(storeId: Long, customerUuid: String): String?
}
