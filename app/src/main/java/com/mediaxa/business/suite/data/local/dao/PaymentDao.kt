package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE transactionUuid = :transactionUuid AND isDeleted = 0")
    fun getPaymentsForTransactionFlow(transactionUuid: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedPayments(): List<Payment>

    @Query("SELECT * FROM payments WHERE uuid = :uuid LIMIT 1")
    suspend fun getPaymentByUuid(uuid: String): Payment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)
}
