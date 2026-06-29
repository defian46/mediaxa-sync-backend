package com.mediaxa.business.suite.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ─────────────────────────────────────────────────────────────────────────────
// Cloud API Response Envelope
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val serverTimestamp: Long = 0L,
    val requestId: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// Transaction DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TransactionDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val transactionNumber: String,
    val timestamp: Long,
    val cashierUuid: String,
    val cashierName: String,
    val discount: Double,
    val subtotal: Double,
    val total: Double,
    val transactionHpp: Double,
    val grossProfit: Double,
    val paymentMethod: String,
    val amountReceived: Double,
    val changeAmount: Double,
    val status: String,
    val customerUuid: String? = null,
    val pointsEarned: Int? = null,
    val pointsRedeemed: Int? = null,
    val notes: String? = null,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class TransactionItemDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val transactionUuid: String,
    val menuUuid: String,
    val menuName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val hpp: Double,
    val notes: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class PaymentDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val transactionUuid: String,
    @SerialName("paymentMethod") val method: String,
    @SerialName("amountPaid") val amount: Double,
    val referenceNumber: String? = null,
    @SerialName("paymentTime") val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Customer & Loyalty DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CustomerDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val customerCode: String,
    val customerName: String,
    val phone: String? = null,
    val email: String? = null,
    val birthday: Long? = null,
    val gender: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val joinDate: Long,
    val membershipLevel: String,
    val totalSpending: Double,
    val lastVisit: Long? = null,
    val favoriteMenuUuid: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class LoyaltyPointHistoryDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val customerUuid: String,
    val transactionUuid: String? = null,
    val points: Int,
    val activityType: String,
    val expiryTime: Long? = null,
    val notes: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Menu & Category DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class MenuDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val name: String,
    val categoryUuid: String,
    val price: Double,
    val promoPrice: Double? = null,
    val isActive: Boolean,
    val description: String? = null,
    val estimatedHpp: Double,
    val estimatedMargin: Double,
    val imageUrl: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class CategoryDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val name: String,
    val displayOrder: Int,
    val isActive: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Inventory DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class IngredientDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val name: String,
    val unit: String,
    val purchasePrice: Double,
    val packageSize: Double,
    val availableStock: Double,
    val minStock: Double,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class MenuRecipeDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val menuUuid: String,
    val ingredientUuid: String,
    val quantityNeeded: Double,
    val unit: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class StockMovementDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val ingredientUuid: String,
    val movementType: String,
    val quantity: Double,
    val referenceUuid: String? = null,
    val referenceType: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// Purchase Expense DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class PurchaseExpenseDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val purchaseDate: Long,
    val purchasePlaceName: String? = null,
    val paymentMethod: String,
    val notes: String? = null,
    val totalAmount: Double,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class PurchaseExpenseItemDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val purchaseExpenseUuid: String,
    val ingredientUuid: String,
    val quantity: Double,
    val unit: String,
    val totalPrice: Double,
    val unitPrice: Double,
    val batchNumber: String? = null,
    val expiredDate: Long? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Stock Opname DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class StockOpnameDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val opnameDate: Long,
    val userUuid: String,
    val notes: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class StockOpnameItemDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val opnameUuid: String,
    val ingredientUuid: String,
    val systemStock: Double,
    val physicalStock: Double,
    val diffStock: Double,
    val notes: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Waste Log DTO
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class WasteLogDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val wasteDate: Long,
    val ingredientUuid: String,
    val quantity: Double,
    val reason: String,
    val calculatedCost: Double,
    val userUuid: String,
    val notes: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Expense DTO
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ExpenseDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val expenseDate: Long,
    val category: String,
    val amount: Double,
    val notes: String? = null,
    val userUuid: String,
    val paymentMethod: String,
    val attachmentPath: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Promotion DTO
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class PromotionRuleDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val name: String,
    val promoType: String,
    val value: Double,
    val buyMenuUuid: String? = null,
    val buyQuantity: Int? = null,
    val getMenuUuid: String? = null,
    val getQuantity: Int? = null,
    val minPurchaseAmount: Double? = null,
    val isActive: Boolean,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val startHour: Int? = null,
    val endHour: Int? = null,
    val applicableDays: String? = null,
    val targetMembershipLevels: String? = null,
    val targetCategoryUuid: String? = null,
    val targetMenuUuid: String? = null,
    val promoCode: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Finance DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CashShiftDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val cashierUuid: String,
    val startTime: Long,
    val endTime: Long? = null,
    val openingCash: Double,
    val closingCash: Double? = null,
    val expectedCash: Double? = null,
    val actualCash: Double? = null,
    val cashDifference: Double? = null,
    val status: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class DailyClosingDto(
    val uuid: String,
    val storeId: Long,
    val deviceId: String,
    val dateStr: String,
    val openingBalance: Double,
    val revenue: Double,
    val hpp: Double,
    val grossProfit: Double,
    val operationalExpense: Double,
    val wasteCost: Double,
    val netProfit: Double,
    val cashInflow: Double,
    val cashOutflow: Double,
    val closingBalance: Double,
    val cashRevenue: Double,
    val qrisRevenue: Double,
    val transferRevenue: Double,
    val totalTransactions: Int,
    val averageTicket: Double,
    val closedByUserUuid: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Web Dashboard Snapshot DTOs (for future cloud dashboard)
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class SalesDashboardPayload(
    val storeId: Long,
    val fromDate: Long,
    val toDate: Long,
    val totalRevenue: Double,
    val totalTransactions: Int,
    val averageTicket: Double,
    val topMenus: List<TopMenuEntry>
)

@Serializable
data class TopMenuEntry(val menuName: String, val quantity: Int, val revenue: Double)

@Serializable
data class FinanceDashboardPayload(
    val storeId: Long,
    val month: String,
    val totalRevenue: Double,
    val totalHpp: Double,
    val grossProfit: Double,
    val operationalExpense: Double,
    val wasteCost: Double,
    val netProfit: Double,
    val grossMarginPct: Double,
    val netMarginPct: Double
)

@Serializable
data class InventoryDashboardPayload(
    val storeId: Long,
    val snapshotAt: Long,
    val totalIngredients: Int,
    val lowStockCount: Int,
    val totalStockValue: Double
)

@Serializable
data class CrmDashboardPayload(
    val storeId: Long,
    val snapshotAt: Long,
    val totalCustomers: Int,
    val activeCustomers: Int,
    val totalLoyaltyPoints: Long,
    val activePromotions: Int
)
