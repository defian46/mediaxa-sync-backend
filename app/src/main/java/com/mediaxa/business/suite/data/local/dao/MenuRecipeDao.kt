package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.MenuRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuRecipeDao {
    @Query("SELECT * FROM menu_recipes WHERE menuUuid = :menuUuid AND isDeleted = 0")
    fun getRecipeForMenuFlow(menuUuid: String): Flow<List<MenuRecipe>>

    @Query("SELECT * FROM menu_recipes WHERE menuUuid = :menuUuid AND isDeleted = 0")
    suspend fun getRecipeForMenu(menuUuid: String): List<MenuRecipe>

    @Query("SELECT * FROM menu_recipes WHERE menuUuid IN (:menuUuids) AND isDeleted = 0")
    suspend fun getRecipesForMenus(menuUuids: List<String>): List<MenuRecipe>

    @Query("SELECT * FROM menu_recipes WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedRecipes(): List<MenuRecipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeItems(recipes: List<MenuRecipe>)

    @Query("UPDATE menu_recipes SET isDeleted = 1, deletedAt = :deletedAt, syncStatus = :syncStatus WHERE menuUuid = :menuUuid")
    suspend fun softDeleteRecipeForMenu(menuUuid: String, deletedAt: Long, syncStatus: String)
}
