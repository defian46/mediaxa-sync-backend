package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "store_settings")
data class StoreSetting(
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

    val storeName: String,
    val logoPath: String? = null,
    val address: String,
    val phoneNumber: String,
    val receiptFooter: String,
    val isTaxEnabled: Boolean = false,
    val isServiceChargeEnabled: Boolean = false,
    val dailySalesTarget: Double = 0.0,
    val defaultPrinterAddress: String? = null,
    val loyaltyPointsPerAmount: Double = 10000.0,
    val loyaltyPointsValue: Double = 100.0,
    val adminPin: String = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4", // SHA-256 hash of "1234"
    val bankName: String? = null,
    val bankAccountNumber: String? = null,
    val bankAccountHolderName: String? = null
)
