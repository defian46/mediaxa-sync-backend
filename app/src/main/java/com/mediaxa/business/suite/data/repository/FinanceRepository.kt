package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.dao.CategoryExpenseResult
import com.mediaxa.business.suite.data.local.dao.DailySalesTrendResult
import com.mediaxa.business.suite.data.local.entity.CashShift
import com.mediaxa.business.suite.data.local.entity.DailyClosing

class FinanceRepository(private val localDataSource: LocalDataSource) {

    suspend fun getProfitSummary(storeId: Long, startDate: Long, endDate: Long): ProfitSummary {
        val sales = localDataSource.financeDao.getSalesSummary(storeId, startDate, endDate)
        val expenses = localDataSource.financeDao.getExpenseSummary(storeId, startDate, endDate)
        val waste = localDataSource.financeDao.getWasteCostSummary(storeId, startDate, endDate)

        val grossProfit = sales.totalRevenue - sales.totalHpp
        val netProfit = sales.totalRevenue - sales.totalHpp - expenses.operationalExpense - waste

        return ProfitSummary(
            revenue = sales.totalRevenue,
            hpp = sales.totalHpp,
            grossProfit = grossProfit,
            operationalExpense = expenses.operationalExpense,
            inventoryPurchase = expenses.inventoryPurchase,
            wasteCost = waste,
            netProfit = netProfit
        )
    }

    suspend fun getCashFlow(storeId: Long, startDate: Long, endDate: Long): CashFlowReport {
        val inflows = localDataSource.financeDao.getSalesInflowByMethod(storeId, startDate, endDate)
        val outflows = localDataSource.financeDao.getExpenseOutflowByMethod(storeId, startDate, endDate)

        val balanceMap = mutableMapOf<String, PaymentMethodBalance>()

        // Process Inflows
        var totalInflow = 0.0
        for (inflow in inflows) {
            val method = inflow.method.uppercase().trim()
            if (method.isEmpty()) continue
            val amount = inflow.amount
            totalInflow += amount

            val current = balanceMap[method] ?: PaymentMethodBalance(method, 0.0, 0.0, 0.0)
            balanceMap[method] = current.copy(
                inflow = current.inflow + amount,
                balance = current.balance + amount
            )
        }

        // Process Outflows
        var totalOutflow = 0.0
        for (outflow in outflows) {
            val method = outflow.method.uppercase().trim()
            if (method.isEmpty()) continue
            val amount = outflow.amount
            totalOutflow += amount

            val current = balanceMap[method] ?: PaymentMethodBalance(method, 0.0, 0.0, 0.0)
            balanceMap[method] = current.copy(
                outflow = current.outflow + amount,
                balance = current.balance - amount
            )
        }

        // Ensure default methods are represented if they have balance or to show in UI
        val defaultMethods = listOf("CASH", "QRIS", "TRANSFER", "DEBIT")
        for (m in defaultMethods) {
            if (!balanceMap.containsKey(m)) {
                balanceMap[m] = PaymentMethodBalance(m, 0.0, 0.0, 0.0)
            }
        }

        return CashFlowReport(
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netCashFlow = totalInflow - totalOutflow,
            cashBalances = balanceMap
        )
    }

    suspend fun getExpensesByCategory(storeId: Long, startDate: Long, endDate: Long): List<CategoryExpenseResult> {
        return localDataSource.financeDao.getExpensesByCategory(storeId, startDate, endDate)
    }

    suspend fun getDailySalesTrend(storeId: Long, startDate: Long, endDate: Long): List<DailySalesTrendResult> {
        return localDataSource.financeDao.getDailySalesTrend(storeId, startDate, endDate)
    }

    suspend fun getLatestClosingBalance(storeId: Long): Double {
        return localDataSource.financeDao.getLatestClosingBalance(storeId) ?: 0.0
    }

    suspend fun insertDailyClosing(dailyClosing: DailyClosing): Long {
        return localDataSource.financeDao.insertDailyClosing(dailyClosing)
    }

    suspend fun getDailyClosings(storeId: Long): List<DailyClosing> {
        return localDataSource.financeDao.getDailyClosings(storeId)
    }

    suspend fun getDailyClosingByDate(storeId: Long, dateStr: String): DailyClosing? {
        return localDataSource.financeDao.getDailyClosingByDate(storeId, dateStr)
    }

    suspend fun getActiveCashShift(storeId: Long): CashShift? {
        return localDataSource.financeDao.getActiveCashShift(storeId)
    }

    suspend fun getCashShifts(storeId: Long): List<CashShift> {
        return localDataSource.financeDao.getCashShifts(storeId)
    }

    suspend fun startCashShift(storeId: Long, cashierUuid: String, openingCash: Double): Long {
        val active = localDataSource.financeDao.getActiveCashShift(storeId)
        if (active != null) return -1L

        val newShift = CashShift(
            storeId = storeId,
            cashierUuid = cashierUuid,
            startTime = System.currentTimeMillis(),
            openingCash = openingCash,
            status = "ACTIVE"
        )
        return localDataSource.financeDao.insertCashShift(newShift)
    }

    suspend fun closeActiveCashShift(storeId: Long, actualCash: Double): Boolean {
        val active = localDataSource.financeDao.getActiveCashShift(storeId) ?: return false
        val endTime = System.currentTimeMillis()
        val salesCash = localDataSource.financeDao.getCashSalesDuringShift(storeId, active.startTime, endTime)
        val expensesCash = localDataSource.financeDao.getCashExpensesDuringShift(storeId, active.startTime, endTime)

        val expected = active.openingCash + salesCash - expensesCash
        val diff = actualCash - expected

        val closedShift = active.copy(
            endTime = endTime,
            closingCash = actualCash,
            expectedCash = expected,
            actualCash = actualCash,
            cashDifference = diff,
            status = "CLOSED",
            updatedAt = System.currentTimeMillis()
        )

        return localDataSource.financeDao.updateCashShift(closedShift) > 0
    }

    suspend fun getExpectedCashForActiveShift(storeId: Long): Double {
        val active = localDataSource.financeDao.getActiveCashShift(storeId) ?: return 0.0
        val salesCash = localDataSource.financeDao.getCashSalesDuringShift(storeId, active.startTime, System.currentTimeMillis())
        val expensesCash = localDataSource.financeDao.getCashExpensesDuringShift(storeId, active.startTime, System.currentTimeMillis())
        return active.openingCash + salesCash - expensesCash
    }

    suspend fun getTransactionCountInRange(storeId: Long, startDate: Long, endDate: Long): Int {
        return localDataSource.financeDao.getTransactionCountInRange(storeId, startDate, endDate)
    }
}

data class ProfitSummary(
    val revenue: Double,
    val hpp: Double,
    val grossProfit: Double,
    val operationalExpense: Double,
    val inventoryPurchase: Double,
    val wasteCost: Double,
    val netProfit: Double
)

data class CashFlowReport(
    val totalInflow: Double,
    val totalOutflow: Double,
    val netCashFlow: Double,
    val cashBalances: Map<String, PaymentMethodBalance>
)

data class PaymentMethodBalance(
    val paymentMethod: String,
    val inflow: Double,
    val outflow: Double,
    val balance: Double
)
