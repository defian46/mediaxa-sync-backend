package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.PromotionRule
import kotlinx.coroutines.flow.Flow

class PromotionRepository(private val localDataSource: LocalDataSource) {

    suspend fun insertPromotionRule(rule: PromotionRule): Long {
        return localDataSource.promotionDao.insertPromotionRule(rule)
    }

    suspend fun updatePromotionRule(rule: PromotionRule) {
        localDataSource.promotionDao.updatePromotionRule(rule)
    }

    suspend fun getPromotionRuleByUuid(uuid: String): PromotionRule? {
        return localDataSource.promotionDao.getPromotionRuleByUuid(uuid)
    }

    fun getAllPromotionRulesFlow(storeId: Long): Flow<List<PromotionRule>> {
        return localDataSource.promotionDao.getAllPromotionRulesFlow(storeId)
    }

    suspend fun getAllPromotionRules(storeId: Long): List<PromotionRule> {
        return localDataSource.promotionDao.getAllPromotionRules(storeId)
    }

    fun getActivePromotionRulesFlow(storeId: Long): Flow<List<PromotionRule>> {
        return localDataSource.promotionDao.getActivePromotionRulesFlow(storeId)
    }

    suspend fun getActivePromotionRules(storeId: Long): List<PromotionRule> {
        return localDataSource.promotionDao.getActivePromotionRules(storeId)
    }

    suspend fun getPromotionByVoucherCode(storeId: Long, code: String): PromotionRule? {
        return localDataSource.promotionDao.getPromotionByVoucherCode(storeId, code)
    }
}
