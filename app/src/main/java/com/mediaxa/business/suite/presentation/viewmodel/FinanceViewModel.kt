package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.dao.CategoryExpenseResult
import com.mediaxa.business.suite.data.local.dao.DailySalesTrendResult
import com.mediaxa.business.suite.data.local.entity.CashShift
import com.mediaxa.business.suite.data.local.entity.DailyClosing
import com.mediaxa.business.suite.data.repository.CashFlowReport
import com.mediaxa.business.suite.data.repository.FinanceRepository
import com.mediaxa.business.suite.data.repository.ProfitSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _profitSummaryToday = MutableStateFlow(ProfitSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val profitSummaryToday: StateFlow<ProfitSummary> = _profitSummaryToday.asStateFlow()

    private val _profitSummaryMonth = MutableStateFlow(ProfitSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val profitSummaryMonth: StateFlow<ProfitSummary> = _profitSummaryMonth.asStateFlow()

    private val _cashFlowToday = MutableStateFlow(CashFlowReport(0.0, 0.0, 0.0, emptyMap()))
    val cashFlowToday: StateFlow<CashFlowReport> = _cashFlowToday.asStateFlow()

    private val _cashFlowMonth = MutableStateFlow(CashFlowReport(0.0, 0.0, 0.0, emptyMap()))
    val cashFlowMonth: StateFlow<CashFlowReport> = _cashFlowMonth.asStateFlow()

    private val _categoryExpenses = MutableStateFlow<List<CategoryExpenseResult>>(emptyList())
    val categoryExpenses: StateFlow<List<CategoryExpenseResult>> = _categoryExpenses.asStateFlow()

    private val _dailyTrend = MutableStateFlow<List<DailySalesTrendResult>>(emptyList())
    val dailyTrend: StateFlow<List<DailySalesTrendResult>> = _dailyTrend.asStateFlow()

    private val _activeShift = MutableStateFlow<CashShift?>(null)
    val activeShift: StateFlow<CashShift?> = _activeShift.asStateFlow()

    private val _shiftList = MutableStateFlow<List<CashShift>>(emptyList())
    val shiftList: StateFlow<List<CashShift>> = _shiftList.asStateFlow()

    private val _dailyClosings = MutableStateFlow<List<DailyClosing>>(emptyList())
    val dailyClosings: StateFlow<List<DailyClosing>> = _dailyClosings.asStateFlow()

    private val _expectedCash = MutableStateFlow(0.0)
    val expectedCash: StateFlow<Double> = _expectedCash.asStateFlow()

    private val _uiStateMessage = MutableStateFlow<String?>(null)
    val uiStateMessage: StateFlow<String?> = _uiStateMessage.asStateFlow()

    private val _latestClosingBalance = MutableStateFlow(0.0)
    val latestClosingBalance: StateFlow<Double> = _latestClosingBalance.asStateFlow()

    private val _todayTransactionCount = MutableStateFlow(0)
    val todayTransactionCount: StateFlow<Int> = _todayTransactionCount.asStateFlow()

    fun loadFinanceData(storeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val now = System.currentTimeMillis()
                val startToday = getStartOfDay(now)
                val endToday = getEndOfDay(now)

                val startMonth = getStartOfMonth(now)
                val endMonth = getEndOfMonth(now)

                // Load Today P&L & Cash Flow
                _profitSummaryToday.value = financeRepository.getProfitSummary(storeId, startToday, endToday)
                _cashFlowToday.value = financeRepository.getCashFlow(storeId, startToday, endToday)

                // Load Month P&L & Cash Flow
                _profitSummaryMonth.value = financeRepository.getProfitSummary(storeId, startMonth, endMonth)
                _cashFlowMonth.value = financeRepository.getCashFlow(storeId, startMonth, endMonth)

                // Load Monthly Category Expenses
                _categoryExpenses.value = financeRepository.getExpensesByCategory(storeId, startMonth, endMonth)

                // Load 30 Days Trend
                val startTrend = getStartOfDay(now - 30L * 24L * 60L * 60L * 1000L)
                _dailyTrend.value = financeRepository.getDailySalesTrend(storeId, startTrend, endToday)

                // Load Closing helper balances
                _latestClosingBalance.value = financeRepository.getLatestClosingBalance(storeId)
                _todayTransactionCount.value = financeRepository.getTransactionCountInRange(storeId, startToday, endToday)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUiMessage() {
        _uiStateMessage.value = null
    }

    fun loadShiftData(storeId: Long) {
        viewModelScope.launch {
            try {
                _activeShift.value = financeRepository.getActiveCashShift(storeId)
                _shiftList.value = financeRepository.getCashShifts(storeId)
                _expectedCash.value = financeRepository.getExpectedCashForActiveShift(storeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startShift(storeId: Long, openingCash: Double, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val id = financeRepository.startCashShift(storeId, "cashier-user", openingCash)
                if (id > 0) {
                    _uiStateMessage.value = "Shift kasir berhasil dibuka!"
                    loadShiftData(storeId)
                    onSuccess()
                } else {
                    _uiStateMessage.value = "Gagal membuka shift, mungkin masih ada shift aktif."
                }
            } catch (e: Exception) {
                _uiStateMessage.value = "Terjadi kesalahan: ${e.message}"
            }
        }
    }

    fun closeShift(storeId: Long, actualCash: Double, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val success = financeRepository.closeActiveCashShift(storeId, actualCash)
                if (success) {
                    _uiStateMessage.value = "Shift kasir berhasil ditutup!"
                    loadShiftData(storeId)
                    onSuccess()
                } else {
                    _uiStateMessage.value = "Gagal menutup shift. Tidak ada shift aktif."
                }
            } catch (e: Exception) {
                _uiStateMessage.value = "Terjadi kesalahan: ${e.message}"
            }
        }
    }

    fun loadDailyClosings(storeId: Long) {
        viewModelScope.launch {
            try {
                _dailyClosings.value = financeRepository.getDailyClosings(storeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun performDailyClosing(storeId: Long, openingBalance: Double, userUuid: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val start = getStartOfDay(now)
                val end = getEndOfDay(now)
                
                val pSummary = financeRepository.getProfitSummary(storeId, start, end)
                val cFlow = financeRepository.getCashFlow(storeId, start, end)

                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val dateStr = sdf.format(java.util.Date(now))

                val existing = financeRepository.getDailyClosingByDate(storeId, dateStr)
                if (existing != null) {
                    _uiStateMessage.value = "Buku harian untuk tanggal $dateStr sudah ditutup!"
                    return@launch
                }

                val cashBalances = cFlow.cashBalances
                val cashIn = cashBalances["CASH"]?.inflow ?: 0.0
                val cashOut = cashBalances["CASH"]?.outflow ?: 0.0

                val cashRevenue = cashBalances["CASH"]?.inflow ?: 0.0
                val qrisRevenue = cashBalances["QRIS"]?.inflow ?: 0.0
                val transferRevenue = cashBalances["TRANSFER"]?.inflow ?: 0.0

                val closingBal = openingBalance + cashIn - cashOut
                val totalTrx = financeRepository.getTransactionCountInRange(storeId, start, end)
                val avgTicket = if (totalTrx > 0) pSummary.revenue / totalTrx else 0.0

                val dailyClosing = DailyClosing(
                    storeId = storeId,
                    dateStr = dateStr,
                    openingBalance = openingBalance,
                    revenue = pSummary.revenue,
                    hpp = pSummary.hpp,
                    grossProfit = pSummary.grossProfit,
                    operationalExpense = pSummary.operationalExpense,
                    wasteCost = pSummary.wasteCost,
                    netProfit = pSummary.netProfit,
                    cashInflow = cashIn,
                    cashOutflow = cashOut,
                    closingBalance = closingBal,
                    cashRevenue = cashRevenue,
                    qrisRevenue = qrisRevenue,
                    transferRevenue = transferRevenue,
                    totalTransactions = totalTrx,
                    averageTicket = avgTicket,
                    closedByUserUuid = userUuid
                )

                val id = financeRepository.insertDailyClosing(dailyClosing)
                if (id > 0) {
                    _uiStateMessage.value = "Tutup buku harian berhasil disimpan!"
                    loadDailyClosings(storeId)
                    onSuccess()
                } else {
                    _uiStateMessage.value = "Gagal menyimpan tutup buku harian."
                }
            } catch (e: Exception) {
                _uiStateMessage.value = "Terjadi kesalahan: ${e.message}"
            }
        }
    }

    fun getEstimatedMonthlyProfit(): Double {
        val summary = _profitSummaryMonth.value
        val calendar = Calendar.getInstance()
        val elapsedDays = calendar.get(Calendar.DAY_OF_MONTH)
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        if (elapsedDays <= 0) return 0.0
        val avgDailyProfit = summary.netProfit / elapsedDays
        return avgDailyProfit * totalDays
    }

    fun getBreakEvenSales(): Double {
        val summary = _profitSummaryMonth.value
        if (summary.revenue <= 0.0) return 0.0
        val grossMarginRatio = summary.grossProfit / summary.revenue
        if (grossMarginRatio <= 0.0) return 0.0
        return summary.operationalExpense / grossMarginRatio
    }

    private fun getStartOfDay(timeMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(timeMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun getStartOfMonth(timeMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfMonth(timeMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
