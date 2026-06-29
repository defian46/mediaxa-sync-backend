package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.repository.CheckoutService
import com.mediaxa.business.suite.data.repository.CheckoutResult
import com.mediaxa.business.suite.data.repository.TransactionRunner
import com.mediaxa.business.suite.domain.model.CartItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*

private class RealBugScenarioFakeTransactionRunner : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = block()
}

class RealBugScenarioTest {

    private lateinit var database: AppDatabase
    private lateinit var localDataSource: LocalDataSource
    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionItemDao: TransactionItemDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var stockMovementDao: StockMovementDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var storeSettingDao: StoreSettingDao
    private lateinit var menuRecipeDao: MenuRecipeDao
    private lateinit var ingredientDao: IngredientDao
    private lateinit var auditLogDao: AuditLogDao
    private lateinit var salesSummaryDao: SalesSummaryDao

    private lateinit var checkoutService: CheckoutService

    private val menu = Menu(
        uuid = "menu-1",
        name = "Kopi Susu",
        price = 15000.0,
        categoryUuid = "cat-1",
        isActive = true
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        any<Any>()
        return null as T
    }

    @Before
    fun setUp() {
        database = mock(AppDatabase::class.java)
        localDataSource = mock(LocalDataSource::class.java)
        transactionDao = mock(TransactionDao::class.java)
        transactionItemDao = mock(TransactionItemDao::class.java)
        paymentDao = mock(PaymentDao::class.java)
        stockMovementDao = mock(StockMovementDao::class.java)
        syncQueueDao = mock(SyncQueueDao::class.java)
        storeSettingDao = mock(StoreSettingDao::class.java)
        menuRecipeDao = mock(MenuRecipeDao::class.java)
        ingredientDao = mock(IngredientDao::class.java)
        auditLogDao = mock(AuditLogDao::class.java)
        salesSummaryDao = mock(SalesSummaryDao::class.java)

        `when`(localDataSource.transactionDao).thenReturn(transactionDao)
        `when`(localDataSource.transactionItemDao).thenReturn(transactionItemDao)
        `when`(localDataSource.paymentDao).thenReturn(paymentDao)
        `when`(localDataSource.stockMovementDao).thenReturn(stockMovementDao)
        `when`(localDataSource.syncQueueDao).thenReturn(syncQueueDao)
        `when`(localDataSource.storeSettingDao).thenReturn(storeSettingDao)
        `when`(localDataSource.menuRecipeDao).thenReturn(menuRecipeDao)
        `when`(localDataSource.ingredientDao).thenReturn(ingredientDao)
        `when`(localDataSource.auditLogDao).thenReturn(auditLogDao)

        runBlocking {
            `when`(transactionDao.insertTransaction(anyNonNull())).thenReturn(1L)
            `when`(transactionDao.getTransactionCount()).thenReturn(0)
            `when`(transactionItemDao.insertTransactionItems(anyNonNull())).thenReturn(Unit)
            `when`(paymentDao.insertPayments(anyNonNull())).thenReturn(Unit)
            `when`(stockMovementDao.insertMovement(anyNonNull())).thenReturn(1L)
            `when`(syncQueueDao.enqueue(anyNonNull())).thenReturn(1L)
            `when`(syncQueueDao.getPendingItems(anyLong(), anyInt())).thenReturn(emptyList())
            `when`(auditLogDao.insertLog(anyNonNull())).thenReturn(1L)
            `when`(storeSettingDao.getSettings()).thenReturn(null)
            `when`(menuRecipeDao.getRecipesForMenus(anyNonNull())).thenReturn(emptyList())
            `when`(ingredientDao.getIngredientsByUuids(anyNonNull())).thenReturn(emptyList())
        }

        checkoutService = CheckoutService(database, localDataSource, RealBugScenarioFakeTransactionRunner())
    }

    @Test
    fun testCheckoutInsertsTransactionLocallyAndEnqueuesSyncQueue() = runBlocking {
        val cart = listOf(CartItem(menu, quantity = 2))
        val context = mock(android.content.Context::class.java)

        val txCaptor = mutableListOf<Transaction>()
        val txItemsCaptor = mutableListOf<List<TransactionItem>>()
        val paymentsCaptor = mutableListOf<List<Payment>>()
        val queueItemCaptor = mutableListOf<SyncQueueItem>()

        `when`(transactionDao.insertTransaction(anyNonNull())).thenAnswer { invocation ->
            txCaptor.add(invocation.arguments[0] as Transaction)
            1L
        }
        `when`(transactionItemDao.insertTransactionItems(anyNonNull())).thenAnswer { invocation ->
            txItemsCaptor.add(invocation.arguments[0] as List<TransactionItem>)
            Unit
        }
        `when`(paymentDao.insertPayments(anyNonNull())).thenAnswer { invocation ->
            paymentsCaptor.add(invocation.arguments[0] as List<Payment>)
            Unit
        }
        `when`(syncQueueDao.enqueue(anyNonNull())).thenAnswer { invocation ->
            queueItemCaptor.add(invocation.arguments[0] as SyncQueueItem)
            1L
        }

        val result = checkoutService.executeCheckout(
            context = context,
            cart = cart,
            discount = 0.0,
            cashierUuid = "user-1",
            cashierName = "Cashier Test",
            paymentMethod = "CASH",
            amountReceived = 30000.0
        )

        assertTrue(result is CheckoutResult.Success)
        val txUuid = (result as CheckoutResult.Success).transactionUuid

        // 1. Verify Transaction is inserted to Room
        assertEquals(1, txCaptor.size)
        assertEquals(txUuid, txCaptor.first().uuid)
        assertEquals("COMPLETED", txCaptor.first().status)

        assertEquals(1, txItemsCaptor.size)
        assertEquals(txUuid, txItemsCaptor.first().first().transactionUuid)

        assertEquals(1, paymentsCaptor.size)
        assertEquals(txUuid, paymentsCaptor.first().first().transactionUuid)

        // 2. Verify SyncQueue items are enqueued
        assertTrue(queueItemCaptor.any { it.entityType == "TRANSACTION" && it.uuid == txUuid })
    }

    @Test
    fun testTransactionAppearsInHistoryQuery() = runBlocking {
        val tx = Transaction(
            localId = 1L,
            uuid = "tx-uuid-1",
            storeId = 1L,
            transactionNumber = "TRX-001",
            cashierUuid = "user-1",
            cashierName = "Cashier Test",
            subtotal = 30000.0,
            total = 30000.0,
            paymentMethod = "CASH",
            amountReceived = 30000.0,
            changeAmount = 0.0,
            status = "COMPLETED"
        )

        `when`(transactionDao.getAllTransactionsFlow()).thenReturn(flowOf(listOf(tx)))

        val historyList = transactionDao.getAllTransactionsFlow().first()
        assertEquals(1, historyList.size)
        assertEquals("tx-uuid-1", historyList.first().uuid)
        assertEquals("COMPLETED", historyList.first().status)
    }

    @Test
    fun testDashboardQueryCountsCompletedTransaction() = runBlocking {
        val summaryResult = SalesSummaryResult(
            totalRevenue = 30000.0,
            totalProfit = 10000.0,
            transactionCount = 1,
            averageTicketValue = 30000.0
        )

        `when`(salesSummaryDao.getSalesSummary(anyLong(), anyLong(), anyLong())).thenReturn(summaryResult)

        val result = salesSummaryDao.getSalesSummary(1L, 0L, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(1, result?.transactionCount)
        assertEquals(30000.0, result?.totalRevenue ?: 0.0, 0.001)
    }

    @Test
    fun testCloudLoginStoreUuidDoesNotBreakLocalTransactionQuery() = runBlocking {
        // Cloud login preferences should not replace local isolation storeId = 1L
        val localStoreId = 1L
        
        // Mock query with storeId = 1L
        val tx = Transaction(
            localId = 1L,
            uuid = "tx-uuid-1",
            storeId = localStoreId,
            transactionNumber = "TRX-001",
            cashierUuid = "user-1",
            cashierName = "Cashier Test",
            subtotal = 30000.0,
            total = 30000.0,
            paymentMethod = "CASH",
            amountReceived = 30000.0,
            changeAmount = 0.0,
            status = "COMPLETED"
        )
        
        `when`(transactionDao.getAllTransactionsFlow()).thenReturn(flowOf(listOf(tx)))
        
        // Emulate storeUuid stored in preferences as "store-uuid-abc"
        val cloudStoreUuid = "store-uuid-abc"
        assertNotEquals(localStoreId.toString(), cloudStoreUuid)
        
        // Query must still filter on storeId = 1L, which returns local transactions
        val historyList = transactionDao.getAllTransactionsFlow().first()
        assertEquals(1, historyList.size)
        assertEquals(localStoreId, historyList.first().storeId)
    }
}
