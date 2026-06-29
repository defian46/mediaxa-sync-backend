package com.mediaxa.business.suite

import android.app.Application
import com.mediaxa.business.suite.data.local.PreferenceHelper
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.remote.NetworkClient
import com.mediaxa.business.suite.data.remote.datasource.HttpRemoteDataSourceImpl
import com.mediaxa.business.suite.data.sync.ConflictResolver
import com.mediaxa.business.suite.data.sync.DeviceIdManager
import com.mediaxa.business.suite.data.sync.PullSyncManager
import com.mediaxa.business.suite.data.sync.SyncEngine
import com.mediaxa.business.suite.data.sync.SyncManager
import com.mediaxa.business.suite.data.sync.SyncScheduler
import com.mediaxa.business.suite.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    // ─── Core Infrastructure ─────────────────────────────────────────────────
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val localDataSource by lazy { LocalDataSource(database) }
    val deviceId by lazy { DeviceIdManager.getOrCreate(this) }

    // ─── Remote — Live HTTP via NetworkClient ─────────────────────────────────
    val remoteDataSource by lazy {
        HttpRemoteDataSourceImpl(
            getStoreUuid = { PreferenceHelper.getStoreUuid(this) },
            deviceId = deviceId,
            getUserUuid = { PreferenceHelper.getUserUuid(this) },
            getToken = { PreferenceHelper.getAccessToken(this) }
        )
    }

    // ─── Sync Infrastructure ─────────────────────────────────────────────────
    val syncEngine by lazy {
        SyncEngine(
            syncQueueDao = localDataSource.syncQueueDao,
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource
        )
    }
    val syncManager by lazy { SyncManager(syncEngine) }
    val pullSyncManager by lazy { PullSyncManager(localDataSource) }

    // ─── Domain Repositories ─────────────────────────────────────────────────
    val userRepository by lazy { UserRepository(localDataSource) }
    val storeSettingRepository by lazy { StoreSettingRepository(localDataSource) }
    val productRepository by lazy { ProductRepository(localDataSource) }
    val inventoryRepository by lazy { InventoryRepository(localDataSource) }
    val transactionRepository by lazy { TransactionRepository(localDataSource) }
    val checkoutService by lazy { CheckoutService(database, localDataSource) }
    val salesRepository by lazy { SalesRepository(localDataSource) }
    val analyticsRepository by lazy { AnalyticsRepository(localDataSource) }
    val expenseRepository by lazy { ExpenseRepository(database, localDataSource) }
    val purchaseExpenseRepository by lazy { PurchaseExpenseRepository(database, localDataSource) }
    val stockOpnameRepository by lazy { StockOpnameRepository(database, localDataSource) }
    val wasteRepository by lazy { WasteRepository(database, localDataSource) }
    val inventoryLiteRepository by lazy { InventoryLiteRepository(localDataSource) }
    val financeRepository by lazy { FinanceRepository(localDataSource) }
    val customerRepository by lazy { CustomerRepository(localDataSource) }
    val loyaltyRepository by lazy { LoyaltyRepository(localDataSource) }
    val promotionRepository by lazy { PromotionRepository(localDataSource) }

    override fun onCreate() {
        super.onCreate()

        // ── Configure NetworkClient URL ────────────────────────────────────────
        // Debug builds: 10.0.2.2 → localhost on host machine (emulator loopback)
        // Release builds: points to Render production backend
        NetworkClient.configure(productionUrl = BuildConfig.SYNC_BACKEND_URL)

        // ── Global exception / crash handler ──────────────────────────────────
        CrashManager.init(this)

        // ── Start periodic background sync (15-min interval, network-gated) ───
        SyncScheduler.schedulePeriodicSync(this)
    }
}
