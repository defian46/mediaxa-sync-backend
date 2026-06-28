package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_opnames",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "opnameDate"])
    ]
)
data class StockOpname(
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

    val opnameDate: Long,
    val userUuid: String,
    val notes: String?
)

@Entity(
    tableName = "stock_opname_items",
    foreignKeys = [
        ForeignKey(
            entity = StockOpname::class,
            parentColumns = ["uuid"],
            childColumns = ["opnameUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["opnameUuid"]),
        Index(value = ["ingredientUuid"])
    ]
)
data class StockOpnameItem(
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

    val opnameUuid: String,
    val ingredientUuid: String,
    val systemStock: Double,
    val physicalStock: Double,
    val diffStock: Double,
    val notes: String // Wajib diisi jika diffStock != 0
)
