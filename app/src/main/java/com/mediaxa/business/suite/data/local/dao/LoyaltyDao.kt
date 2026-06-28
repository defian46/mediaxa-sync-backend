package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.LoyaltyPointHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointHistory(history: LoyaltyPointHistory): Long

    @Query("SELECT * FROM loyalty_point_history WHERE customerUuid = :customerUuid AND storeId = :storeId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getPointHistoryFlow(storeId: Long, customerUuid: String): Flow<List<LoyaltyPointHistory>>

    @Query("SELECT * FROM loyalty_point_history WHERE customerUuid = :customerUuid AND storeId = :storeId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getPointHistory(storeId: Long, customerUuid: String): List<LoyaltyPointHistory>

    @Query("SELECT COALESCE(SUM(points), 0) FROM loyalty_point_history WHERE customerUuid = :customerUuid AND storeId = :storeId AND isDeleted = 0")
    fun getPointsBalanceFlow(storeId: Long, customerUuid: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(points), 0) FROM loyalty_point_history WHERE customerUuid = :customerUuid AND storeId = :storeId AND isDeleted = 0")
    suspend fun getPointsBalance(storeId: Long, customerUuid: String): Int
}
