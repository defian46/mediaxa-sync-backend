package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.LoyaltyPointHistory
import kotlinx.coroutines.flow.Flow

class LoyaltyRepository(private val localDataSource: LocalDataSource) {

    suspend fun insertPointHistory(history: LoyaltyPointHistory): Long {
        return localDataSource.loyaltyDao.insertPointHistory(history)
    }

    fun getPointHistoryFlow(storeId: Long, customerUuid: String): Flow<List<LoyaltyPointHistory>> {
        return localDataSource.loyaltyDao.getPointHistoryFlow(storeId, customerUuid)
    }

    suspend fun getPointHistory(storeId: Long, customerUuid: String): List<LoyaltyPointHistory> {
        return localDataSource.loyaltyDao.getPointHistory(storeId, customerUuid)
    }

    fun getPointsBalanceFlow(storeId: Long, customerUuid: String): Flow<Int> {
        return localDataSource.loyaltyDao.getPointsBalanceFlow(storeId, customerUuid)
    }

    suspend fun getPointsBalance(storeId: Long, customerUuid: String): Int {
        return localDataSource.loyaltyDao.getPointsBalance(storeId, customerUuid)
    }
}
