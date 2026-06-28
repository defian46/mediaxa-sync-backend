package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.SyncQueueDao
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.remote.datasource.MockRemoteDataSourceImpl
import com.mediaxa.business.suite.data.remote.datasource.SyncResult
import com.mediaxa.business.suite.data.remote.dto.TransactionDto
import com.mediaxa.business.suite.data.sync.SyncEngine
import com.mediaxa.business.suite.data.sync.SyncEngineResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncEngineTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ─── Fake DAO ────────────────────────────────────────────────────────────

    private inner class FakeSyncQueueDao(
        private val items: MutableList<SyncQueueItem> = mutableListOf()
    ) : SyncQueueDao {

        override suspend fun enqueue(item: SyncQueueItem): Long {
            items.add(item.copy(localId = items.size.toLong() + 1))
            return items.size.toLong()
        }

        override suspend fun update(item: SyncQueueItem) {
            val idx = items.indexOfFirst { it.localId == item.localId }
            if (idx >= 0) items[idx] = item
        }

        override suspend fun getPendingItems(now: Long, limit: Int): List<SyncQueueItem> {
            return items.filter { it.status == SyncQueueStatus.PENDING.name && it.nextRetryAt <= now }
                .take(limit)
        }

        override suspend fun markInProgress(ids: List<Long>, now: Long) {
            ids.forEach { id ->
                val idx = items.indexOfFirst { it.localId == id }
                if (idx >= 0) items[idx] = items[idx].copy(
                    status = SyncQueueStatus.IN_PROGRESS.name,
                    lastAttemptAt = now
                )
            }
        }

        override suspend fun markSynced(localId: Long) {
            val idx = items.indexOfFirst { it.localId == localId }
            if (idx >= 0) items[idx] = items[idx].copy(status = SyncQueueStatus.SYNCED.name)
        }

        override suspend fun incrementRetry(localId: Long, nextRetryAt: Long, errorMsg: String, now: Long) {
            val idx = items.indexOfFirst { it.localId == localId }
            if (idx >= 0) items[idx] = items[idx].copy(
                status = SyncQueueStatus.PENDING.name,
                retryCount = items[idx].retryCount + 1,
                nextRetryAt = nextRetryAt,
                errorMessage = errorMsg
            )
        }

        override suspend fun markFailed(localId: Long, errorMsg: String) {
            val idx = items.indexOfFirst { it.localId == localId }
            if (idx >= 0) items[idx] = items[idx].copy(
                status = SyncQueueStatus.FAILED.name,
                errorMessage = errorMsg
            )
        }

        override suspend fun cancelByEntityUuid(entityUuid: String, entityType: String) {}
        override fun observePendingCount(): Flow<Int> = flowOf(items.count { it.status == "PENDING" })
        override fun observeFailedCount(): Flow<Int> = flowOf(items.count { it.status == "FAILED" })
        override fun observeFailedItems(): Flow<List<SyncQueueItem>> = flowOf(items.filter { it.status == "FAILED" })
        override fun observePendingItems(): Flow<List<SyncQueueItem>> = flowOf(items.filter { it.status == "PENDING" })
        override fun observeLastSyncedAt(): Flow<Long?> = flowOf(null)
        override fun observeTotalQueueSize(): Flow<Int> = flowOf(items.size)

        override suspend fun clearSyncedItems(olderThanMs: Long) {
            items.removeAll { it.status == SyncQueueStatus.SYNCED.name && (it.lastAttemptAt ?: 0) < olderThanMs }
        }

        override suspend fun recoverStuckItems() {
            items.replaceAll { if (it.status == SyncQueueStatus.IN_PROGRESS.name) it.copy(status = SyncQueueStatus.PENDING.name) else it }
        }

        override suspend fun resetFailedItems() {
            items.replaceAll { if (it.status == SyncQueueStatus.FAILED.name) it.copy(status = SyncQueueStatus.PENDING.name, retryCount = 0, nextRetryAt = 0) else it }
        }

        override suspend fun getSyncedCount(): Int = items.count { it.status == "SYNCED" }

        fun getAll() = items.toList()
    }

    private fun makeTransactionPayload(uuid: String): String {
        val dto = TransactionDto(
            uuid = uuid, storeId = 1, deviceId = "DEV-TEST",
            transactionNumber = "TRX-001", timestamp = 1000L,
            cashierUuid = "user-1", cashierName = "Kasir",
            discount = 0.0, subtotal = 50000.0, total = 50000.0,
            transactionHpp = 20000.0, grossProfit = 30000.0,
            paymentMethod = "CASH", amountReceived = 50000.0, changeAmount = 0.0,
            status = "PAID", updatedAt = System.currentTimeMillis()
        )
        return Json.encodeToString(dto)
    }

    private fun pendingItem(uuid: String = "test-uuid", entityType: String = SyncEntityType.TRANSACTION.name) =
        SyncQueueItem(
            localId = 0,
            uuid = uuid,
            storeId = 1,
            deviceId = "DEV-TEST",
            entityType = entityType,
            operation = SyncOperation.CREATE.name,
            payload = makeTransactionPayload(uuid),
            status = SyncQueueStatus.PENDING.name,
            nextRetryAt = 0L,
            createdAt = System.currentTimeMillis()
        )

    // ─── Test Cases ───────────────────────────────────────────────────────────

    @Test
    fun whenQueueIsEmptyResultHasZeroProcessedCount() = runTest {
        val dao = FakeSyncQueueDao()
        val remote = MockRemoteDataSourceImpl()
        val engine = SyncEngine(dao, remote)

        val result = engine.processQueue()

        assertEquals(0, result.processedCount)
        assertEquals(0, result.successCount)
        assertEquals(0, result.failureCount)
        assertFalse(result.hasFailures)
    }

    @Test
    fun successfulSyncMarksItemAsSynced() = runTest {
        val dao = FakeSyncQueueDao()
        val remote = MockRemoteDataSourceImpl(failureProbability = 0.0, simulatedDelayMs = 1L..1L)
        val engine = SyncEngine(dao, remote)

        dao.enqueue(pendingItem("uuid-1"))
        val result = engine.processQueue()

        assertEquals(1, result.processedCount)
        assertEquals(1, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(SyncQueueStatus.SYNCED.name, dao.getAll().first { it.uuid == "uuid-1" }.status)
    }

    @Test
    fun failedSyncIncrementsRetryCountAndSetsNextRetryAt() = runTest {
        val dao = FakeSyncQueueDao()
        val remote = MockRemoteDataSourceImpl(failureProbability = 1.0, simulatedDelayMs = 1L..1L)
        val engine = SyncEngine(dao, remote)

        dao.enqueue(pendingItem("uuid-fail"))
        engine.processQueue()

        val item = dao.getAll().first { it.uuid == "uuid-fail" }
        assertEquals(SyncQueueStatus.PENDING.name, item.status)
        assertEquals(1, item.retryCount)
        assertTrue(item.nextRetryAt > System.currentTimeMillis() - 1000)
    }

    @Test
    fun itemMarkedFailedAfterExceedingMaxRetries() = runTest {
        val dao = FakeSyncQueueDao()
        val remote = MockRemoteDataSourceImpl(failureProbability = 1.0, simulatedDelayMs = 1L..1L)
        val engine = SyncEngine(dao, remote)

        val item = pendingItem("uuid-exhaust").copy(retryCount = 5, maxRetries = 5)
        dao.enqueue(item)

        engine.processQueue()

        val result = dao.getAll().first { it.uuid == "uuid-exhaust" }
        assertEquals(SyncQueueStatus.FAILED.name, result.status)
    }

    @Test
    fun duplicatePayloadBothProcessedIdempotencyIsServerSide() = runTest {
        val dao = FakeSyncQueueDao()
        val remote = MockRemoteDataSourceImpl(failureProbability = 0.0, simulatedDelayMs = 1L..1L)
        val engine = SyncEngine(dao, remote)

        dao.enqueue(pendingItem("duplicate-uuid"))
        dao.enqueue(pendingItem("duplicate-uuid"))

        val result = engine.processQueue()

        assertEquals(2, result.processedCount)
        assertEquals(2, result.successCount)
    }

    @Test
    fun stuckInProgressItemsResetToPendingOnStartup() = runTest {
        val stuckItem = pendingItem("stuck-uuid").copy(
            localId = 1,
            status = SyncQueueStatus.IN_PROGRESS.name
        )
        val dao = FakeSyncQueueDao(mutableListOf(stuckItem))
        val remote = MockRemoteDataSourceImpl(failureProbability = 0.0, simulatedDelayMs = 1L..1L)
        val engine = SyncEngine(dao, remote)

        engine.processQueue()

        val all = dao.getAll()
        assertTrue(all.any { it.uuid == "stuck-uuid" && it.status == SyncQueueStatus.SYNCED.name })
    }

    @Test
    fun failedItemsResetToPendingWhenRetryFailedCalled() = runTest {
        val failedItem = pendingItem("failed-uuid").copy(
            localId = 1,
            status = SyncQueueStatus.FAILED.name,
            retryCount = 5
        )
        val dao = FakeSyncQueueDao(mutableListOf(failedItem))
        val remote = MockRemoteDataSourceImpl(failureProbability = 0.0, simulatedDelayMs = 1L..1L)
        val engine = SyncEngine(dao, remote)

        engine.retryFailedItems()

        val item = dao.getAll().first()
        assertEquals(SyncQueueStatus.PENDING.name, item.status)
        assertEquals(0, item.retryCount)
    }
}
