package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "loyalty_point_history",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "customerUuid"]),
        Index(value = ["transactionUuid"])
    ]
)
data class LoyaltyPointHistory(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1L,
    val deviceId: String = "DEV-01",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0,
    val syncStatus: String = "PENDING_INSERT",

    val customerUuid: String,
    val transactionUuid: String?,
    val points: Int, // Positive for earned, negative for used/redeemed
    val activityType: String, // "EARNED", "REDEEMED", "EXPIRED"
    val expiryTime: Long? = null,
    val notes: String? = null
)
