package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "ingredients",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class Ingredient(
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

    val name: String,
    val unit: String,
    val purchasePrice: Double,
    val packageSize: Double,
    val unitPrice: Double,
    val availableStock: Double = 0.0,
    val minStock: Double = 0.0,
    val isActive: Boolean = true,
    val supplier: String? = null
)
