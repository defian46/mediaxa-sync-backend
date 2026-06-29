package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.repository.*
import com.mediaxa.business.suite.domain.model.CartItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class CustomerLoyaltyPromotionTest {

    private lateinit var database: AppDatabase
    private lateinit var localDataSource: LocalDataSource
    
    private lateinit var storeSettingDao: StoreSettingDao
    private lateinit var customerDao: CustomerDao
    private lateinit var loyaltyDao: LoyaltyDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionItemDao: TransactionItemDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var ingredientDao: IngredientDao
    private lateinit var stockMovementDao: StockMovementDao
    private lateinit var menuRecipeDao: MenuRecipeDao
    private lateinit var auditLogDao: AuditLogDao
    private lateinit var syncQueueDao: SyncQueueDao

    private lateinit var checkoutService: CheckoutService

    private class TestTransactionRunner : TransactionRunner {
        override suspend fun <R> run(block: suspend () -> R): R = block()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<Any>()
        return null as T
    }

    @Before
    fun setUp() {
        database = Mockito.mock(AppDatabase::class.java)
        localDataSource = Mockito.mock(LocalDataSource::class.java)
        
        storeSettingDao = Mockito.mock(StoreSettingDao::class.java)
        customerDao = Mockito.mock(CustomerDao::class.java)
        loyaltyDao = Mockito.mock(LoyaltyDao::class.java)
        transactionDao = Mockito.mock(TransactionDao::class.java)
        transactionItemDao = Mockito.mock(TransactionItemDao::class.java)
        paymentDao = Mockito.mock(PaymentDao::class.java)
        ingredientDao = Mockito.mock(IngredientDao::class.java)
        stockMovementDao = Mockito.mock(StockMovementDao::class.java)
        menuRecipeDao = Mockito.mock(MenuRecipeDao::class.java)
        auditLogDao = Mockito.mock(AuditLogDao::class.java)
        syncQueueDao = Mockito.mock(SyncQueueDao::class.java)

        // Bind mock DAOs to localDataSource getters
        Mockito.`when`(localDataSource.storeSettingDao).thenReturn(storeSettingDao)
        Mockito.`when`(localDataSource.customerDao).thenReturn(customerDao)
        Mockito.`when`(localDataSource.loyaltyDao).thenReturn(loyaltyDao)
        Mockito.`when`(localDataSource.transactionDao).thenReturn(transactionDao)
        Mockito.`when`(localDataSource.transactionItemDao).thenReturn(transactionItemDao)
        Mockito.`when`(localDataSource.paymentDao).thenReturn(paymentDao)
        Mockito.`when`(localDataSource.ingredientDao).thenReturn(ingredientDao)
        Mockito.`when`(localDataSource.stockMovementDao).thenReturn(stockMovementDao)
        Mockito.`when`(localDataSource.menuRecipeDao).thenReturn(menuRecipeDao)
        Mockito.`when`(localDataSource.auditLogDao).thenReturn(auditLogDao)
        Mockito.`when`(localDataSource.syncQueueDao).thenReturn(syncQueueDao)

        checkoutService = CheckoutService(database, localDataSource, TestTransactionRunner())

        // Default setting: earn 1 point per Rp 10.000, 1 point = Rp 100 discount
        val defaultSettings = StoreSetting(
            storeName = "Mediaxa Business Suite",
            address = "Street No 1",
            phoneNumber = "081",
            receiptFooter = "Footer",
            loyaltyPointsPerAmount = 10000.0,
            loyaltyPointsValue = 100.0
        )
        runBlocking {
            Mockito.`when`(storeSettingDao.getSettings()).thenReturn(defaultSettings)
            Mockito.`when`(menuRecipeDao.getRecipesForMenus(anyNonNull())).thenReturn(emptyList())
            Mockito.`when`(ingredientDao.getIngredientsByUuids(anyNonNull())).thenReturn(emptyList())
            Mockito.`when`(transactionDao.getTransactionCount()).thenReturn(0)
            Mockito.`when`(syncQueueDao.enqueue(anyNonNull())).thenReturn(1L)
            Mockito.`when`(syncQueueDao.getPendingItems(Mockito.anyLong(), Mockito.anyInt())).thenReturn(emptyList())
        }
    }

    @Test
    fun testCheckoutEarnsLoyaltyPoints() = runBlocking {
        val custUuid = "cust-1"
        val customer = Customer(
            uuid = custUuid,
            customerCode = "MBR-001",
            customerName = "Ahmad",
            phone = "0812",
            email = "ahmad@mail.com",
            birthday = null,
            gender = "MALE",
            address = "Jakarta",
            notes = "",
            totalSpending = 0.0
        )

        // Static stubs instead of matchers
        Mockito.`when`(customerDao.getCustomerByUuid(custUuid)).thenReturn(customer)
        Mockito.`when`(loyaltyDao.getPointsBalance(1L, custUuid)).thenReturn(5)

        val menu = Menu(uuid = "menu-1", name = "Coffee Latte", price = 25000.0, categoryUuid = "cat-1")
        val cart = listOf(CartItem(menu = menu, quantity = 2))

        val res = try {
            checkoutService.executeCheckout(
                context = Mockito.mock(android.content.Context::class.java),
                cart = cart,
                discount = 0.0,
                cashierUuid = "user-1",
                cashierName = "Cashier 1",
                paymentMethod = "CASH",
                amountReceived = 50000.0,
                storeId = 1L,
                customerUuid = custUuid
            )
        } catch (e: Exception) {
            println("Exception in testCheckoutEarnsLoyaltyPoints:")
            e.printStackTrace()
            throw e
        }

        if (res is CheckoutResult.Failure) {
            println("Checkout failed in testCheckoutEarnsLoyaltyPoints: ${res.errorMsg}")
        }
        assertTrue(res is CheckoutResult.Success)

        // Verify points earned
        val balance = loyaltyDao.getPointsBalance(1L, custUuid)
        assertEquals(5, balance)
    }

    @Test
    fun testCheckoutRedeemsLoyaltyPoints() = runBlocking {
        val custUuid = "cust-2"
        val customer = Customer(
            uuid = custUuid,
            customerCode = "MBR-002",
            customerName = "Budi",
            phone = "0813",
            email = "budi@mail.com",
            birthday = null,
            gender = "MALE",
            address = "Bandung",
            notes = "",
            totalSpending = 0.0
        )

        Mockito.`when`(customerDao.getCustomerByUuid(custUuid)).thenReturn(customer)
        // Sequential mock behavior: 20 points initial, then 14 points after redemption
        Mockito.`when`(loyaltyDao.getPointsBalance(1L, custUuid)).thenReturn(20).thenReturn(14)

        val menu = Menu(uuid = "menu-1", name = "Coffee Latte", price = 25000.0, categoryUuid = "cat-1")
        val cart = listOf(CartItem(menu = menu, quantity = 2))

        val res = try {
            checkoutService.executeCheckout(
                context = Mockito.mock(android.content.Context::class.java),
                cart = cart,
                discount = 0.0,
                cashierUuid = "user-1",
                cashierName = "Cashier 1",
                paymentMethod = "CASH",
                amountReceived = 49000.0, // 50.000 - 1.000 discount
                storeId = 1L,
                customerUuid = custUuid,
                pointsToRedeem = 10
            )
        } catch (e: Exception) {
            println("Exception in testCheckoutRedeemsLoyaltyPoints:")
            e.printStackTrace()
            throw e
        }

        if (res is CheckoutResult.Failure) {
            println("Checkout failed in testCheckoutRedeemsLoyaltyPoints: ${res.errorMsg}")
        }
        assertTrue(res is CheckoutResult.Success)

        val balance = loyaltyDao.getPointsBalance(1L, custUuid)
        assertEquals(14, balance)
    }

    @Test
    fun testCheckoutPreventsOverRedemption() = runBlocking {
        val custUuid = "cust-3"
        val customer = Customer(
            uuid = custUuid,
            customerCode = "MBR-003",
            customerName = "Citra",
            phone = "0814",
            email = "citra@mail.com",
            birthday = null,
            gender = "FEMALE",
            address = "Surabaya",
            notes = "",
            totalSpending = 0.0
        )

        Mockito.`when`(customerDao.getCustomerByUuid(custUuid)).thenReturn(customer)
        Mockito.`when`(loyaltyDao.getPointsBalance(1L, custUuid)).thenReturn(5)

        val menu = Menu(uuid = "menu-1", name = "Coffee Latte", price = 25000.0, categoryUuid = "cat-1")
        val cart = listOf(CartItem(menu = menu, quantity = 2))

        val res = try {
            checkoutService.executeCheckout(
                context = Mockito.mock(android.content.Context::class.java),
                cart = cart,
                discount = 0.0,
                cashierUuid = "user-1",
                cashierName = "Cashier 1",
                paymentMethod = "CASH",
                amountReceived = 50000.0,
                storeId = 1L,
                customerUuid = custUuid,
                pointsToRedeem = 10
            )
        } catch (e: Exception) {
            println("Exception in testCheckoutPreventsOverRedemption:")
            e.printStackTrace()
            throw e
        }

        assertTrue(res is CheckoutResult.Failure)
        assertTrue((res as CheckoutResult.Failure).errorMsg.contains("Poin tidak mencukupi"))
    }
}
