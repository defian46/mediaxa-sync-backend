package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.StoreSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSettingDao {
    @Query("SELECT * FROM store_settings WHERE isDeleted = 0 LIMIT 1")
    fun getSettingsFlow(): Flow<StoreSetting?>

    @Query("SELECT * FROM store_settings WHERE isDeleted = 0 LIMIT 1")
    suspend fun getSettings(): StoreSetting?

    @Query("SELECT * FROM store_settings WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedSettings(): List<StoreSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: StoreSetting)
}
