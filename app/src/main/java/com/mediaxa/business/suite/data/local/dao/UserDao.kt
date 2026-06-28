package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND isDeleted = 0 AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE uuid = :uuid AND isDeleted = 0 LIMIT 1")
    suspend fun getUserByUuid(uuid: String): User?

    @Query("SELECT * FROM users WHERE pin = :hashedPin AND isDeleted = 0 AND isActive = 1 LIMIT 1")
    suspend fun getUserByPin(hashedPin: String): User?

    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY username ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedUsers(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)
}
