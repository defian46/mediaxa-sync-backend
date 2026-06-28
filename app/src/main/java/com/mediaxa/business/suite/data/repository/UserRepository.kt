package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.SyncStatus
import com.mediaxa.business.suite.data.local.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val localDataSource: LocalDataSource) {
    suspend fun getUserByUsername(username: String): User? = localDataSource.userDao.getUserByUsername(username)
    
    suspend fun getUserByUuid(uuid: String): User? = localDataSource.userDao.getUserByUuid(uuid)
    
    suspend fun getUserByPin(hashedPin: String): User? = localDataSource.userDao.getUserByPin(hashedPin)
    
    fun getAllUsersFlow(): Flow<List<User>> = localDataSource.userDao.getAllUsersFlow()
    
    suspend fun insertUser(user: User): Long = localDataSource.userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = localDataSource.userDao.updateUser(user)
    
    suspend fun softDeleteUser(user: User) {
        val updated = user.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_DELETE.name,
            updatedAt = System.currentTimeMillis()
        )
        localDataSource.userDao.updateUser(updated)
    }
}
