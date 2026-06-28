package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "menus",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class Menu(
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
    val categoryUuid: String, // Refer to Category's UUID for cloud merge stability
    val price: Double,
    val promoPrice: Double? = null,
    val imagePath: String? = null,
    val isActive: Boolean = true,
    val description: String? = null,
    val estimatedHpp: Double = 0.0,
    val estimatedMargin: Double = 0.0
)
