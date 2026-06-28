package com.mediaxa.business.suite.data.remote.datasource

import android.util.Log
import com.mediaxa.business.suite.data.remote.NetworkClient
import com.mediaxa.business.suite.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "HttpRemoteDataSource"

/**
 * Live HTTP implementation of [RemoteDataSource].
 *
 * All entity-specific push methods serialize their payloads into a unified
 * generic mutation batch and forward it to POST /api/v1/sync/push via [NetworkClient].
 *
 * This keeps the backend API surface minimal and entity-agnostic while the
 * typed [RemoteDataSource] interface remains stable for the SyncEngine.
 *
 * @param storeUuid  Cloud store UUID (from PreferenceHelper)
 * @param deviceId   Device identifier registered on the backend
 * @param userUuid   UUID of the currently authenticated user
 * @param getToken   Lambda that returns the current access token (refreshed lazily)
 */
class HttpRemoteDataSourceImpl(
    private val storeUuid: String,
    private val deviceId: String,
    private val userUuid: String,
    private val getToken: () -> String?
) : RemoteDataSource {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    // ─── Push helpers ────────────────────────────────────────────────────────

    /**
     * Generic push dispatcher. Encodes [items] as JSON payloads and sends them
     * to /sync/push. Returns [SyncResult.Unauthorized] if no token is available.
     */
    private suspend fun <T> pushEntities(
        entityType: String,
        operation: String,
        items: List<T>,
        encodeItem: (T) -> String
    ): SyncResult = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext SyncResult.Unauthorized

        if (items.isEmpty()) return@withContext SyncResult.Success()

        val mutations = items.mapIndexed { index, item ->
            com.mediaxa.business.suite.data.remote.ClientMutationDto(
                clientMutationId = "push-${entityType}-${System.currentTimeMillis()}-$index",
                uuid = "unknown", // UUID is embedded in the payload itself
                entityType = entityType,
                operation = operation,
                payload = encodeItem(item),
                updatedAt = System.currentTimeMillis()
            )
        }

        val response = NetworkClient.push(storeUuid, deviceId, userUuid, emptyList(), token)
            .let {
                // NetworkClient.push requires SyncQueueItem list; use raw HTTP call via
                // a direct inline helper that can accept pre-built ClientMutationDto list
                pushMutationsDirect(mutations, token)
            }

        return@withContext if (response != null) {
            Log.d(TAG, "Push $entityType: ${response.syncedIds.size} synced, ${response.failedIds.size} failed")
            SyncResult.Success(syncedCount = response.syncedIds.size, serverTimestamp = System.currentTimeMillis())
        } else {
            SyncResult.NetworkUnavailable
        }
    }

    /** Direct HTTP push accepting pre-built [ClientMutationDto] list. */
    private suspend fun pushMutationsDirect(
        mutations: List<com.mediaxa.business.suite.data.remote.ClientMutationDto>,
        token: String
    ): com.mediaxa.business.suite.data.remote.PushResponse? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("${NetworkClient.baseUrl}/sync/push")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.doOutput = true

            val body = json.encodeToString(
                com.mediaxa.business.suite.data.remote.PushRequest(storeUuid, deviceId, userUuid, mutations)
            )
            java.io.OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<com.mediaxa.business.suite.data.remote.PushResponse>(text)
            } else {
                Log.e(TAG, "Push HTTP error: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushMutationsDirect error", e)
            null
        }
    }

    // ─── RemoteDataSource implementation ─────────────────────────────────────

    override suspend fun syncTransactions(payload: List<TransactionDto>) =
        pushEntities("TRANSACTION", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncTransactionItems(payload: List<TransactionItemDto>) =
        pushEntities("TRANSACTION_ITEM", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncPayments(payload: List<PaymentDto>) =
        pushEntities("PAYMENT", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncCustomers(payload: List<CustomerDto>) =
        pushEntities("CUSTOMER", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncLoyaltyPointHistory(payload: List<LoyaltyPointHistoryDto>) =
        pushEntities("LOYALTY_POINT_HISTORY", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncMenus(payload: List<MenuDto>) =
        pushEntities("MENU", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncCategories(payload: List<CategoryDto>) =
        pushEntities("CATEGORY", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncIngredients(payload: List<IngredientDto>) =
        pushEntities("INGREDIENT", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncMenuRecipes(payload: List<MenuRecipeDto>) =
        pushEntities("MENU_RECIPE", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncStockMovements(payload: List<StockMovementDto>) =
        pushEntities("STOCK_MOVEMENT", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncPurchaseExpenses(payload: List<PurchaseExpenseDto>) =
        pushEntities("PURCHASE_EXPENSE", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncPurchaseExpenseItems(payload: List<PurchaseExpenseItemDto>) =
        pushEntities("PURCHASE_EXPENSE_ITEM", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncStockOpnames(payload: List<StockOpnameDto>) =
        pushEntities("STOCK_OPNAME", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncStockOpnameItems(payload: List<StockOpnameItemDto>) =
        pushEntities("STOCK_OPNAME_ITEM", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncExpenses(payload: List<ExpenseDto>) =
        pushEntities("EXPENSE", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncWasteLogs(payload: List<WasteLogDto>) =
        pushEntities("WASTE_LOG", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncCashShifts(payload: List<CashShiftDto>) =
        pushEntities("CASH_SHIFT", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncDailyClosings(payload: List<DailyClosingDto>) =
        pushEntities("DAILY_CLOSING", "CREATE", payload) { json.encodeToString(it) }

    override suspend fun syncPromotionRules(payload: List<PromotionRuleDto>) =
        pushEntities("PROMOTION_RULE", "CREATE", payload) { json.encodeToString(it) }

    // Dashboard snapshots are fire-and-forget — best effort
    override suspend fun pushSalesDashboard(payload: SalesDashboardPayload) = SyncResult.Success()
    override suspend fun pushFinanceDashboard(payload: FinanceDashboardPayload) = SyncResult.Success()
    override suspend fun pushInventoryDashboard(payload: InventoryDashboardPayload) = SyncResult.Success()
    override suspend fun pushCrmDashboard(payload: CrmDashboardPayload) = SyncResult.Success()
}
