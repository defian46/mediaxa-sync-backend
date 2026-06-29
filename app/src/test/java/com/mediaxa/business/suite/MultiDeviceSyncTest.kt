package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.remote.datasource.MockRemoteDataSourceImpl
import com.mediaxa.business.suite.data.sync.SyncEngine
import com.mediaxa.business.suite.data.sync.ConflictResolver
import com.mediaxa.business.suite.data.sync.ConflictWinner
import com.mediaxa.business.suite.data.remote.dto.TransactionDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests multi-device synchronization scenarios:
 * - Two devices creating data for the same store
 * - Conflict resolution when both devices update the same record
 * - UUIDs ensure no collisions between devices
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiDeviceSyncTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun makeDto(uuid: String, deviceId: String, updatedAt: Long): TransactionDto {
        return TransactionDto(
            uuid = uuid,
            storeId = 1L,
            deviceId = deviceId,
            transactionNumber = "TRX-${uuid.takeLast(4)}",
            timestamp = updatedAt,
            cashierUuid = "cashier-$deviceId",
            cashierName = "Kasir $deviceId",
            discount = 0.0,
            subtotal = 50000.0,
            total = 50000.0,
            transactionHpp = 20000.0,
            grossProfit = 30000.0,
            paymentMethod = "CASH",
            amountReceived = 50000.0,
            changeAmount = 0.0,
            status = "COMPLETED",
            updatedAt = updatedAt
        )
    }

    @Test
    fun `two devices create different transactions - both sync successfully`() = runTest {
        // Simulate Device A and Device B each creating a unique transaction
        val device1Uuid = "txn-device-a-uuid"
        val device2Uuid = "txn-device-b-uuid"

        val payloadA = Json.encodeToString(makeDto(device1Uuid, "DEV-A", 1000L))
        val payloadB = Json.encodeToString(makeDto(device2Uuid, "DEV-B", 1001L))

        val items = mutableListOf(
            SyncQueueItem(
                localId = 1L,
                uuid = device1Uuid,
                storeId = 1L,
                deviceId = "DEV-A",
                entityType = SyncEntityType.TRANSACTION.name,
                operation = SyncOperation.CREATE.name,
                payload = payloadA,
                status = SyncQueueStatus.PENDING.name,
                nextRetryAt = 0L,
                createdAt = System.currentTimeMillis()
            ),
            SyncQueueItem(
                localId = 2L,
                uuid = device2Uuid,
                storeId = 1L,
                deviceId = "DEV-B",
                entityType = SyncEntityType.TRANSACTION.name,
                operation = SyncOperation.CREATE.name,
                payload = payloadB,
                status = SyncQueueStatus.PENDING.name,
                nextRetryAt = 0L,
                createdAt = System.currentTimeMillis()
            )
        )

        val fakeDao = createFakeDao(items)
        val remote = MockRemoteDataSourceImpl(failureProbability = 0.0, simulatedDelayMs = 1L..1L)
        val engine = SyncEngine(fakeDao, remote)

        val result = engine.processQueue()

        assertEquals(2, result.processedCount)
        assertEquals(2, result.successCount)
        assertFalse(result.hasFailures)
    }

    @Test
    fun `conflict resolution - device A wins when its updatedAt is newer`() {
        val deviceAUpdatedAt = 2000L
        val deviceBUpdatedAt = 1000L

        val winner = ConflictResolver.resolve(
            localUpdatedAt = deviceAUpdatedAt,
            remoteUpdatedAt = deviceBUpdatedAt
        )
        assertEquals(ConflictWinner.LOCAL, winner)
    }

    @Test
    fun `conflict resolution - server wins when server timestamp is newer`() {
        val localUpdatedAt = 1000L
        val serverUpdatedAt = 2000L

        val winner = ConflictResolver.resolve(
            localUpdatedAt = localUpdatedAt,
            remoteUpdatedAt = serverUpdatedAt
        )
        assertEquals(ConflictWinner.REMOTE, winner)
    }

    @Test
    fun `two devices update same record - newer updatedAt wins`() {
        // Both devices update customer-uuid-1, Device B has a newer timestamp
        val deviceATime = 1000L
        val deviceBTime = 1500L // Device B's change is 500ms newer

        val winner = ConflictResolver.resolve(deviceATime, deviceBTime)
        assertEquals("Server (Device B) should win with newer timestamp",
            ConflictWinner.REMOTE, winner)
    }

    @Test
    fun `device IDs are unique and distinguishable from each other`() {
        // Simulate two device IDs being different UUIDs
        val deviceIdA = "DEV-${java.util.UUID.randomUUID()}"
        val deviceIdB = "DEV-${java.util.UUID.randomUUID()}"

        assertNotEquals(deviceIdA, deviceIdB)
    }

    @Test
    fun `transaction UUIDs from two devices never collide`() {
        // Each device generates its own UUID — statistical impossibility of collision
        val uuidsDevice1 = (1..100).map { java.util.UUID.randomUUID().toString() }.toSet()
        val uuidsDevice2 = (1..100).map { java.util.UUID.randomUUID().toString() }.toSet()

        val collision = uuidsDevice1.intersect(uuidsDevice2)
        assertTrue("UUID collision detected between devices", collision.isEmpty())
    }

    // ─── Fake DAO helper ─────────────────────────────────────────────────────

    private fun createFakeDao(items: MutableList<SyncQueueItem>) = object : com.mediaxa.business.suite.data.local.dao.SyncQueueDao {
        override suspend fun enqueue(item: SyncQueueItem): Long { items.add(item); return items.size.toLong() }
        override suspend fun hasPendingMutation(uuid: String): Boolean {
            return items.any { it.uuid == uuid && it.status in listOf("PENDING", "IN_PROGRESS", "FAILED") }
        }
        override suspend fun update(item: SyncQueueItem) {}
        override suspend fun getPendingItems(now: Long, limit: Int) =
            items.filter { it.status == "PENDING" && it.nextRetryAt <= now }.take(limit)
        override suspend fun markInProgress(ids: List<Long>, now: Long) {
            ids.forEach { id -> val i = items.indexOfFirst { it.localId == id }; if (i >= 0) items[i] = items[i].copy(status = "IN_PROGRESS") }
        }
        override suspend fun markSynced(localId: Long) {
            val i = items.indexOfFirst { it.localId == localId }; if (i >= 0) items[i] = items[i].copy(status = "SYNCED")
        }
        override suspend fun incrementRetry(localId: Long, nextRetryAt: Long, errorMsg: String, now: Long) {}
        override suspend fun markFailed(localId: Long, errorMsg: String) {}
        override suspend fun cancelByEntityUuid(entityUuid: String, entityType: String) {}
        override fun observePendingCount() = flowOf(0)
        override fun observeFailedCount() = flowOf(0)
        override fun observeFailedItems() = flowOf(emptyList<SyncQueueItem>())
        override fun observePendingItems() = flowOf(emptyList<SyncQueueItem>())
        override fun observeLastSyncedAt() = flowOf(null)
        override fun observeTotalQueueSize() = flowOf(items.size)
        override suspend fun clearSyncedItems(olderThanMs: Long) {}
        override suspend fun recoverStuckItems() {}
        override suspend fun resetFailedItems() {}
        override suspend fun getSyncedCount() = items.count { it.status == "SYNCED" }
    }
}
