package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.Menu
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllMenusFlow(): Flow<List<Menu>>

    @Query("SELECT * FROM menus WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getActiveMenusFlow(): Flow<List<Menu>>

    @Query("SELECT * FROM menus WHERE categoryUuid = :categoryUuid AND isDeleted = 0 ORDER BY name ASC")
    fun getMenusByCategoryFlow(categoryUuid: String): Flow<List<Menu>>

    @Query("SELECT * FROM menus WHERE uuid = :uuid AND isDeleted = 0 LIMIT 1")
    suspend fun getMenuByUuid(uuid: String): Menu?

    @Query("SELECT * FROM menus WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedMenus(): List<Menu>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: Menu): Long

    @Update
    suspend fun updateMenu(menu: Menu)
}
