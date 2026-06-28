package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["uuid"],
            childColumns = ["transactionUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionUuid"])
    ]
)
data class Payment(
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
    val method: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
