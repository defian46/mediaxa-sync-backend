package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

private class FakeFinanceDao : FinanceDao {
    var salesSummary = FinanceSalesSummaryResult(0.0, 0.0)
    var expenseSummary = ExpenseSummaryResult(0.0, 0.0)
    var wasteCost = 0.0
    val salesInflows = mutableListOf<PaymentMethodSummary>()
    val expenseOutflows = mutableListOf<PaymentMethodSummary>()
    val categoryExpenses = mutableListOf<CategoryExpenseResult>()
    val salesTrend = mutableListOf<DailySalesTrendResult>()

    override suspend fun getSalesSummary(storeId: Long, startDate: Long, endDate: Long): FinanceSalesSummaryResult = salesSummary
    override suspend fun getExpenseSummary(storeId: Long, startDate: Long, endDate: Long): ExpenseSummaryResult = expenseSummary
    override suspend fun getWasteCostSummary(storeId: Long, startDate: Long, endDate: Long): Double = wasteCost
    override suspend fun getSalesInflowByMethod(storeId: Long, startDate: Long, endDate: Long): List<PaymentMethodSummary> = salesInflows
    override suspend fun getExpenseOutflowByMethod(storeId: Long, startDate: Long, endDate: Long): List<PaymentMethodSummary> = expenseOutflows
    override suspend fun getExpensesByCategory(storeId: Long, startDate: Long, endDate: Long): List<CategoryExpenseResult> = categoryExpenses
    override suspend fun getDailySalesTrend(storeId: Long, startDate: Long, endDate: Long): List<DailySalesTrendResult> = salesTrend

    val dailyClosingsList = mutableListOf<DailyClosing>()
    val cashShiftsList = mutableListOf<CashShift>()
    var cashSalesDuringShift = 0.0
    var cashExpensesDuringShift = 0.0
    var transactionCount = 0

    override suspend fun insertDailyClosing(dailyClosing: DailyClosing): Long {
        dailyClosingsList.add(dailyClosing)
        return dailyClosing.localId.takeIf { it > 0 } ?: dailyClosingsList.size.toLong()
    }
    override suspend fun getDailyClosings(storeId: Long): List<DailyClosing> = dailyClosingsList.filter { it.storeId == storeId }
    override suspend fun getDailyClosingByDate(storeId: Long, dateStr: String): DailyClosing? =
        dailyClosingsList.firstOrNull { it.storeId == storeId && it.dateStr == dateStr }
    override suspend fun getLatestClosingBalance(storeId: Long): Double? =
        dailyClosingsList.filter { it.storeId == storeId }.maxByOrNull { it.dateStr }?.closingBalance

    override suspend fun insertCashShift(cashShift: CashShift): Long {
        cashShiftsList.add(cashShift)
        return cashShift.localId.takeIf { it > 0 } ?: cashShiftsList.size.toLong()
    }
    override suspend fun updateCashShift(cashShift: CashShift): Int {
        val idx = cashShiftsList.indexOfFirst { it.uuid == cashShift.uuid }
        if (idx >= 0) {
            cashShiftsList[idx] = cashShift
            return 1
        }
        return 0
    }
    override suspend fun getActiveCashShift(storeId: Long): CashShift? =
        cashShiftsList.firstOrNull { it.storeId == storeId && it.status == "ACTIVE" }
    override suspend fun getCashShifts(storeId: Long): List<CashShift> =
        cashShiftsList.filter { it.storeId == storeId }.sortedByDescending { it.startTime }

    override suspend fun getCashSalesDuringShift(storeId: Long, startTime: Long, endTime: Long): Double = cashSalesDuringShift
    override suspend fun getCashExpensesDuringShift(storeId: Long, startTime: Long, endTime: Long): Double = cashExpensesDuringShift
    override suspend fun getTransactionCountInRange(storeId: Long, startDate: Long, endDate: Long): Int = transactionCount
}

class FinanceTest {

    private lateinit var database: AppDatabase
    private lateinit var localDataSource: LocalDataSource
    private lateinit var fakeFinanceDao: FakeFinanceDao
    private lateinit var financeRepository: FinanceRepository

    @Before
    fun setUp() {
        fakeFinanceDao = FakeFinanceDao()
        database = mock(AppDatabase::class.java)
        `when`(database.financeDao()).thenReturn(fakeFinanceDao)

        localDataSource = LocalDataSource(database)
        financeRepository = FinanceRepository(localDataSource)
    }

    @Test
    fun testGrossProfitAndNetProfitCalculations() {
        runBlocking {
            // Set Sales: Revenue = 1.000.000, HPP = 400.000
            fakeFinanceDao.salesSummary = FinanceSalesSummaryResult(1000000.0, 400000.0)
            
            // Set Expenses: Operational = 150.000, Inventory Purchase = 300.000 (stock asset)
            fakeFinanceDao.expenseSummary = ExpenseSummaryResult(
                inventoryPurchase = 300000.0,
                operationalExpense = 150000.0
            )

            // Set Waste: 50.000
            fakeFinanceDao.wasteCost = 50000.0

            val summary = financeRepository.getProfitSummary(1L, 0L, System.currentTimeMillis())

            // 1. Gross Profit = Revenue - HPP = 1.000.000 - 400.000 = 600.000
            assertEquals(600000.0, summary.grossProfit, 0.0)

            // 2. Net Profit = Revenue - HPP - Operational - Waste = 1.000.000 - 400.000 - 150.000 - 50.000 = 400.000
            // Inventory Purchase (300.000) must be omitted (double count prevention)
            assertEquals(400000.0, summary.netProfit, 0.0)
            assertEquals(300000.0, summary.inventoryPurchase, 0.0)
            assertEquals(50000.0, summary.wasteCost, 0.0)
        }
    }

    @Test
    fun testCashFlowBalancesByPaymentMethod() {
        runBlocking {
            // Inflow: CASH = 500.000, QRIS = 300.000
            fakeFinanceDao.salesInflows.addAll(listOf(
                PaymentMethodSummary("CASH", 500000.0),
                PaymentMethodSummary("qris", 300000.0) // Lowercase to test uppercase standardization
            ))

            // Outflow: CASH = 100.000, TRANSFER = 200.000
            fakeFinanceDao.expenseOutflows.addAll(listOf(
                PaymentMethodSummary("CASH", 100000.0),
                PaymentMethodSummary("TRANSFER", 200000.0)
            ))

            val report = financeRepository.getCashFlow(1L, 0L, System.currentTimeMillis())

            // Total Inflow = 800.000
            assertEquals(800000.0, report.totalInflow, 0.0)

            // Total Outflow = 300.000
            assertEquals(300000.0, report.totalOutflow, 0.0)

            // Net Cash Flow = 500.000
            assertEquals(500000.0, report.netCashFlow, 0.0)

            // Cash balances map
            val balances = report.cashBalances
            assertNotNull(balances)

            // CASH: In 500.000 - Out 100.000 = 400.000
            val cashBalance = balances["CASH"]
            assertNotNull(cashBalance)
            assertEquals(500000.0, cashBalance!!.inflow, 0.0)
            assertEquals(100000.0, cashBalance.outflow, 0.0)
            assertEquals(400000.0, cashBalance.balance, 0.0)

            // QRIS: In 300.000 - Out 0.0 = 300.000
            val qrisBalance = balances["QRIS"]
            assertNotNull(qrisBalance)
            assertEquals(300000.0, qrisBalance!!.inflow, 0.0)
            assertEquals(0.0, qrisBalance.outflow, 0.0)
            assertEquals(300000.0, qrisBalance.balance, 0.0)

            // TRANSFER: In 0.0 - Out 200.000 = -200.000
            val transferBalance = balances["TRANSFER"]
            assertNotNull(transferBalance)
            assertEquals(0.0, transferBalance!!.inflow, 0.0)
            assertEquals(200000.0, transferBalance.outflow, 0.0)
            assertEquals(-200000.0, transferBalance.balance, 0.0)
        }
    }

    @Test
    fun testDailyClosingCalculation() {
        runBlocking {
            // Seed a previous closing record
            fakeFinanceDao.dailyClosingsList.add(
                DailyClosing(
                    uuid = "prev-uuid",
                    storeId = 1L,
                    dateStr = "2026-06-25",
                    openingBalance = 100000.0,
                    revenue = 200000.0,
                    hpp = 80000.0,
                    grossProfit = 120000.0,
                    operationalExpense = 30000.0,
                    wasteCost = 10000.0,
                    netProfit = 80000.0,
                    cashInflow = 200000.0,
                    cashOutflow = 40000.0,
                    closingBalance = 260000.0, // 100k + 200k - 40k = 260k
                    cashRevenue = 200000.0,
                    qrisRevenue = 0.0,
                    transferRevenue = 0.0,
                    totalTransactions = 5,
                    averageTicket = 40000.0,
                    closedByUserUuid = "user-admin"
                )
            )

            val lastBalance = financeRepository.getLatestClosingBalance(1L)
            assertEquals(260000.0, lastBalance, 0.0)

            val newClosing = DailyClosing(
                uuid = "new-uuid",
                storeId = 1L,
                dateStr = "2026-06-26",
                openingBalance = lastBalance,
                revenue = 150000.0,
                hpp = 60000.0,
                grossProfit = 90000.0,
                operationalExpense = 20000.0,
                wasteCost = 5000.0,
                netProfit = 65000.0,
                cashInflow = 150000.0,
                cashOutflow = 25000.0,
                closingBalance = lastBalance + 150000.0 - 25000.0, // 260k + 150k - 25k = 385k
                cashRevenue = 150000.0,
                qrisRevenue = 0.0,
                transferRevenue = 0.0,
                totalTransactions = 3,
                averageTicket = 50000.0,
                closedByUserUuid = "user-admin"
            )

            val id = financeRepository.insertDailyClosing(newClosing)
            assertTrue(id > 0)

            val fetchClosing = financeRepository.getDailyClosingByDate(1L, "2026-06-26")
            assertNotNull(fetchClosing)
            assertEquals(385000.0, fetchClosing!!.closingBalance, 0.0)
        }
    }

    @Test
    fun testCashShiftAndVariance() {
        runBlocking {
            val openId = financeRepository.startCashShift(1L, "cashier-01", 150000.0)
            assertTrue(openId > 0)

            fakeFinanceDao.cashSalesDuringShift = 450000.0
            fakeFinanceDao.cashExpensesDuringShift = 50000.0

            val expected = financeRepository.getExpectedCashForActiveShift(1L)
            assertEquals(550000.0, expected, 0.0)

            val success = financeRepository.closeActiveCashShift(1L, 540000.0)
            assertTrue(success)

            val active = financeRepository.getActiveCashShift(1L)
            assertNull(active)

            val shifts = financeRepository.getCashShifts(1L)
            assertEquals(1, shifts.size)
            assertEquals(550000.0, shifts[0].expectedCash!!, 0.0)
            assertEquals(540000.0, shifts[0].actualCash!!, 0.0)
            assertEquals(-10000.0, shifts[0].cashDifference!!, 0.0)
        }
    }
}
