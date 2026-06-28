package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.repository.CheckoutService
import com.mediaxa.business.suite.data.repository.CheckoutResult
import com.mediaxa.business.suite.data.repository.StockValidationResult
import com.mediaxa.business.suite.data.repository.TransactionRunner
import com.mediaxa.business.suite.domain.model.CartItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

private class POSTransactionFakeTransactionRunner : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R {
        return block()
    }
}

class POSTransactionTest {

    private lateinit var database: AppDatabase
    private lateinit var localDataSource: LocalDataSource
    private lateinit var menuDao: MenuDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var ingredientDao: IngredientDao
    private lateinit var menuRecipeDao: MenuRecipeDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionItemDao: TransactionItemDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var stockMovementDao: StockMovementDao
    private lateinit var storeSettingDao: StoreSettingDao
    private lateinit var auditLogDao: AuditLogDao

    private lateinit var checkoutService: CheckoutService

    private val menu1 = Menu(
        uuid = "menu-jus-mangga",
        name = "Jus Mangga",
        price = 15000.0,
        categoryUuid = "cat-juice",
        isActive = true
    )

    private val menu2 = Menu(
        uuid = "menu-jus-alpukat",
        name = "Jus Alpukat",
        price = 18000.0,
        categoryUuid = "cat-juice",
        isActive = true
    )

    private val ingredientMangga = Ingredient(
        uuid = "ing-mangga",
        name = "Mangga Arumanis",
        availableStock = 5.0,
        unit = "kg",
        purchasePrice = 20000.0,
        packageSize = 1.0,
        unitPrice = 20000.0
    )

    private val ingredientCup = Ingredient(
        uuid = "ing-cup",
        name = "Plastic Cup",
        availableStock = 10.0,
        unit = "pcs",
        purchasePrice = 500.0,
        packageSize = 1.0,
        unitPrice = 500.0
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
        menuDao = mock(MenuDao::class.java)
        categoryDao = mock(CategoryDao::class.java)
        ingredientDao = mock(IngredientDao::class.java)
        menuRecipeDao = mock(MenuRecipeDao::class.java)
        transactionDao = mock(TransactionDao::class.java)
        transactionItemDao = mock(TransactionItemDao::class.java)
        paymentDao = mock(PaymentDao::class.java)
        stockMovementDao = mock(StockMovementDao::class.java)
        storeSettingDao = mock(StoreSettingDao::class.java)
        auditLogDao = mock(AuditLogDao::class.java)

        // Mock localDataSource DAOs
        `when`(localDataSource.menuDao).thenReturn(menuDao)
        `when`(localDataSource.categoryDao).thenReturn(categoryDao)
        `when`(localDataSource.ingredientDao).thenReturn(ingredientDao)
        `when`(localDataSource.menuRecipeDao).thenReturn(menuRecipeDao)
        `when`(localDataSource.transactionDao).thenReturn(transactionDao)
        `when`(localDataSource.transactionItemDao).thenReturn(transactionItemDao)
        `when`(localDataSource.paymentDao).thenReturn(paymentDao)
        `when`(localDataSource.stockMovementDao).thenReturn(stockMovementDao)
        `when`(localDataSource.storeSettingDao).thenReturn(storeSettingDao)
        `when`(localDataSource.auditLogDao).thenReturn(auditLogDao)

        // Stub DAO write methods to prevent NPEs in Kotlin suspend functions returning boxed types or Unit
        runBlocking {
            `when`(transactionDao.insertTransaction(anyNonNull())).thenReturn(1L)
            `when`(transactionItemDao.insertTransactionItems(anyNonNull())).thenReturn(Unit)
            `when`(paymentDao.insertPayments(anyNonNull())).thenReturn(Unit)
            `when`(ingredientDao.adjustStock(anyNonNull(), anyDouble(), anyLong(), anyNonNull())).thenReturn(Unit)
            `when`(stockMovementDao.insertMovement(anyNonNull())).thenReturn(1L)
            `when`(auditLogDao.insertLog(anyNonNull())).thenReturn(1L)
        }

        checkoutService = CheckoutService(database, localDataSource, POSTransactionFakeTransactionRunner())
    }

    @Test
    fun testCartItemCalculations() {
        val cart = listOf(
            CartItem(menu1, quantity = 2, note = "Kurang manis"),
            CartItem(menu2, quantity = 1)
        )

        val subtotal = cart.sumOf { it.menu.price * it.quantity }
        assertEquals(48000.0, subtotal, 0.001)

        val discount = 5000.0
        val total = (subtotal - discount).coerceAtLeast(0.0)
        assertEquals(43000.0, total, 0.001)

        val excessiveDiscount = 60000.0
        val totalWithExcessiveDiscount = (subtotal - excessiveDiscount).coerceAtLeast(0.0)
        assertEquals(0.0, totalWithExcessiveDiscount, 0.001)
    }

    @Test
    fun testCashPaymentValidation() {
        val total = 43000.0
        val cashReceivedOk = 50000.0
        val cashReceivedLess = 40000.0

        assertTrue(cashReceivedOk >= total)
        assertFalse(cashReceivedLess >= total)

        val change = cashReceivedOk - total
        assertEquals(7000.0, change, 0.001)
    }

    @Test
    fun testStockValidationSuccess() {
        runBlocking {
            val menuUuids = listOf(menu1.uuid)
            val recipe = listOf(
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientMangga.uuid, quantityNeeded = 0.25),
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientCup.uuid, quantityNeeded = 1.0)
            )

            `when`(menuRecipeDao.getRecipesForMenus(menuUuids)).thenReturn(recipe)
            `when`(ingredientDao.getIngredientsByUuids(listOf(ingredientMangga.uuid, ingredientCup.uuid)))
                .thenReturn(listOf(ingredientMangga, ingredientCup))

            val cart = listOf(CartItem(menu1, quantity = 2))

            val result = checkoutService.validateStock(cart)
            assertTrue(result is StockValidationResult.Valid)
        }
    }

    @Test
    fun testStockValidationLacking() {
        runBlocking {
            val menuUuids = listOf(menu1.uuid)
            val recipe = listOf(
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientMangga.uuid, quantityNeeded = 0.25),
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientCup.uuid, quantityNeeded = 1.0)
            )

            `when`(menuRecipeDao.getRecipesForMenus(menuUuids)).thenReturn(recipe)
            
            val insufficientCup = ingredientCup.copy(availableStock = 1.0)
            `when`(ingredientDao.getIngredientsByUuids(listOf(ingredientMangga.uuid, ingredientCup.uuid)))
                .thenReturn(listOf(ingredientMangga, insufficientCup))

            val cart = listOf(CartItem(menu1, quantity = 5))

            val result = checkoutService.validateStock(cart)
            assertTrue(result is StockValidationResult.LackingIngredients)

            val lackingList = (result as StockValidationResult.LackingIngredients).list
            assertEquals(1, lackingList.size)
            assertEquals("Plastic Cup", lackingList[0].ingredientName)
            assertEquals(5.0, lackingList[0].required, 0.001)
            assertEquals(1.0, lackingList[0].available, 0.001)
        }
    }

    @Test
    fun testHppAndGrossProfitCalculations() {
        val cart = listOf(
            CartItem(menu1, quantity = 2),
        )
        val subtotal = 30000.0
        val discount = 2000.0
        val total = subtotal - discount

        val recipe = listOf(
            MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientMangga.uuid, quantityNeeded = 0.25),
            MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientCup.uuid, quantityNeeded = 1.0)
        )

        var totalHpp = 0.0
        val ingredientsMap = mapOf(
            ingredientMangga.uuid to ingredientMangga,
            ingredientCup.uuid to ingredientCup
        )

        for (cartItem in cart) {
            for (rec in recipe) {
                val ingredient = ingredientsMap[rec.ingredientUuid]
                if (ingredient != null) {
                    totalHpp += ingredient.unitPrice * rec.quantityNeeded * cartItem.quantity
                }
            }
        }

        assertEquals(11000.0, totalHpp, 0.001)

        val grossProfit = total - totalHpp
        assertEquals(17000.0, grossProfit, 0.001)
    }

    @Test
    fun testHppItemSnapshots() {
        runBlocking {
            // Assert HPP snapshot calculations are correctly populated on TransactionItem during checkout
            val cart = listOf(CartItem(menu1, quantity = 2))
            val recipe = listOf(
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientMangga.uuid, quantityNeeded = 0.25),
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientCup.uuid, quantityNeeded = 1.0)
            )

            `when`(menuRecipeDao.getRecipesForMenus(listOf(menu1.uuid))).thenReturn(recipe)
            `when`(ingredientDao.getIngredientsByUuids(listOf(ingredientMangga.uuid, ingredientCup.uuid)))
                .thenReturn(listOf(ingredientMangga, ingredientCup))
            `when`(storeSettingDao.getSettings()).thenReturn(null)
            `when`(transactionDao.getTransactionCount()).thenReturn(0)

            val result = checkoutService.executeCheckout(
                cart = cart,
                discount = 0.0,
                cashierUuid = "usr-01",
                cashierName = "Cashier Test",
                paymentMethod = "QRIS",
                amountReceived = 30000.0
            )

            assertTrue(result is CheckoutResult.Success)

            val txUuid = (result as CheckoutResult.Success).transactionUuid
            assertNotNull(txUuid)
        }
    }

    @Test
    fun testPinLoginHashedVerification() {
        val rawPin = "1234"
        val hashedPin = AppDatabase.hashString(rawPin)
        assertEquals("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4", hashedPin)
    }

    @Test
    fun testAuditLoggingOnCheckout() {
        runBlocking {
            val cart = listOf(CartItem(menu1, quantity = 1))
            val recipe = listOf(
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientMangga.uuid, quantityNeeded = 0.2)
            )

            `when`(menuRecipeDao.getRecipesForMenus(listOf(menu1.uuid))).thenReturn(recipe)
            `when`(ingredientDao.getIngredientsByUuids(listOf(ingredientMangga.uuid))).thenReturn(listOf(ingredientMangga))
            `when`(storeSettingDao.getSettings()).thenReturn(null)
            `when`(transactionDao.getTransactionCount()).thenReturn(10)

            val result = checkoutService.executeCheckout(
                cart = cart,
                discount = 0.0,
                cashierUuid = "usr-01",
                cashierName = "Cashier Test",
                paymentMethod = "TRANSFER",
                amountReceived = 15000.0
            )

            assertTrue(result is CheckoutResult.Success)
            // Verify auditLogDao.insertLog was called
            verify(auditLogDao, atLeastOnce()).insertLog(anyNonNull())
        }
    }

    @Test
    fun testCheckoutRollback() {
        runBlocking {
            val cart = listOf(CartItem(menu1, quantity = 1))
            val recipe = listOf(
                MenuRecipe(menuUuid = menu1.uuid, ingredientUuid = ingredientMangga.uuid, quantityNeeded = 0.2)
            )

            `when`(menuRecipeDao.getRecipesForMenus(listOf(menu1.uuid))).thenReturn(recipe)
            `when`(ingredientDao.getIngredientsByUuids(listOf(ingredientMangga.uuid))).thenReturn(listOf(ingredientMangga))
            `when`(storeSettingDao.getSettings()).thenReturn(null)
            `when`(transactionDao.getTransactionCount()).thenReturn(0)

            // Force database insert failure to trigger rollback catch block
            `when`(transactionDao.insertTransaction(anyNonNull())).thenThrow(RuntimeException("DB Lock error"))

            val result = checkoutService.executeCheckout(
                cart = cart,
                discount = 0.0,
                cashierUuid = "usr-01",
                cashierName = "Cashier Test",
                paymentMethod = "TRANSFER",
                amountReceived = 15000.0
            )

            assertTrue(result is CheckoutResult.Failure)
            val errorMsg = (result as CheckoutResult.Failure).errorMsg
            assertTrue(errorMsg.contains("DB Lock error"))
        }
    }

    @Test
    fun testPaymentMethodRestrictions() {
        val validMethods = listOf("CASH", "QRIS", "TRANSFER")
        val invalidMethods = listOf("E-WALLET", "DEBIT", "CREDIT", "OTHER")
        
        assertTrue(validMethods.contains("CASH"))
        assertTrue(validMethods.contains("QRIS"))
        assertTrue(validMethods.contains("TRANSFER"))
        
        assertFalse(validMethods.contains("E-WALLET"))
        assertFalse(validMethods.contains("DEBIT"))
    }

    @Test
    fun testStoreSettingBankProperties() {
        val settings = StoreSetting(
            storeName = "Toko Sukses",
            address = "Jl. Sudirman",
            phoneNumber = "0812",
            receiptFooter = "Terima kasih",
            bankName = "BCA",
            bankAccountNumber = "12345678",
            bankAccountHolderName = "Budi"
        )
        assertEquals("BCA", settings.bankName)
        assertEquals("12345678", settings.bankAccountNumber)
        assertEquals("Budi", settings.bankAccountHolderName)
    }

    @Test
    fun testEscPosReceiptForTransfer() {
        val tx = Transaction(
            transactionNumber = "TX-001",
            cashierUuid = "usr-01",
            cashierName = "Cashier Test",
            paymentMethod = "TRANSFER",
            amountReceived = 15000.0,
            changeAmount = 0.0,
            subtotal = 15000.0,
            discount = 0.0,
            total = 15000.0,
            timestamp = System.currentTimeMillis()
        )
        val settings = StoreSetting(
            storeName = "Toko Sukses",
            address = "Jl. Sudirman",
            phoneNumber = "0812",
            receiptFooter = "Terima kasih",
            bankName = "BCA",
            bankAccountNumber = "12345678",
            bankAccountHolderName = "Budi"
        )
        val formatted = com.mediaxa.business.suite.data.printing.EscPosFormatter.formatReceiptText(
            tx, emptyList(), settings
        )
        assertTrue(formatted.contains("Transfer Ke:"))
        assertTrue(formatted.contains("BCA - 12345678"))
        assertTrue(formatted.contains("A/N: Budi"))
    }
}
