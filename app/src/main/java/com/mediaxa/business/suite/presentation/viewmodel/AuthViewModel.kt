package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.dao.AuditLogDao
import com.mediaxa.business.suite.data.local.entity.User
import com.mediaxa.business.suite.data.local.entity.AuditLog
import com.mediaxa.business.suite.data.local.PreferenceHelper
import com.mediaxa.business.suite.data.remote.NetworkClient
import com.mediaxa.business.suite.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LoginResult {
    object Idle : LoginResult()
    object Loading : LoginResult()
    data class Success(val user: User) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class AuthViewModel(
    private val userRepository: UserRepository,
    private val auditLogDao: AuditLogDao,
    private val context: Context
) : ViewModel() {

    private val _loginResult = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginResult: StateFlow<LoginResult> = _loginResult.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val allUsers: StateFlow<List<User>> = userRepository.getAllUsersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun login(username: String, passwordRaw: String) {
        viewModelScope.launch {
            _loginResult.value = LoginResult.Loading
            try {
                // ── Step 1: Attempt online login (cloud-first) ─────────────────
                val deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: "OFFLINE-DEVICE"

                val cloudResult = withContext(Dispatchers.IO) {
                    NetworkClient.login(username, passwordRaw, deviceId)
                }

                if (cloudResult != null) {
                    // Save JWT tokens and identity to secure local preferences
                    PreferenceHelper.saveTokens(
                        context = context,
                        accessToken = cloudResult.accessToken,
                        refreshToken = cloudResult.refreshToken,
                        storeUuid = cloudResult.user.storeUuid,
                        userUuid = cloudResult.user.uuid
                    )

                    // Sync cloud user profile to local Room (upsert)
                    val localUser = userRepository.getUserByUsername(username)
                    if (localUser != null) {
                        _currentUser.value = localUser
                        _loginResult.value = LoginResult.Success(localUser)
                    } else {
                        // Cloud user not yet in local DB — create a minimal record
                        val newUser = User(
                            uuid = cloudResult.user.uuid,
                            username = cloudResult.user.username,
                            passwordHash = AppDatabase.hashString(passwordRaw),
                            pin = null,
                            role = cloudResult.user.role,
                            isActive = true
                        )
                        userRepository.insertUser(newUser)
                        _currentUser.value = newUser
                        _loginResult.value = LoginResult.Success(newUser)
                    }

                    auditLogDao.insertLog(
                        AuditLog(
                            userUuid = cloudResult.user.uuid,
                            username = cloudResult.user.username,
                            action = "LOGIN_CLOUD",
                            entity = "User",
                            entityId = cloudResult.user.uuid,
                            oldValue = null,
                            newValue = "SUCCESS"
                        )
                    )
                    return@launch
                }

                // ── Step 2: Offline fallback — verify against local Room DB ────
                val user = userRepository.getUserByUsername(username)
                if (user == null) {
                    _loginResult.value = LoginResult.Error("Username tidak ditemukan")
                    return@launch
                }

                if (!user.isActive) {
                    _loginResult.value = LoginResult.Error("Akun Anda dinonaktifkan")
                    return@launch
                }

                val hashed = AppDatabase.hashString(passwordRaw)
                if (user.passwordHash == hashed) {
                    _currentUser.value = user
                    _loginResult.value = LoginResult.Success(user)

                    auditLogDao.insertLog(
                        AuditLog(
                            userUuid = user.uuid,
                            username = user.username,
                            action = "LOGIN_OFFLINE",
                            entity = "User",
                            entityId = user.uuid,
                            oldValue = null,
                            newValue = "SUCCESS"
                        )
                    )
                } else {
                    _loginResult.value = LoginResult.Error("Password salah")
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun loginWithPin(pin: String) {
        viewModelScope.launch {
            _loginResult.value = LoginResult.Loading
            try {
                val hashedPin = AppDatabase.hashString(pin)
                val user = userRepository.getUserByPin(hashedPin)
                if (user != null) {
                    if (!user.isActive) {
                        _loginResult.value = LoginResult.Error("Akun Anda dinonaktifkan")
                        return@launch
                    }
                    _currentUser.value = user
                    _loginResult.value = LoginResult.Success(user)

                    // Audit log login success
                    auditLogDao.insertLog(
                        AuditLog(
                            userUuid = user.uuid,
                            username = user.username,
                            action = "LOGIN_PIN",
                            entity = "User",
                            entityId = user.uuid,
                            oldValue = null,
                            newValue = "SUCCESS"
                        )
                    )
                } else {
                    _loginResult.value = LoginResult.Error("PIN tidak valid")
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                auditLogDao.insertLog(
                    AuditLog(
                        userUuid = user.uuid,
                        username = user.username,
                        action = "LOGOUT",
                        entity = "User",
                        entityId = user.uuid,
                        oldValue = "ACTIVE",
                        newValue = "INACTIVE"
                    )
                )
            }
            _currentUser.value = null
            _loginResult.value = LoginResult.Idle
        }
    }

    fun clearResult() {
        _loginResult.value = LoginResult.Idle
    }

    fun changeOwnUsername(oldPasswordRaw: String, newUsername: String, onResult: (String?, Boolean) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onResult("User belum login", false)
            return
        }
        viewModelScope.launch {
            try {
                val existingUser = userRepository.getUserByUsername(newUsername)
                if (existingUser != null && existingUser.uuid != user.uuid) {
                    onResult("Username sudah digunakan", false)
                    return@launch
                }

                val hashedOldPassword = AppDatabase.hashString(oldPasswordRaw)
                if (user.passwordHash != hashedOldPassword) {
                    onResult("Password lama salah", false)
                    return@launch
                }

                val updatedUser = user.copy(
                    username = newUsername,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = com.mediaxa.business.suite.data.local.entity.SyncStatus.PENDING_UPDATE.name
                )
                userRepository.updateUser(updatedUser)
                _currentUser.value = updatedUser

                auditLogDao.insertLog(
                    AuditLog(
                        userUuid = user.uuid,
                        username = user.username,
                        action = "CHANGE_USERNAME",
                        entity = "User",
                        entityId = user.uuid,
                        oldValue = user.username,
                        newValue = newUsername
                    )
                )
                onResult("Username berhasil diubah", true)
            } catch (e: Exception) {
                onResult("Terjadi kesalahan: ${e.message}", false)
            }
        }
    }

    fun changeOwnPassword(oldPasswordRaw: String, newPasswordRaw: String, onResult: (String?, Boolean) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onResult("User belum login", false)
            return
        }
        if (newPasswordRaw.length < 6) {
            onResult("Password baru minimal 6 karakter", false)
            return
        }
        viewModelScope.launch {
            try {
                val hashedOldPassword = AppDatabase.hashString(oldPasswordRaw)
                if (user.passwordHash != hashedOldPassword) {
                    onResult("Password lama salah", false)
                    return@launch
                }

                val hashedNewPassword = AppDatabase.hashString(newPasswordRaw)
                val updatedUser = user.copy(
                    passwordHash = hashedNewPassword,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = com.mediaxa.business.suite.data.local.entity.SyncStatus.PENDING_UPDATE.name
                )
                userRepository.updateUser(updatedUser)
                _currentUser.value = updatedUser

                auditLogDao.insertLog(
                    AuditLog(
                        userUuid = user.uuid,
                        username = user.username,
                        action = "CHANGE_PASSWORD",
                        entity = "User",
                        entityId = user.uuid,
                        oldValue = "OLD_PASSWORD_HASHED",
                        newValue = "NEW_PASSWORD_HASHED"
                    )
                )
                onResult("Password berhasil diubah", true)
            } catch (e: Exception) {
                onResult("Terjadi kesalahan: ${e.message}", false)
            }
        }
    }

    fun changeOwnPin(oldPinRaw: String, newPinRaw: String, onResult: (String?, Boolean) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onResult("User belum login", false)
            return
        }
        if (!newPinRaw.matches(Regex("^\\d{4,6}$"))) {
            onResult("PIN baru harus 4-6 digit angka", false)
            return
        }
        viewModelScope.launch {
            try {
                if (!user.pin.isNullOrEmpty()) {
                    val hashedOldPin = AppDatabase.hashString(oldPinRaw)
                    if (user.pin != hashedOldPin) {
                        onResult("PIN lama salah", false)
                        return@launch
                    }
                }
                
                val hashedNewPin = AppDatabase.hashString(newPinRaw)
                val updatedUser = user.copy(
                    pin = hashedNewPin,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = com.mediaxa.business.suite.data.local.entity.SyncStatus.PENDING_UPDATE.name
                )
                userRepository.updateUser(updatedUser)
                _currentUser.value = updatedUser

                auditLogDao.insertLog(
                    AuditLog(
                        userUuid = user.uuid,
                        username = user.username,
                        action = "CHANGE_PIN",
                        entity = "User",
                        entityId = user.uuid,
                        oldValue = "OLD_PIN_HASHED",
                        newValue = "NEW_PIN_HASHED"
                    )
                )
                onResult("PIN berhasil diubah", true)
            } catch (e: Exception) {
                onResult("Terjadi kesalahan: ${e.message}", false)
            }
        }
    }

    fun adminResetPassword(targetUserUuid: String, newPasswordRaw: String, onResult: (String?, Boolean) -> Unit) {
        val admin = _currentUser.value
        if (admin == null || admin.role != "ADMIN") {
            onResult("Hanya admin yang dapat meriset password user lain", false)
            return
        }
        if (newPasswordRaw.length < 6) {
            onResult("Password baru minimal 6 karakter", false)
            return
        }
        viewModelScope.launch {
            try {
                val targetUser = userRepository.getUserByUuid(targetUserUuid)
                if (targetUser == null) {
                    onResult("User tidak ditemukan", false)
                    return@launch
                }
                val hashedNewPassword = AppDatabase.hashString(newPasswordRaw)
                val updatedUser = targetUser.copy(
                    passwordHash = hashedNewPassword,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = com.mediaxa.business.suite.data.local.entity.SyncStatus.PENDING_UPDATE.name
                )
                userRepository.updateUser(updatedUser)

                auditLogDao.insertLog(
                    AuditLog(
                        userUuid = admin.uuid,
                        username = admin.username,
                        action = "CHANGE_PASSWORD",
                        entity = "User",
                        entityId = targetUser.uuid,
                        oldValue = "RESET_BY_ADMIN",
                        newValue = "NEW_PASSWORD_HASHED"
                    )
                )
                onResult("Password kasir berhasil diriset", true)
            } catch (e: Exception) {
                onResult("Terjadi kesalahan: ${e.message}", false)
            }
        }
    }

    fun adminResetPin(targetUserUuid: String, newPinRaw: String, onResult: (String?, Boolean) -> Unit) {
        val admin = _currentUser.value
        if (admin == null || admin.role != "ADMIN") {
            onResult("Hanya admin yang dapat meriset PIN user lain", false)
            return
        }
        if (!newPinRaw.matches(Regex("^\\d{4,6}$"))) {
            onResult("PIN baru harus 4-6 digit angka", false)
            return
        }
        viewModelScope.launch {
            try {
                val targetUser = userRepository.getUserByUuid(targetUserUuid)
                if (targetUser == null) {
                    onResult("User tidak ditemukan", false)
                    return@launch
                }
                val hashedNewPin = AppDatabase.hashString(newPinRaw)
                val updatedUser = targetUser.copy(
                    pin = hashedNewPin,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = com.mediaxa.business.suite.data.local.entity.SyncStatus.PENDING_UPDATE.name
                )
                userRepository.updateUser(updatedUser)

                auditLogDao.insertLog(
                    AuditLog(
                        userUuid = admin.uuid,
                        username = admin.username,
                        action = "CHANGE_PIN",
                        entity = "User",
                        entityId = targetUser.uuid,
                        oldValue = "RESET_BY_ADMIN",
                        newValue = "NEW_PIN_HASHED"
                    )
                )
                onResult("PIN kasir berhasil diriset", true)
            } catch (e: Exception) {
                onResult("Terjadi kesalahan: ${e.message}", false)
            }
        }
    }

    fun adminToggleUserActive(targetUserUuid: String, isActive: Boolean, onResult: (String?, Boolean) -> Unit) {
        val admin = _currentUser.value
        if (admin == null || admin.role != "ADMIN") {
            onResult("Hanya admin yang dapat mengubah status aktif user lain", false)
            return
        }
        viewModelScope.launch {
            try {
                val targetUser = userRepository.getUserByUuid(targetUserUuid)
                if (targetUser == null) {
                    onResult("User tidak ditemukan", false)
                    return@launch
                }
                if (targetUser.uuid == admin.uuid) {
                    onResult("Tidak dapat menonaktifkan akun sendiri", false)
                    return@launch
                }
                val updatedUser = targetUser.copy(
                    isActive = isActive,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = com.mediaxa.business.suite.data.local.entity.SyncStatus.PENDING_UPDATE.name
                )
                userRepository.updateUser(updatedUser)

                auditLogDao.insertLog(
                    AuditLog(
                        userUuid = admin.uuid,
                        username = admin.username,
                        action = if (isActive) "ACTIVATE_USER" else "DEACTIVATE_USER",
                        entity = "User",
                        entityId = targetUser.uuid,
                        oldValue = targetUser.isActive.toString(),
                        newValue = isActive.toString()
                    )
                )
                val statusText = if (isActive) "diaktifkan" else "dinonaktifkan"
                onResult("User berhasil $statusText", true)
            } catch (e: Exception) {
                onResult("Terjadi kesalahan: ${e.message}", false)
            }
        }
    }

    class Factory(
        private val repository: UserRepository,
        private val auditLogDao: AuditLogDao,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(repository, auditLogDao, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
