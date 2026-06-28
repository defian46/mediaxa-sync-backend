package com.mediaxa.business.suite.data.remote.datasource

import com.mediaxa.business.suite.data.remote.dto.*
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock implementation of [RemoteDataSource] for Phase 9.
 *
 * Simulates realistic network latency (50–200ms) and always returns [SyncResult.Success].
 * This class is the integration point for a real Retrofit implementation in Phase 10.
 *
 * To simulate failure scenarios during testing, use [MockRemoteDataSourceImpl]
 * with a custom [failureProbability] (0.0 = never fail, 1.0 = always fail).
 */
class MockRemoteDataSourceImpl(
    private val failureProbability: Double = 0.0,
    private val simulatedDelayMs: LongRange = 50L..200L
) : RemoteDataSource {

    private suspend fun simulateRequest(): SyncResult {
        val delayMs = if (simulatedDelayMs.first >= simulatedDelayMs.last) {
            simulatedDelayMs.first
        } else {
            Random.nextLong(simulatedDelayMs.first, simulatedDelayMs.last)
        }
        if (delayMs > 0) {
            delay(delayMs)
        }
        return if (Random.nextDouble() < failureProbability) {
            SyncResult.Failure(errorMsg = "Simulated network error", isRetryable = true)
        } else {
            SyncResult.Success(syncedCount = 1, serverTimestamp = System.currentTimeMillis())
        }
    }

    // ─── Transaction Data ───────────────────────────────────────────────────
    override suspend fun syncTransactions(payload: List<TransactionDto>) = simulateRequest()
    override suspend fun syncTransactionItems(payload: List<TransactionItemDto>) = simulateRequest()
    override suspend fun syncPayments(payload: List<PaymentDto>) = simulateRequest()

    // ─── Customer & Loyalty ─────────────────────────────────────────────────
    override suspend fun syncCustomers(payload: List<CustomerDto>) = simulateRequest()
    override suspend fun syncLoyaltyPointHistory(payload: List<LoyaltyPointHistoryDto>) = simulateRequest()

    // ─── Menu & Category ────────────────────────────────────────────────────
    override suspend fun syncMenus(payload: List<MenuDto>) = simulateRequest()
    override suspend fun syncCategories(payload: List<CategoryDto>) = simulateRequest()

    // ─── Inventory ──────────────────────────────────────────────────────────
    override suspend fun syncIngredients(payload: List<IngredientDto>) = simulateRequest()
    override suspend fun syncMenuRecipes(payload: List<MenuRecipeDto>) = simulateRequest()
    override suspend fun syncStockMovements(payload: List<StockMovementDto>) = simulateRequest()

    // ─── Purchase & Opname ──────────────────────────────────────────────────
    override suspend fun syncPurchaseExpenses(payload: List<PurchaseExpenseDto>) = simulateRequest()
    override suspend fun syncPurchaseExpenseItems(payload: List<PurchaseExpenseItemDto>) = simulateRequest()
    override suspend fun syncStockOpnames(payload: List<StockOpnameDto>) = simulateRequest()
    override suspend fun syncStockOpnameItems(payload: List<StockOpnameItemDto>) = simulateRequest()

    // ─── Finance & Expense ──────────────────────────────────────────────────
    override suspend fun syncExpenses(payload: List<ExpenseDto>) = simulateRequest()
    override suspend fun syncWasteLogs(payload: List<WasteLogDto>) = simulateRequest()
    override suspend fun syncCashShifts(payload: List<CashShiftDto>) = simulateRequest()
    override suspend fun syncDailyClosings(payload: List<DailyClosingDto>) = simulateRequest()

    // ─── Promotions ─────────────────────────────────────────────────────────
    override suspend fun syncPromotionRules(payload: List<PromotionRuleDto>) = simulateRequest()

    // ─── Web Dashboard Snapshots ─────────────────────────────────────────────
    override suspend fun pushSalesDashboard(payload: SalesDashboardPayload) = simulateRequest()
    override suspend fun pushFinanceDashboard(payload: FinanceDashboardPayload) = simulateRequest()
    override suspend fun pushInventoryDashboard(payload: InventoryDashboardPayload) = simulateRequest()
    override suspend fun pushCrmDashboard(payload: CrmDashboardPayload) = simulateRequest()
}
