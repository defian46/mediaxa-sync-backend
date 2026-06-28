package com.mediaxa.business.suite.data.remote.datasource

import com.mediaxa.business.suite.data.remote.dto.*

/**
 * Result type for all remote synchronization calls.
 *
 * [isRetryable] on Failure distinguishes transient errors (network, 5xx)
 * from permanent client errors (400, 401, 403) where retrying is pointless.
 */
sealed class SyncResult {
    data class Success(
        val syncedCount: Int = 0,
        val serverTimestamp: Long? = null
    ) : SyncResult()

    data class Failure(
        val errorMsg: String,
        val httpCode: Int? = null,
        val isRetryable: Boolean = true
    ) : SyncResult()

    /** No network connectivity — do not mark as failure, just reschedule. */
    object NetworkUnavailable : SyncResult()

    /** Authentication token expired or missing — do not retry until re-auth. */
    object Unauthorized : SyncResult()
}

/**
 * Contract for all remote cloud synchronization operations.
 *
 * All methods accept batched payloads for efficiency and return a [SyncResult].
 * The implementation in Phase 9 is [MockRemoteDataSourceImpl].
 * A real Retrofit-based implementation will be added in Phase 10 once a backend exists.
 *
 * Web Dashboard fetch methods return payloads that can be forwarded to a cloud
 * dashboard service — they are not expected to modify local data.
 */
interface RemoteDataSource {

    // ─── Transaction Data ───────────────────────────────────────────────────

    suspend fun syncTransactions(payload: List<TransactionDto>): SyncResult
    suspend fun syncTransactionItems(payload: List<TransactionItemDto>): SyncResult
    suspend fun syncPayments(payload: List<PaymentDto>): SyncResult

    // ─── Customer & Loyalty ─────────────────────────────────────────────────

    suspend fun syncCustomers(payload: List<CustomerDto>): SyncResult
    suspend fun syncLoyaltyPointHistory(payload: List<LoyaltyPointHistoryDto>): SyncResult

    // ─── Menu & Category ────────────────────────────────────────────────────

    suspend fun syncMenus(payload: List<MenuDto>): SyncResult
    suspend fun syncCategories(payload: List<CategoryDto>): SyncResult

    // ─── Inventory ──────────────────────────────────────────────────────────

    suspend fun syncIngredients(payload: List<IngredientDto>): SyncResult
    suspend fun syncMenuRecipes(payload: List<MenuRecipeDto>): SyncResult
    suspend fun syncStockMovements(payload: List<StockMovementDto>): SyncResult

    // ─── Purchase & Opname ──────────────────────────────────────────────────

    suspend fun syncPurchaseExpenses(payload: List<PurchaseExpenseDto>): SyncResult
    suspend fun syncPurchaseExpenseItems(payload: List<PurchaseExpenseItemDto>): SyncResult
    suspend fun syncStockOpnames(payload: List<StockOpnameDto>): SyncResult
    suspend fun syncStockOpnameItems(payload: List<StockOpnameItemDto>): SyncResult

    // ─── Finance & Expense ──────────────────────────────────────────────────

    suspend fun syncExpenses(payload: List<ExpenseDto>): SyncResult
    suspend fun syncWasteLogs(payload: List<WasteLogDto>): SyncResult
    suspend fun syncCashShifts(payload: List<CashShiftDto>): SyncResult
    suspend fun syncDailyClosings(payload: List<DailyClosingDto>): SyncResult

    // ─── Promotions ─────────────────────────────────────────────────────────

    suspend fun syncPromotionRules(payload: List<PromotionRuleDto>): SyncResult

    // ─── Web Dashboard Snapshots (future cloud endpoints) ───────────────────

    suspend fun pushSalesDashboard(payload: SalesDashboardPayload): SyncResult
    suspend fun pushFinanceDashboard(payload: FinanceDashboardPayload): SyncResult
    suspend fun pushInventoryDashboard(payload: InventoryDashboardPayload): SyncResult
    suspend fun pushCrmDashboard(payload: CrmDashboardPayload): SyncResult
}
