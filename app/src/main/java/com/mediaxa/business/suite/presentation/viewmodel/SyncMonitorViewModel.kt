package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.SyncQueueItem
import com.mediaxa.business.suite.data.sync.SyncEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SyncMonitorUiState(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val totalQueueSize: Int = 0,
    val lastSyncedAt: Long? = null,
    val failedItems: List<SyncQueueItem> = emptyList(),
    val pendingItems: List<SyncQueueItem> = emptyList(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val localTransactionsCount: Int = 0,
    val localTransactionItemsCount: Int = 0,
    val localPaymentsCount: Int = 0,
    val localStockMovementsCount: Int = 0,
    val currentStoreUuid: String = "unknown",
    val currentUserUuid: String = "unknown",
    val backendBaseUrl: String = "unknown",
    val lastCheckoutError: String? = null,
    val lastLoginError: String? = null
)

class SyncMonitorViewModel(
    private val context: android.content.Context,
    private val localDataSource: LocalDataSource,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncMonitorUiState())
    val uiState: StateFlow<SyncMonitorUiState> = _uiState.asStateFlow()

    private val appContext = context.applicationContext

    init {
        observeQueueState()
    }

    private fun observeQueueState() {
        viewModelScope.launch {
            combine(
                localDataSource.syncQueueDao.observePendingCount(),
                localDataSource.syncQueueDao.observeFailedCount(),
                localDataSource.syncQueueDao.observeTotalQueueSize(),
                localDataSource.syncQueueDao.observeLastSyncedAt(),
                localDataSource.syncQueueDao.observeFailedItems(),
                localDataSource.syncQueueDao.observePendingItems()
            ) { values ->
                SyncMonitorUiState(
                    pendingCount = values[0] as Int,
                    failedCount = values[1] as Int,
                    totalQueueSize = values[2] as Int,
                    lastSyncedAt = values[3] as Long?,
                    failedItems = @Suppress("UNCHECKED_CAST") (values[4] as List<SyncQueueItem>),
                    pendingItems = @Suppress("UNCHECKED_CAST") (values[5] as List<SyncQueueItem>),
                    isSyncing = _uiState.value.isSyncing
                )
            }.collect { state ->
                val txCount = localDataSource.transactionDao.getTransactionCount()
                val tiCount = localDataSource.transactionItemDao.getTransactionItemCount()
                val pmCount = localDataSource.paymentDao.getPaymentCount()
                val smCount = localDataSource.stockMovementDao.getMovementCount()
                val storeUuid = com.mediaxa.business.suite.data.local.PreferenceHelper.getStoreUuid(appContext) ?: "unknown"
                val userUuid = com.mediaxa.business.suite.data.local.PreferenceHelper.getUserUuid(appContext) ?: "unknown"
                val baseUrl = com.mediaxa.business.suite.data.remote.NetworkClient.baseUrl
                val lastErr = com.mediaxa.business.suite.data.repository.CheckoutService.lastCheckoutError
                val lastLoginErr = com.mediaxa.business.suite.data.remote.NetworkClient.lastLoginError

                _uiState.update { it.copy(
                    pendingCount = state.pendingCount,
                    failedCount = state.failedCount,
                    totalQueueSize = state.totalQueueSize,
                    lastSyncedAt = state.lastSyncedAt,
                    failedItems = state.failedItems,
                    pendingItems = state.pendingItems,
                    localTransactionsCount = txCount,
                    localTransactionItemsCount = tiCount,
                    localPaymentsCount = pmCount,
                    localStockMovementsCount = smCount,
                    currentStoreUuid = storeUuid,
                    currentUserUuid = userUuid,
                    backendBaseUrl = baseUrl,
                    lastCheckoutError = lastErr,
                    lastLoginError = lastLoginErr
                ) }
            }
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val result = syncEngine.processQueue()
            _uiState.update { it.copy(
                isSyncing = false,
                syncMessage = if (result.isFullSuccess) {
                    "Sinkronisasi berhasil: ${result.successCount} item"
                } else if (result.processedCount == 0) {
                    "Queue kosong — tidak ada yang perlu disinkronkan"
                } else {
                    "Selesai dengan ${result.failureCount} error. Silakan coba ulang."
                }
            ) }
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            syncEngine.retryFailedItems()
            val result = syncEngine.processQueue()
            _uiState.update { it.copy(
                isSyncing = false,
                syncMessage = "Retry selesai: ${result.successCount} berhasil, ${result.failureCount} gagal"
            ) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }

    class Factory(
        private val context: android.content.Context,
        private val localDataSource: LocalDataSource,
        private val syncEngine: SyncEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SyncMonitorViewModel(context, localDataSource, syncEngine) as T
        }
    }
}
