package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.PromotionRule
import kotlinx.coroutines.flow.Flow

@Dao
interface PromotionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotionRule(rule: PromotionRule): Long

    @Update
    suspend fun updatePromotionRule(rule: PromotionRule)

    @Query("SELECT * FROM promotion_rules WHERE uuid = :uuid LIMIT 1")
    suspend fun getPromotionRuleByUuid(uuid: String): PromotionRule?

    @Query("SELECT * FROM promotion_rules WHERE storeId = :storeId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllPromotionRulesFlow(storeId: Long): Flow<List<PromotionRule>>

    @Query("SELECT * FROM promotion_rules WHERE storeId = :storeId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllPromotionRules(storeId: Long): List<PromotionRule>

    @Query("SELECT * FROM promotion_rules WHERE storeId = :storeId AND isActive = 1 AND isDeleted = 0")
    fun getActivePromotionRulesFlow(storeId: Long): Flow<List<PromotionRule>>

    @Query("SELECT * FROM promotion_rules WHERE storeId = :storeId AND isActive = 1 AND isDeleted = 0")
    suspend fun getActivePromotionRules(storeId: Long): List<PromotionRule>

    @Query("SELECT * FROM promotion_rules WHERE storeId = :storeId AND promoCode = :code AND isActive = 1 AND isDeleted = 0 LIMIT 1")
    suspend fun getPromotionByVoucherCode(storeId: Long, code: String): PromotionRule?
}
