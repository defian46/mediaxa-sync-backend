package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.StoreSetting
import kotlinx.coroutines.flow.Flow

class StoreSettingRepository(private val localDataSource: LocalDataSource) {
    fun getSettingsFlow(): Flow<StoreSetting?> = localDataSource.storeSettingDao.getSettingsFlow()
    
    suspend fun getSettings(): StoreSetting? = localDataSource.storeSettingDao.getSettings()
    
    suspend fun saveSettings(settings: StoreSetting) = localDataSource.storeSettingDao.insertOrUpdate(settings)
}
