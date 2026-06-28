package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.dao.CashierPerformanceResult
import com.mediaxa.business.suite.data.local.dao.PaymentDistributionResult
import com.mediaxa.business.suite.data.local.dao.HourlySalesResult
import com.mediaxa.business.suite.data.local.dao.CategorySalesResult

class AnalyticsRepository(private val localDataSource: LocalDataSource) {

    suspend fun getCashierPerformance(storeId: Long, startDate: Long, endDate: Long): List<CashierPerformanceResult> {
        return localDataSource.salesSummaryDao.getCashierPerformance(storeId, startDate, endDate)
    }

    suspend fun getPaymentMethodDistribution(storeId: Long, startDate: Long, endDate: Long): List<PaymentDistributionResult> {
        return localDataSource.salesSummaryDao.getPaymentMethodDistribution(storeId, startDate, endDate)
    }

    suspend fun getHourlySalesDistribution(storeId: Long, startDate: Long, endDate: Long): List<HourlySalesResult> {
        return localDataSource.salesSummaryDao.getHourlySalesDistribution(storeId, startDate, endDate)
    }

    suspend fun getCategorySalesDistribution(storeId: Long, startDate: Long, endDate: Long): List<CategorySalesResult> {
        return localDataSource.salesSummaryDao.getCategorySalesDistribution(storeId, startDate, endDate)
    }
}
