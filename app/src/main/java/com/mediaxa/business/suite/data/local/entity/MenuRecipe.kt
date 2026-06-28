package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "menu_recipes",
    foreignKeys = [
        ForeignKey(
            entity = Menu::class,
            parentColumns = ["uuid"],
            childColumns = ["menuUuid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["uuid"],
            childColumns = ["ingredientUuid"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["menuUuid"]),
        Index(value = ["ingredientUuid"])
    ]
)
data class MenuRecipe(
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

    val menuUuid: String,
    val ingredientUuid: String,
    val quantityNeeded: Double
)
