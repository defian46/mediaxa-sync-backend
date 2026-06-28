package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "purchase_expenses",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "purchaseDate"])
    ]
)
data class PurchaseExpense(
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

    val purchaseDate: Long,
    val purchasePlaceName: String?,
    val paymentMethod: String,
    val notes: String?,
    val totalAmount: Double
)

@Entity(
    tableName = "purchase_expense_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseExpense::class,
            parentColumns = ["uuid"],
            childColumns = ["purchaseExpenseUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["purchaseExpenseUuid"]),
        Index(value = ["ingredientUuid"])
    ]
)
data class PurchaseExpenseItem(
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

    val purchaseExpenseUuid: String,
    val ingredientUuid: String,
    val quantity: Double,
    val unit: String,
    val totalPrice: Double,
    val unitPrice: Double, // calculated: totalPrice / quantity
    val batchNumber: String?,
    val expiredDate: Long?
)
