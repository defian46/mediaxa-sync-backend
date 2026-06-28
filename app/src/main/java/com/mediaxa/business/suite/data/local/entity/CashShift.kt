package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "cash_shifts",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "status"]),
        Index(value = ["storeId", "startTime"])
    ]
)
data class CashShift(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1L,
    val deviceId: String = "DEV-01",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0,
    val syncStatus: String = "PENDING_INSERT",

    val cashierUuid: String,
    val startTime: Long,
    val endTime: Long? = null,
    val openingCash: Double,
    val closingCash: Double? = null,
    val expectedCash: Double? = null,
    val actualCash: Double? = null,
    val cashDifference: Double? = null,
    val status: String = "ACTIVE" // "ACTIVE", "CLOSED"
)
