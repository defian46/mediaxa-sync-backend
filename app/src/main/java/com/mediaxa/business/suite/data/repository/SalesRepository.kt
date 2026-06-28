package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.dao.SalesSummaryResult
import com.mediaxa.business.suite.data.local.dao.ProductSalesResult

class SalesRepository(private val localDataSource: LocalDataSource) {

    suspend fun getSalesSummary(storeId: Long, startDate: Long, endDate: Long): SalesSummaryResult? {
        return localDataSource.salesSummaryDao.getSalesSummary(storeId, startDate, endDate)
    }

    suspend fun getItemsSoldCount(storeId: Long, startDate: Long, endDate: Long): Int {
        return localDataSource.salesSummaryDao.getItemsSoldCount(storeId, startDate, endDate)
    }

    suspend fun getTopSellingProducts(storeId: Long, startDate: Long, endDate: Long, limit: Int = 10): List<ProductSalesResult> {
        return localDataSource.salesSummaryDao.getTopSellingProducts(storeId, startDate, endDate, limit)
    }

    suspend fun getBottomSellingProducts(storeId: Long, startDate: Long, endDate: Long, limit: Int = 10): List<ProductSalesResult> {
        return localDataSource.salesSummaryDao.getBottomSellingProducts(storeId, startDate, endDate, limit)
    }
}
