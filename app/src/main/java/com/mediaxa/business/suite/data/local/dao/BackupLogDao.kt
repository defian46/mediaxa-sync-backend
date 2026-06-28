package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.BackupLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupLogDao {
    @Query("SELECT * FROM backup_logs WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllBackupLogsFlow(): Flow<List<BackupLog>>

    @Query("SELECT * FROM backup_logs WHERE status = 'SUCCESS' AND isDeleted = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSuccessfulBackup(): BackupLog?

    @Query("SELECT * FROM backup_logs WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedBackupLogs(): List<BackupLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupLog(log: BackupLog): Long
}
