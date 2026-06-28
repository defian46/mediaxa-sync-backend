package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "daily_closings",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "dateStr"], unique = true)
    ]
)
data class DailyClosing(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1L,
    val deviceId: String = "DEV-01",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0,
    val syncStatus: String = "PENDING_INSERT",
    val dateStr: String, // format YYYY-MM-DD
    val openingBalance: Double,
    val revenue: Double,
    val hpp: Double,
    val grossProfit: Double,
    val operationalExpense: Double,
    val wasteCost: Double,
    val netProfit: Double,
    val cashInflow: Double,
    val cashOutflow: Double,
    val closingBalance: Double, // openingBalance + cashInflow - cashOutflow
    val cashRevenue: Double,
    val qrisRevenue: Double,
    val transferRevenue: Double,
    val totalTransactions: Int,
    val averageTicket: Double,
    val closedByUserUuid: String
)
