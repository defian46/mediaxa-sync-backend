package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "promotion_rules",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["storeId", "isActive"]),
        Index(value = ["promoCode"])
    ]
)
data class PromotionRule(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val storeId: Long = 1L,
    val deviceId: String = "DEV-01",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0,
    val syncStatus: String = "PENDING_INSERT",

    val name: String,
    val promoType: String, // "BUY_X_GET_Y", "PERCENTAGE_DISCOUNT", "NOMINAL_DISCOUNT"
    val value: Double, // Discount percentage or nominal amount
    
    // Conditions
    val buyMenuUuid: String? = null,
    val buyQuantity: Int? = null,
    val getMenuUuid: String? = null,
    val getQuantity: Int? = null,
    val minPurchaseAmount: Double? = null,
    
    // Scheduling and targeting
    val isActive: Boolean = true,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val startHour: Int? = null, // Happy Hour hour (0-23)
    val endHour: Int? = null,
    val applicableDays: String? = null, // Comma-separated days of week, e.g. "SATURDAY,SUNDAY"
    val targetMembershipLevels: String? = null, // Comma-separated tiers, e.g. "GOLD,PLATINUM"
    
    val targetCategoryUuid: String? = null,
    val targetMenuUuid: String? = null,
    val promoCode: String? = null // For voucher codes
)
