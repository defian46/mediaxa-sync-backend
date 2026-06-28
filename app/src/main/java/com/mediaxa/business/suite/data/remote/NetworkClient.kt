package com.mediaxa.business.suite.data.remote

import android.util.Log
import com.mediaxa.business.suite.data.local.entity.SyncQueueItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String,
    val deviceName: String = "Android Terminal"
)

@Serializable
data class UserResponse(
    val uuid: String,
    val storeUuid: String,
    val username: String,
    val role: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

@Serializable
data class ClientMutationDto(
    val clientMutationId: String,
    val uuid: String,
    val entityType: String,
    val operation: String,
    val payload: String,
    val updatedAt: Long
)

@Serializable
data class PushRequest(
    val storeUuid: String,
    val deviceId: String,
    val userUuid: String,
    val mutations: List<ClientMutationDto>
)

@Serializable
data class PushResponse(
    val status: String,
    val syncedIds: List<String>,
    val conflicts: List<ConflictDto> = emptyList(),
    val failedIds: List<String> = emptyList()
)

@Serializable
data class ConflictDto(
    val clientMutationId: String,
    val uuid: String,
    val entityType: String,
    val reason: String
)

@Serializable
data class CategoryPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val name: String,
    val isDeleted: Boolean = false,
    val updatedAt: String
)

@Serializable
data class MenuPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val name: String,
    val price: Double,
    val categoryUuid: String,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val updatedAt: String
)

@Serializable
data class IngredientPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val name: String,
    val availableStock: Double,
    val unit: String,
    val isDeleted: Boolean = false,
    val updatedAt: String
)

@Serializable
data class TransactionPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val cashierUserUuid: String,
    val transactionDate: String,
    val subtotal: Double,
    val transactionHpp: Double,
    val grossProfit: Double,
    val marginPercent: Double,
    val total: Double,
    val paymentMethod: String,
    val status: String,
    val updatedAt: String
)

@Serializable
data class TransactionItemPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val transactionUuid: String,
    val menuUuid: String,
    val qty: Double,
    val sellingPrice: Double,
    val costPrice: Double,
    val subtotal: Double,
    val grossProfit: Double,
    val marginPercent: Double,
    val updatedAt: String
)

@Serializable
data class PaymentPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val transactionUuid: String,
    val method: String,
    val amount: Double,
    val updatedAt: String
)

@Serializable
data class StockMovementPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val ingredientUuid: String,
    val type: String,
    val qty: Double,
    val reason: String,
    val movementDate: String,
    val updatedAt: String
)

@Serializable
data class ExpensePullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val amount: Double,
    val description: String,
    val paymentMethod: String,
    val category: String,
    val expenseDate: String,
    val updatedAt: String
)

@Serializable
data class CustomerPullDto(
    val uuid: String,
    val storeUuid: String,
    val deviceId: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val points: Int = 0,
    val updatedAt: String
)

@Serializable
data class PullResponse(
    val categories: List<CategoryPullDto> = emptyList(),
    val menus: List<MenuPullDto> = emptyList(),
    val ingredients: List<IngredientPullDto> = emptyList(),
    val transactions: List<TransactionPullDto> = emptyList(),
    val transactionItems: List<TransactionItemPullDto> = emptyList(),
    val payments: List<PaymentPullDto> = emptyList(),
    val stockMovements: List<StockMovementPullDto> = emptyList(),
    val expenses: List<ExpensePullDto> = emptyList(),
    val customers: List<CustomerPullDto> = emptyList()
)

object NetworkClient {
    private const val TAG = "NetworkClient"

    /**
     * Base API URL — set this to your Render backend URL in production.
     *
     * Debug (emulator): http://10.0.2.2:3000/api/v1  (routes to localhost on host machine)
     * Production:       https://mediaxa-sync-backend.onrender.com/api/v1
     *
     * To switch environment, update SYNC_BACKEND_URL in app/build.gradle:
     *   buildConfigField("String", "SYNC_BACKEND_URL", '"https://your-render-url.onrender.com/api/v1"')
     */
    var baseUrl: String = "http://10.0.2.2:3000/api/v1"

    /** Call this at Application.onCreate() after BuildConfig is available */
    fun configure(productionUrl: String? = null) {
        if (!productionUrl.isNullOrBlank()) {
            baseUrl = productionUrl
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    fun login(username: String, passwordRaw: String, deviceId: String): LoginResponse? {
        try {
            val url = URL("$baseUrl/auth/login")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val body = json.encodeToString(LoginRequest(username, passwordRaw, deviceId))
            OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                return json.decodeFromString<LoginResponse>(responseText)
            } else {
                Log.e(TAG, "Login failed with code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login network connection error", e)
        }
        return null
    }

    fun push(
        storeUuid: String,
        deviceId: String,
        userUuid: String,
        mutations: List<SyncQueueItem>,
        accessToken: String
    ): PushResponse? {
        try {
            val url = URL("$baseUrl/sync/push")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.doOutput = true

            val dtoList = mutations.map {
                ClientMutationDto(
                    clientMutationId = "mut-${it.localId}-${it.createdAt}",
                    uuid = it.uuid,
                    entityType = it.entityType,
                    operation = it.operation,
                    payload = it.payload,
                    updatedAt = it.createdAt
                )
            }

            val body = json.encodeToString(PushRequest(storeUuid, deviceId, userUuid, dtoList))
            OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                return json.decodeFromString<PushResponse>(responseText)
            } else {
                Log.e(TAG, "Push failed with code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Push network connection error", e)
        }
        return null
    }

    fun pull(
        storeUuid: String,
        lastSyncTime: Long,
        accessToken: String
    ): PullResponse? {
        try {
            val url = URL("$baseUrl/sync/pull?storeUuid=$storeUuid&lastSyncTime=$lastSyncTime&limit=100")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                return json.decodeFromString<PullResponse>(responseText)
            } else {
                Log.e(TAG, "Pull failed with code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pull network connection error", e)
        }
        return null
    }
    
    fun parseIsoDateTime(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
