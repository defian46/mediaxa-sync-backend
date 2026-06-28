package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.repository.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

private class InventoryLiteFakeTransactionRunner : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R {
        return block()
    }
}

// --- FAKE DAOS TO AVOID MOCKITO COROUTINE/SUSPEND PROBLEMS ---

private class FakeIngredientDao(initialIngredients: List<Ingredient>) : IngredientDao {
    val ingredients = initialIngredients.associateBy { it.uuid }.toMutableMap()
    val updatedIngredients = mutableListOf<Ingredient>()
    
    override suspend fun getIngredientByUuid(uuid: String): Ingredient? {
        return ingredients[uuid]
    }
    
    override suspend fun updateIngredient(ingredient: Ingredient) {
        ingredients[ingredient.uuid] = ingredient
        updatedIngredients.add(ingredient)
    }

    override fun getAllIngredientsFlow(): kotlinx.coroutines.flow.Flow<List<Ingredient>> = TODO()
    override fun getActiveIngredientsFlow(): kotlinx.coroutines.flow.Flow<List<Ingredient>> = TODO()
    override suspend fun getIngredientsByUuids(uuids: List<String>): List<Ingredient> = TODO()
    override suspend fun getUnsyncedIngredients(): List<Ingredient> = TODO()
    override suspend fun insertIngredient(ingredient: Ingredient): Long = TODO()
    override suspend fun adjustStock(uuid: String, amount: Double, updatedAt: Long, syncStatus: String) = TODO()
    override suspend fun getIngredientVelocities(storeId: Long, thirtyDaysAgo: Long): List<IngredientVelocityResult> = TODO()
}

private class FakeStockMovementDao : StockMovementDao {
    val movements = mutableListOf<StockMovement>()
    
    override suspend fun insertMovement(movement: StockMovement): Long {
        movements.add(movement)
        return movements.size.toLong()
    }
    
    override fun getAllStockMovementsFlow(): kotlinx.coroutines.flow.Flow<List<StockMovement>> = TODO()
    override fun getStockMovementsByIngredientFlow(ingredientUuid: String): kotlinx.coroutines.flow.Flow<List<StockMovement>> = TODO()
    override suspend fun getUnsyncedStockMovements(): List<StockMovement> = TODO()
}

private class FakeInventoryLiteDao : InventoryLiteDao {
    val purchaseExpenses = mutableListOf<PurchaseExpense>()
    val purchaseItems = mutableListOf<PurchaseExpenseItem>()
    val expenses = mutableListOf<Expense>()
    val stockOpnames = mutableListOf<StockOpname>()
    val stockOpnameItems = mutableListOf<StockOpnameItem>()
    val wasteLogs = mutableListOf<WasteLog>()
    
    override suspend fun insertPurchaseExpense(expense: PurchaseExpense): Long {
        purchaseExpenses.add(expense)
        return purchaseExpenses.size.toLong()
    }
    
    override suspend fun insertPurchaseExpenseItems(items: List<PurchaseExpenseItem>) {
        purchaseItems.addAll(items)
    }
    
    override suspend fun insertExpense(expense: Expense): Long {
        expenses.add(expense)
        return expenses.size.toLong()
    }
    
    override suspend fun insertStockOpname(opname: StockOpname): Long {
        stockOpnames.add(opname)
        return stockOpnames.size.toLong()
    }
    
    override suspend fun insertStockOpnameItems(items: List<StockOpnameItem>) {
        stockOpnameItems.addAll(items)
    }
    
    override suspend fun insertWasteLog(log: WasteLog): Long {
        wasteLogs.add(log)
        return wasteLogs.size.toLong()
    }

    override fun getAllExpensesFlow(storeId: Long): kotlinx.coroutines.flow.Flow<List<Expense>> = TODO()
    override suspend fun getExpensesInPeriod(storeId: Long, startDate: Long, endDate: Long): List<Expense> = TODO()
    override fun getAllPurchaseExpensesFlow(storeId: Long): kotlinx.coroutines.flow.Flow<List<PurchaseExpense>> = TODO()
    override fun getAllStockOpnamesFlow(storeId: Long): kotlinx.coroutines.flow.Flow<List<StockOpname>> = TODO()
    override fun getAllWasteLogsFlow(storeId: Long): kotlinx.coroutines.flow.Flow<List<WasteLog>> = TODO()
    override suspend fun getWasteLogsInPeriod(storeId: Long, startDate: Long, endDate: Long): List<WasteLog> = TODO()
    override suspend fun getTotalStockAssetValue(storeId: Long): Double? = TODO()
    override suspend fun getLowStockCount(storeId: Long): Int = TODO()
    override suspend fun getOutOfStockCount(storeId: Long): Int = TODO()
    override suspend fun getTopConsumedIngredients(storeId: Long, startDate: Long, endDate: Long, limit: Int): List<IngredientUsageResult> = TODO()
    override suspend fun getTopPurchasedIngredients(storeId: Long, startDate: Long, endDate: Long, limit: Int): List<IngredientUsageResult> = TODO()
}

private class FakeUserDao : UserDao {
    var user: User? = null

    override suspend fun getUserByUsername(username: String): User? = user
    override suspend fun getUserByUuid(uuid: String): User? = user
    override suspend fun getUserByPin(hashedPin: String): User? = user
    override fun getAllUsersFlow(): kotlinx.coroutines.flow.Flow<List<User>> = TODO()
    override suspend fun getUnsyncedUsers(): List<User> = TODO()
    override suspend fun insertUser(user: User): Long {
        this.user = user
        return 1L
    }
    override suspend fun updateUser(user: User) {
        this.user = user
    }
}

private class FakeAuditLogDao : AuditLogDao {
    val logs = mutableListOf<AuditLog>()

    override fun getAllLogsFlow(): kotlinx.coroutines.flow.Flow<List<AuditLog>> = TODO()
    override suspend fun insertLog(log: AuditLog): Long {
        logs.add(log)
        return logs.size.toLong()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryLiteTest {

    private lateinit var database: AppDatabase
    private lateinit var localDataSource: LocalDataSource
    
    private lateinit var fakeIngredientDao: FakeIngredientDao
    private lateinit var fakeStockMovementDao: FakeStockMovementDao
    private lateinit var fakeInventoryLiteDao: FakeInventoryLiteDao
    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao

    private lateinit var purchaseRepository: PurchaseExpenseRepository
    private lateinit var stockOpnameRepository: StockOpnameRepository
    private lateinit var wasteRepository: WasteRepository

    private val ingredientSusu = Ingredient(
        uuid = "ing-susu",
        name = "Susu UHT",
        availableStock = 5.0,
        unit = "liter",
        purchasePrice = 15000.0,
        packageSize = 1.0,
        unitPrice = 15000.0
    )

    @Before
    fun setUp() {
        fakeIngredientDao = FakeIngredientDao(listOf(ingredientSusu))
        fakeStockMovementDao = FakeStockMovementDao()
        fakeInventoryLiteDao = FakeInventoryLiteDao()
        fakeUserDao = FakeUserDao().apply {
            user = User(username = "admin", passwordHash = "", pin = "", role = "ADMIN")
        }
        fakeAuditLogDao = FakeAuditLogDao()

        database = mock(AppDatabase::class.java)
        `when`(database.userDao()).thenReturn(fakeUserDao)
        `when`(database.storeSettingDao()).thenReturn(mock(StoreSettingDao::class.java))
        `when`(database.categoryDao()).thenReturn(mock(CategoryDao::class.java))
        `when`(database.menuDao()).thenReturn(mock(MenuDao::class.java))
        `when`(database.ingredientDao()).thenReturn(fakeIngredientDao)
        `when`(database.menuRecipeDao()).thenReturn(mock(MenuRecipeDao::class.java))
        `when`(database.stockMovementDao()).thenReturn(fakeStockMovementDao)
        `when`(database.transactionDao()).thenReturn(mock(TransactionDao::class.java))
        `when`(database.transactionItemDao()).thenReturn(mock(TransactionItemDao::class.java))
        `when`(database.paymentDao()).thenReturn(mock(PaymentDao::class.java))
        `when`(database.voidLogDao()).thenReturn(mock(VoidLogDao::class.java))
        `when`(database.backupLogDao()).thenReturn(mock(BackupLogDao::class.java))
        `when`(database.auditLogDao()).thenReturn(fakeAuditLogDao)
        `when`(database.salesSummaryDao()).thenReturn(mock(SalesSummaryDao::class.java))
        `when`(database.inventoryLiteDao()).thenReturn(fakeInventoryLiteDao)

        localDataSource = LocalDataSource(database)

        purchaseRepository = PurchaseExpenseRepository(database, localDataSource, InventoryLiteFakeTransactionRunner())
        stockOpnameRepository = StockOpnameRepository(database, localDataSource, InventoryLiteFakeTransactionRunner())
        wasteRepository = WasteRepository(database, localDataSource, InventoryLiteFakeTransactionRunner())
    }

    @Test
    fun testPurchaseExpenseIncreasesStockAndCreatesExpense() {
        runBlocking {
            val purchase = PurchaseExpense(
                purchaseDate = System.currentTimeMillis(),
                purchasePlaceName = "Toko Susu Sejahtera",
                paymentMethod = "CASH",
                notes = "Belanja bulanan susu",
                totalAmount = 30000.0
            )

            val purchaseItem = PurchaseExpenseItem(
                purchaseExpenseUuid = purchase.uuid,
                ingredientUuid = "ing-susu",
                quantity = 2.0,
                unit = "liter",
                totalPrice = 30000.0,
                unitPrice = 15000.0,
                batchNumber = "B-01",
                expiredDate = null
            )

            val success = purchaseRepository.recordPurchaseExpense(
                purchaseExpense = purchase,
                items = listOf(purchaseItem),
                userUuid = "user-admin",
                paymentMethod = "CASH"
            )

            assertTrue(success)

            // Verify stock increment on ingredient
            val updatedIng = fakeIngredientDao.updatedIngredients.firstOrNull()
            assertNotNull(updatedIng)
            assertEquals("ing-susu", updatedIng!!.uuid)
            assertEquals(7.0, updatedIng.availableStock, 0.0)
            assertEquals(15000.0, updatedIng.purchasePrice, 0.0)

            // Verify stock movement registration (type = STOCK_IN_PURCHASE)
            val movement = fakeStockMovementDao.movements.firstOrNull()
            assertNotNull(movement)
            assertEquals("ing-susu", movement!!.ingredientUuid)
            assertEquals(2.0, movement.quantity, 0.0)
            assertEquals("STOCK_IN_PURCHASE", movement.type)

            // Verify operational expense registration (category = INVENTORY_PURCHASE)
            val expense = fakeInventoryLiteDao.expenses.firstOrNull()
            assertNotNull(expense)
            assertEquals(ExpenseCategory.INVENTORY_PURCHASE.name, expense!!.category)
            assertEquals(30000.0, expense.amount, 0.0)

            // Verify audit log registration
            assertEquals(1, fakeAuditLogDao.logs.size)
        }
    }

    @Test
    fun testWasteDeductionSubsumesStockSuccessfully() {
        runBlocking {
            val waste = WasteLog(
                wasteDate = System.currentTimeMillis(),
                ingredientUuid = "ing-susu",
                quantity = 1.0,
                reason = "Kadaluarsa",
                calculatedCost = 0.0, // calculated dynamically in repo
                userUuid = "user-admin",
                notes = "Susu pecah"
            )

            val success = wasteRepository.recordWaste(waste, "user-admin")
            assertTrue(success)

            // Verify stock decrement (5.0 -> 4.0 liter)
            val updatedIng = fakeIngredientDao.updatedIngredients.firstOrNull()
            assertNotNull(updatedIng)
            assertEquals("ing-susu", updatedIng!!.uuid)
            assertEquals(4.0, updatedIng.availableStock, 0.0)

            // Verify stock movement registration (type = WASTE, qty = -1.0)
            val movement = fakeStockMovementDao.movements.firstOrNull()
            assertNotNull(movement)
            assertEquals("ing-susu", movement!!.ingredientUuid)
            assertEquals(-1.0, movement.quantity, 0.0)
            assertEquals("WASTE", movement.type)

            // Verify waste cost calculated (1.0 qty * 15000.0 price = 15000.0)
            val wasteLog = fakeInventoryLiteDao.wasteLogs.firstOrNull()
            assertNotNull(wasteLog)
            assertEquals("ing-susu", wasteLog!!.ingredientUuid)
            assertEquals(15000.0, wasteLog.calculatedCost, 0.0)

            // Verify audit log registration
            assertEquals(1, fakeAuditLogDao.logs.size)
        }
    }

    @Test
    fun testWasteRejectsInsufficiency() {
        runBlocking {
            val excessWaste = WasteLog(
                wasteDate = System.currentTimeMillis(),
                ingredientUuid = "ing-susu",
                quantity = 10.0, // exceeds available stock (5.0)
                reason = "Tumpah",
                calculatedCost = 0.0,
                userUuid = "user-admin",
                notes = "Semua tumpah"
            )

            val success = wasteRepository.recordWaste(excessWaste, "user-admin")
            assertFalse(success) // Should return false / fail catch block
        }
    }

    @Test
    fun testStockOpnameAdjustments() {
        runBlocking {
            val opname = StockOpname(
                opnameDate = System.currentTimeMillis(),
                userUuid = "user-admin",
                notes = "Opname bulanan"
            )

            // Physical count = 4.0 liter (diff = 4.0 - 5.0 = -1.0)
            val opnameItem = StockOpnameItem(
                opnameUuid = opname.uuid,
                ingredientUuid = "ing-susu",
                systemStock = 5.0,
                physicalStock = 4.0,
                diffStock = -1.0,
                notes = "Selisih 1L pecah"
            )

            val success = stockOpnameRepository.recordStockOpname(
                opname = opname,
                items = listOf(opnameItem),
                userUuid = "user-admin"
            )

            assertTrue(success)

            // Verify stock update to match physical count (4.0)
            val updatedIng = fakeIngredientDao.updatedIngredients.firstOrNull()
            assertNotNull(updatedIng)
            assertEquals("ing-susu", updatedIng!!.uuid)
            assertEquals(4.0, updatedIng.availableStock, 0.0)

            // Verify StockMovement registration (qty = -1.0, type = ADJUSTMENT)
            val movement = fakeStockMovementDao.movements.firstOrNull()
            assertNotNull(movement)
            assertEquals("ing-susu", movement!!.ingredientUuid)
            assertEquals(-1.0, movement.quantity, 0.0)
            assertEquals("ADJUSTMENT", movement.type)

            // Verify audit log registration
            assertEquals(1, fakeAuditLogDao.logs.size)
        }
    }

    @Test
    fun testGeneralExpenseCreatesAuditLog() {
        runBlocking {
            val expenseRepo = ExpenseRepository(database, localDataSource, InventoryLiteFakeTransactionRunner())
            val expense = Expense(
                expenseDate = System.currentTimeMillis(),
                category = "RENT",
                amount = 1500000.0,
                notes = "Sewa bulanan ruko",
                userUuid = "user-admin",
                paymentMethod = "TRANSFER"
            )
            val result = expenseRepo.insertExpense(expense)
            assertEquals(1, fakeAuditLogDao.logs.size)
        }
    }
}
