package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "waste_logs",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "wasteDate"]),
        Index(value = ["ingredientUuid"])
    ]
)
data class WasteLog(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1,
    val deviceId: String = "DEV-01",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isDeleted: Boolean = false,
    val syncStatus: String = "PENDING_CREATE",
    val lastSyncedAt: Long? = null,

    val wasteDate: Long,
    val ingredientUuid: String,
    val quantity: Double,
    val reason: String, // Rusak, Kadaluarsa, Tumpah, Sampling, Lain-lain
    val calculatedCost: Double,
    val userUuid: String,
    val notes: String?
)
