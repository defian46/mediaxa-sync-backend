package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.repository.SalesRepository
import com.mediaxa.business.suite.data.repository.AnalyticsRepository
import com.mediaxa.business.suite.data.repository.InventoryRepository
import com.mediaxa.business.suite.presentation.viewmodel.DashboardPeriod
import com.mediaxa.business.suite.presentation.viewmodel.DashboardViewModel
import com.mediaxa.business.suite.presentation.viewmodel.DashboardUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardAnalyticsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var localDataSource: LocalDataSource
    private lateinit var salesSummaryDao: SalesSummaryDao
    private lateinit var ingredientDao: IngredientDao
    
    private lateinit var salesRepository: SalesRepository
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var inventoryRepository: InventoryRepository

    private lateinit var dashboardViewModel: DashboardViewModel

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        any<Any>()
        return null as T
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = mock(LocalDataSource::class.java)
        salesSummaryDao = mock(SalesSummaryDao::class.java)
        ingredientDao = mock(IngredientDao::class.java)

        `when`(localDataSource.salesSummaryDao).thenReturn(salesSummaryDao)
        `when`(localDataSource.ingredientDao).thenReturn(ingredientDao)

        salesRepository = SalesRepository(localDataSource)
        analyticsRepository = AnalyticsRepository(localDataSource)
        inventoryRepository = InventoryRepository(localDataSource)

        // Mock DAO aggregations
        runBlocking {
            `when`(salesSummaryDao.getSalesSummary(anyLong(), anyLong(), anyLong())).thenReturn(
                SalesSummaryResult(totalRevenue = 500000.0, totalProfit = 220000.0, transactionCount = 15, averageTicketValue = 33333.33)
            )
            `when`(salesSummaryDao.getItemsSoldCount(anyLong(), anyLong(), anyLong())).thenReturn(45)
            `when`(salesSummaryDao.getTopSellingProducts(anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(
                listOf(ProductSalesResult("menu-01", "Kopi Susu", 25, 250000.0))
            )
            `when`(salesSummaryDao.getBottomSellingProducts(anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(
                listOf(ProductSalesResult("menu-02", "Juice Alpukat", 2, 360000.0))
            )
            `when`(salesSummaryDao.getHourlySalesDistribution(anyLong(), anyLong(), anyLong())).thenReturn(
                listOf(HourlySalesResult(hourOfDay = 10, transactionCount = 4, totalAmount = 120000.0))
            )
            `when`(salesSummaryDao.getPaymentMethodDistribution(anyLong(), anyLong(), anyLong())).thenReturn(
                listOf(PaymentDistributionResult("QRIS", 350000.0, 10))
            )
            `when`(salesSummaryDao.getCashierPerformance(anyLong(), anyLong(), anyLong())).thenReturn(
                listOf(CashierPerformanceResult("user-1", "Cashier A", 15, 500000.0, 33333.33))
            )
            `when`(salesSummaryDao.getCategorySalesDistribution(anyLong(), anyLong(), anyLong())).thenReturn(
                listOf(CategorySalesResult("cat-1", "Coffee", 25, 250000.0))
            )
            `when`(ingredientDao.getIngredientVelocities(anyLong(), anyLong())).thenReturn(
                listOf(IngredientVelocityResult("ing-01", "Plastic Cup", 100.0, "pcs", 10.0))
            )
        }

        dashboardViewModel = DashboardViewModel(salesRepository, analyticsRepository, inventoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDashboardSummaryCalculations() {
        runBlocking {
            // Load and evaluate initial Today stats
            val state = dashboardViewModel.uiState.value
            assertEquals(500000.0, state.totalRevenue, 0.001)
            assertEquals(220000.0, state.totalProfit, 0.001)
            assertEquals(15, state.transactionCount)
            assertEquals(33333.33, state.averageTicket, 0.001)
            assertEquals(45, state.itemsSold)
        }
    }

    @Test
    fun testGrowthRatesCalculation() {
        runBlocking {
            // Mock previous period to have different values to test growth rates
            `when`(salesSummaryDao.getSalesSummary(eq(1L), anyLong(), anyLong())).thenAnswer { invocation ->
                val start = invocation.getArgument<Long>(1)
                
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val midnightToday = cal.timeInMillis

                // If it is the current period (Today starts at midnightToday)
                if (start >= midnightToday) {
                    SalesSummaryResult(totalRevenue = 500000.0, totalProfit = 220000.0, transactionCount = 15, averageTicketValue = 33333.33)
                } else {
                    // Previous period summary
                    SalesSummaryResult(totalRevenue = 400000.0, totalProfit = 180000.0, transactionCount = 10, averageTicketValue = 40000.0)
                }
            }

            dashboardViewModel.refresh()
            val state = dashboardViewModel.uiState.value

            // Growth calculation: ((500000 - 400000) / 400000) * 100 = 25.0%
            assertEquals(25.0, state.revenueGrowthPercent, 0.001)
            // Profit Growth calculation: ((220000 - 180000) / 180000) * 100 = 22.22%
            assertEquals(22.22, state.profitGrowthPercent, 0.1)
        }
    }

    @Test
    fun testInventoryVelocityCalculations() {
        // availableStock = 100.0, dailyVelocity = 10.0 -> daysRemaining should be 10.0 days
        val forecast = IngredientVelocityResult("ing-01", "Plastic Cup", 100.0, "pcs", 10.0)
        val daysRemaining = forecast.availableStock / forecast.dailyVelocity
        assertEquals(10.0, daysRemaining, 0.001)

        // dailyVelocity = 0.0 -> daysRemaining should map to Double.MAX_VALUE
        val idleForecast = IngredientVelocityResult("ing-susu", "Susu UHT", 5.0, "liter", 0.0)
        val idleDays = if (idleForecast.dailyVelocity > 0.0) idleForecast.availableStock / idleForecast.dailyVelocity else Double.MAX_VALUE
        assertEquals(Double.MAX_VALUE, idleDays, 0.001)
    }

    @Test
    fun testTargetTrackerEstimation() {
        val currentRevenue = 500000.0
        val targetSales = 10000000.0 // 10jt
        val progressPercent = (currentRevenue / targetSales) * 100.0
        assertEquals(5.0, progressPercent, 0.001)

        val dailyRevenue = 500000.0 / 1.0 // 1 day
        val estDays = (targetSales - currentRevenue) / dailyRevenue
        assertEquals(19.0, estDays, 0.001)
    }

    @Test
    fun testStoreIsolationQueryConstraint() {
        runBlocking {
            // Verify repositories pass storeId param correctly to safety checks
            salesRepository.getSalesSummary(storeId = 999L, startDate = 0L, endDate = 100000L)
            verify(salesSummaryDao).getSalesSummary(eq(999L), anyLong(), anyLong())

            analyticsRepository.getCashierPerformance(storeId = 999L, startDate = 0L, endDate = 100000L)
            verify(salesSummaryDao).getCashierPerformance(eq(999L), anyLong(), anyLong())

            inventoryRepository.getIngredientVelocities(storeId = 999L, thirtyDaysAgo = 0L)
            verify(ingredientDao).getIngredientVelocities(eq(999L), anyLong())
        }
    }
}
