package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "expenseDate"]),
        Index(value = ["category"])
    ]
)
data class Expense(
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

    val expenseDate: Long,
    val category: String, // Sewa, Listrik, Air, Gaji, Marketing, Transport, dll.
    val amount: Double,
    val notes: String?,
    val userUuid: String,
    val paymentMethod: String,
    val attachmentPath: String? = null
)

enum class ExpenseCategory(val displayName: String) {
    INVENTORY_PURCHASE("Belanja Bahan Baku"),
    RENT("Sewa"),
    ELECTRICITY("Listrik"),
    WATER("Air"),
    SALARY("Gaji"),
    MARKETING("Marketing"),
    TRANSPORT("Transport"),
    MAINTENANCE("Maintenance"),
    CLEANING("Kebersihan"),
    OTHER("Lain-lain");

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull { it.name == value || it.displayName == value } ?: OTHER
        }
    }
}

