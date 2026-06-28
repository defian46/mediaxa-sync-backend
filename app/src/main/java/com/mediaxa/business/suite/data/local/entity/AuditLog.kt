package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1,
    val deviceId: String = "DEV-01",
    val userUuid: String,
    val username: String,
    val action: String, // e.g. LOGIN, LOGOUT, VOID, STOCK_ADJUST, EDIT_MENU
    val entity: String, // e.g. User, Transaction, Ingredient, Menu
    val entityId: String?, // uuid of target entity
    val oldValue: String?, // representation of old state
    val newValue: String?, // representation of new state
    val createdAt: Long = System.currentTimeMillis()
)
