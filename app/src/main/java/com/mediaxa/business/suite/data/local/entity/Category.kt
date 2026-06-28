package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
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
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)
