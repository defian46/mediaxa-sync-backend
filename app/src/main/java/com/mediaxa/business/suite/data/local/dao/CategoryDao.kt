package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY displayOrder ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isDeleted = 0 AND isActive = 1 ORDER BY displayOrder ASC")
    fun getActiveCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE uuid = :uuid LIMIT 1")
    suspend fun getCategoryByUuid(uuid: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)
}
