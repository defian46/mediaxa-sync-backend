package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["storeId", "status", "createdAt"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1,
    val deviceId: String = "DEV-01",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val lastSyncedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING_CREATE.name,

    val transactionNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierUuid: String,
    val cashierName: String,
    val discount: Double = 0.0,
    val subtotal: Double,
    val total: Double,
    val transactionHpp: Double = 0.0,
    val grossProfit: Double = 0.0,
    val paymentMethod: String,
    val amountReceived: Double,
    val changeAmount: Double,
    val status: String = "PAID",
    val customerUuid: String? = null,
    val pointsEarned: Int? = null,
    val pointsRedeemed: Int? = null,
    val notes: String? = null
)
