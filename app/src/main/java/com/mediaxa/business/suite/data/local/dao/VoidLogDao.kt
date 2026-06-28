package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.VoidLog
import kotlinx.coroutines.flow.Flow

@Dao
interface VoidLogDao {
    @Query("SELECT * FROM void_logs WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllVoidLogsFlow(): Flow<List<VoidLog>>

    @Query("SELECT * FROM void_logs WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedVoidLogs(): List<VoidLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoidLog(voidLog: VoidLog): Long
}
