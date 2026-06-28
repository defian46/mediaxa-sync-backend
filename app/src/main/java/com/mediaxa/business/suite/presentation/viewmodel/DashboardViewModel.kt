package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.repository.SalesRepository
import com.mediaxa.business.suite.data.repository.AnalyticsRepository
import com.mediaxa.business.suite.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.util.Calendar

enum class DashboardPeriod {
    TODAY, YESTERDAY, WEEKLY, MONTHLY
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val period: DashboardPeriod = DashboardPeriod.TODAY,
    val storeId: Long = 1,
    // Summary Card values
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTicket: Double = 0.0,
    val itemsSold: Int = 0,
    // Comparisons
    val revenueGrowthPercent: Double = 0.0,
    val profitGrowthPercent: Double = 0.0,
    // Lists & Graphs
    val hourlyTrends: List<HourlySalesResult> = emptyList(),
    val topProducts: List<ProductSalesResult> = emptyList(),
    val bottomProducts: List<ProductSalesResult> = emptyList(),
    val paymentBreakdown: List<PaymentDistributionResult> = emptyList(),
    val cashierStats: List<CashierPerformanceResult> = emptyList(),
    val categoryStats: List<CategorySalesResult> = emptyList(),
    val stockAlerts: List<IngredientForecast> = emptyList(),
    // Targets
    val targetSales: Double = 10000000.0, // default target 10jt
    val targetProgressPercent: Double = 0.0,
    val targetEstimationDays: Double = 0.0
)

data class IngredientForecast(
    val uuid: String,
    val name: String,
    val availableStock: Double,
    val unit: String,
    val dailyVelocity: Double,
    val daysRemaining: Double // estimated days left, Double.MAX_VALUE if velocity is 0
)

class DashboardViewModel(
    private val salesRepository: SalesRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Simple cache for dashboard metrics to achieve instant loads
    private val cache = mutableMapOf<DashboardPeriod, DashboardUiState>()

    init {
        loadDashboardData(DashboardPeriod.TODAY)
    }

    fun setPeriod(period: DashboardPeriod) {
        _uiState.value = _uiState.value.copy(period = period)
        loadDashboardData(period)
    }

    fun refresh() {
        cache.clear()
        loadDashboardData(_uiState.value.period)
    }

    private fun loadDashboardData(period: DashboardPeriod) {
        // If cached and it's not TODAY (which changes frequently), return cache
        if (period != DashboardPeriod.TODAY && cache.containsKey(period)) {
            _uiState.value = cache[period]!!
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val storeId = _uiState.value.storeId

            val now = System.currentTimeMillis()
            val (startDate, endDate) = getPeriodRange(period, now)
            val (prevStartDate, prevEndDate) = getPreviousPeriodRange(period, now)

            try {
                // 1. Fetch sales summary in parallel
                val summaryDeferred = async { salesRepository.getSalesSummary(storeId, startDate, endDate) }
                val prevSummaryDeferred = async { salesRepository.getSalesSummary(storeId, prevStartDate, prevEndDate) }
                val itemsSoldDeferred = async { salesRepository.getItemsSoldCount(storeId, startDate, endDate) }

                // 2. Fetch lists and graphs in parallel
                val hourlyTrendsDeferred = async { analyticsRepository.getHourlySalesDistribution(storeId, startDate, endDate) }
                val topProductsDeferred = async { salesRepository.getTopSellingProducts(storeId, startDate, endDate, 5) }
                val bottomProductsDeferred = async { salesRepository.getBottomSellingProducts(storeId, startDate, endDate, 5) }
                val paymentBreakdownDeferred = async { analyticsRepository.getPaymentMethodDistribution(storeId, startDate, endDate) }
                val cashierStatsDeferred = async { analyticsRepository.getCashierPerformance(storeId, startDate, endDate) }
                val categoryStatsDeferred = async { analyticsRepository.getCategorySalesDistribution(storeId, startDate, endDate) }
                
                // Thirty days ago for inventory velocity
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                val velocitiesDeferred = async { inventoryRepository.getIngredientVelocities(storeId, thirtyDaysAgo) }

                val summary = summaryDeferred.await()
                val prevSummary = prevSummaryDeferred.await()
                val itemsSold = itemsSoldDeferred.await()
                val hourlyTrends = hourlyTrendsDeferred.await()
                val topProducts = topProductsDeferred.await()
                val bottomProducts = bottomProductsDeferred.await()
                val paymentBreakdown = paymentBreakdownDeferred.await()
                val cashierStats = cashierStatsDeferred.await()
                val categoryStats = categoryStatsDeferred.await()
                val velocities = velocitiesDeferred.await()

                // Calculate growth rates
                val currentRevenue = summary?.totalRevenue ?: 0.0
                val prevRevenue = prevSummary?.totalRevenue ?: 0.0
                val revenueGrowth = if (prevRevenue > 0.0) {
                    ((currentRevenue - prevRevenue) / prevRevenue) * 100.0
                } else {
                    0.0
                }

                val currentProfit = summary?.totalProfit ?: 0.0
                val prevProfit = prevSummary?.totalProfit ?: 0.0
                val profitGrowth = if (prevProfit > 0.0) {
                    ((currentProfit - prevProfit) / prevProfit) * 100.0
                } else {
                    0.0
                }

                // Inventory Forecast Mapping
                val forecasts = velocities.map { vel ->
                    val days = if (vel.dailyVelocity > 0.0) vel.availableStock / vel.dailyVelocity else Double.MAX_VALUE
                    IngredientForecast(
                        uuid = vel.ingredientUuid,
                        name = vel.ingredientName,
                        availableStock = vel.availableStock,
                        unit = vel.unit,
                        dailyVelocity = vel.dailyVelocity,
                        daysRemaining = days
                    )
                }.sortedBy { it.daysRemaining }

                // Sales goal target estimation
                val targetSales = _uiState.value.targetSales
                val progressPercent = (currentRevenue / targetSales * 100.0).coerceIn(0.0, 100.0)
                
                // Calculate average sales rate to target
                val daysInPeriod = when (period) {
                    DashboardPeriod.TODAY, DashboardPeriod.YESTERDAY -> 1.0
                    DashboardPeriod.WEEKLY -> 7.0
                    DashboardPeriod.MONTHLY -> 30.0
                }
                val dailyRevenue = currentRevenue / daysInPeriod
                val targetEstDays = if (dailyRevenue > 0) {
                    (targetSales - currentRevenue).coerceAtLeast(0.0) / dailyRevenue
                } else {
                    Double.MAX_VALUE
                }

                val resultState = DashboardUiState(
                    isLoading = false,
                    period = period,
                    storeId = storeId,
                    totalRevenue = currentRevenue,
                    totalProfit = currentProfit,
                    transactionCount = summary?.transactionCount ?: 0,
                    averageTicket = summary?.averageTicketValue ?: 0.0,
                    itemsSold = itemsSold,
                    revenueGrowthPercent = revenueGrowth,
                    profitGrowthPercent = profitGrowth,
                    hourlyTrends = hourlyTrends,
                    topProducts = topProducts,
                    bottomProducts = bottomProducts,
                    paymentBreakdown = paymentBreakdown,
                    cashierStats = cashierStats,
                    categoryStats = categoryStats,
                    stockAlerts = forecasts,
                    targetSales = targetSales,
                    targetProgressPercent = progressPercent,
                    targetEstimationDays = targetEstDays
                )

                // Store in cache
                cache[period] = resultState
                _uiState.value = resultState

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                e.printStackTrace()
            }
        }
    }

    private fun getPeriodRange(period: DashboardPeriod, now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now

        return when (period) {
            DashboardPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + (24L * 60 * 60 * 1000) - 1
                Pair(start, end)
            }
            DashboardPeriod.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + (24L * 60 * 60 * 1000) - 1
                Pair(start, end)
            }
            DashboardPeriod.WEEKLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            DashboardPeriod.MONTHLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
        }
    }

    private fun getPreviousPeriodRange(period: DashboardPeriod, now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now

        return when (period) {
            DashboardPeriod.TODAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + (24L * 60 * 60 * 1000) - 1
                Pair(start, end)
            }
            DashboardPeriod.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -2)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + (24L * 60 * 60 * 1000) - 1
                Pair(start, end)
            }
            DashboardPeriod.WEEKLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -14)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + (7L * 24 * 60 * 60 * 1000) - 1
                Pair(start, end)
            }
            DashboardPeriod.MONTHLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -60)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + (30L * 24 * 60 * 60 * 1000) - 1
                Pair(start, end)
            }
        }
    }
}

class DashboardViewModelFactory(
    private val salesRepository: SalesRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(salesRepository, analyticsRepository, inventoryRepository) as T
    }
}
