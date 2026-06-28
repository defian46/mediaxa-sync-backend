package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "customerCode"], unique = true),
        Index(value = ["storeId", "phone"])
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1L,
    val deviceId: String = "DEV-01",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0,
    val syncStatus: String = "PENDING_INSERT",

    val customerCode: String,
    val customerName: String,
    val phone: String?,
    val email: String?,
    val birthday: Long?,
    val gender: String?,
    val address: String?,
    val notes: String?,
    val joinDate: Long = System.currentTimeMillis(),
    val membershipLevel: String = "BRONZE", // BRONZE, SILVER, GOLD, PLATINUM
    val totalSpending: Double = 0.0,
    val lastVisit: Long? = null,
    val favoriteMenuUuid: String? = null
)
