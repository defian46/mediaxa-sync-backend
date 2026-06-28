package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["uuid"],
            childColumns = ["transactionUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["transactionUuid"]),
        Index(value = ["menuUuid"]),
        Index(value = ["storeId", "createdAt"])
    ]
)
data class TransactionItem(
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

    val transactionUuid: String,
    val menuUuid: String,
    val menuName: String,
    val quantity: Int,
    val price: Double,
    val discount: Double = 0.0,
    val subtotal: Double,
    
    // HPP Snapshot fields (Phase 3.5)
    val sellingPrice: Double = price,
    val costPrice: Double = 0.0,
    val grossProfit: Double = 0.0,
    val marginPercent: Double = 0.0
)
