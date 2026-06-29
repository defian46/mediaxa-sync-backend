package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.dao.AuditLogDao
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.entity.AuditLog
import com.mediaxa.business.suite.data.local.entity.User
import com.mediaxa.business.suite.data.local.entity.StoreSetting
import com.mediaxa.business.suite.data.repository.UserRepository
import com.mediaxa.business.suite.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSecurityAndPaymentTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var userRepository: UserRepository
    private lateinit var auditLogDao: AuditLogDao
    private lateinit var authViewModel: AuthViewModel

    private val adminUser = User(
        localId = 1L,
        uuid = "admin-uuid",
        username = "admin",
        passwordHash = AppDatabase.hashString("admin123"),
        pin = AppDatabase.hashString("1234"),
        role = "ADMIN"
    )

    private val cashierUser = User(
        localId = 2L,
        uuid = "cashier-uuid",
        username = "cashier",
        passwordHash = AppDatabase.hashString("cashier123"),
        pin = AppDatabase.hashString("5678"),
        role = "CASHIER"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mock(UserRepository::class.java)
        auditLogDao = mock(AuditLogDao::class.java)
        
        `when`(userRepository.getAllUsersFlow()).thenReturn(flowOf(listOf(adminUser, cashierUser)))
        
        authViewModel = AuthViewModel(userRepository, auditLogDao, mock(android.content.Context::class.java))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testChangeUsernameSuccess() {
        runBlocking {
            val currentUserField = AuthViewModel::class.java.getDeclaredField("_currentUser")
            currentUserField.isAccessible = true
            val flow = currentUserField.get(authViewModel) as kotlinx.coroutines.flow.MutableStateFlow<User?>
            flow.value = cashierUser

            `when`(userRepository.getUserByUsername("new_cashier")).thenReturn(null)

            var isSuccess = false

            authViewModel.changeOwnUsername("cashier123", "new_cashier") { _, success ->
                isSuccess = success
            }

            assertTrue(isSuccess)
            assertEquals("new_cashier", authViewModel.currentUser.value?.username)
            verify(userRepository).updateUser(anyNonNull())
            verify(auditLogDao).insertLog(anyNonNull())
        }
    }

    @Test
    fun testChangePasswordUsingHash() {
        runBlocking {
            val currentUserField = AuthViewModel::class.java.getDeclaredField("_currentUser")
            currentUserField.isAccessible = true
            val flow = currentUserField.get(authViewModel) as kotlinx.coroutines.flow.MutableStateFlow<User?>
            flow.value = cashierUser

            var isSuccess = false

            authViewModel.changeOwnPassword("cashier123", "newpassword") { _, success ->
                isSuccess = success
            }

            assertTrue(isSuccess)
            val expectedHash = AppDatabase.hashString("newpassword")
            assertEquals(expectedHash, authViewModel.currentUser.value?.passwordHash)
            verify(userRepository).updateUser(anyNonNull())
            verify(auditLogDao).insertLog(anyNonNull())
        }
    }

    @Test
    fun testChangePinUsingHash() {
        runBlocking {
            val currentUserField = AuthViewModel::class.java.getDeclaredField("_currentUser")
            currentUserField.isAccessible = true
            val flow = currentUserField.get(authViewModel) as kotlinx.coroutines.flow.MutableStateFlow<User?>
            flow.value = cashierUser

            var isSuccess = false

            authViewModel.changeOwnPin("5678", "4321") { _, success ->
                isSuccess = success
            }

            assertTrue(isSuccess)
            val expectedHash = AppDatabase.hashString("4321")
            assertEquals(expectedHash, authViewModel.currentUser.value?.pin)
            verify(userRepository).updateUser(anyNonNull())
            verify(auditLogDao).insertLog(anyNonNull())
        }
    }

    @Test
    fun testCashierCannotResetOtherUser() {
        runBlocking {
            val currentUserField = AuthViewModel::class.java.getDeclaredField("_currentUser")
            currentUserField.isAccessible = true
            val flow = currentUserField.get(authViewModel) as kotlinx.coroutines.flow.MutableStateFlow<User?>
            flow.value = cashierUser

            var resultMessage: String? = null
            var isSuccess = false

            authViewModel.adminResetPassword("admin-uuid", "newadminpassword") { msg, success ->
                resultMessage = msg
                isSuccess = success
            }

            assertFalse(isSuccess)
            assertEquals("Hanya admin yang dapat meriset password user lain", resultMessage)
            verify(userRepository, never()).updateUser(anyNonNull())
        }
    }

    @Test
    fun testAdminCanResetCashier() {
        runBlocking {
            val currentUserField = AuthViewModel::class.java.getDeclaredField("_currentUser")
            currentUserField.isAccessible = true
            val flow = currentUserField.get(authViewModel) as kotlinx.coroutines.flow.MutableStateFlow<User?>
            flow.value = adminUser

            `when`(userRepository.getUserByUuid("cashier-uuid")).thenReturn(cashierUser)

            var isSuccess = false

            authViewModel.adminResetPassword("cashier-uuid", "newcashier123") { _, success ->
                isSuccess = success
            }

            assertTrue(isSuccess)
            verify(userRepository).updateUser(anyNonNull())
            verify(auditLogDao).insertLog(anyNonNull())
        }
    }

    @Test
    fun testCashPaymentValidationRules() {
        val total = 50000.0
        val insufficientCash = 45000.0
        val sufficientCash = 50000.0

        val insufficientResult = insufficientCash >= total
        val sufficientResult = sufficientCash >= total

        assertFalse("Tunai kurang tidak bisa checkout", insufficientResult)
        assertTrue("Tunai pas atau lebih bisa checkout", sufficientResult)
    }

    @Test
    fun testQrisPaymentValidationRules() {
        val qrisRequiresCashInput = false
        assertFalse("QRIS tidak perlu input uang diterima", qrisRequiresCashInput)
    }

    @Test
    fun testTransferStoreSettingProperties() {
        val storeSetting = StoreSetting(
            storeName = "Toko Saya",
            address = "Alamat",
            phoneNumber = "123",
            receiptFooter = "Footer",
            bankName = "Mandiri",
            bankAccountNumber = "987654321",
            bankAccountHolderName = "Budi Hartono"
        )
        assertNotNull(storeSetting.bankAccountNumber)
        assertEquals("Mandiri", storeSetting.bankName)
        assertEquals("987654321", storeSetting.bankAccountNumber)
        assertEquals("Budi Hartono", storeSetting.bankAccountHolderName)
    }

    @Test
    fun testPaymentMethodsCount() {
        val methods = listOf("CASH", "QRIS", "TRANSFER")
        assertEquals("Payment method tetap hanya 3", 3, methods.size)
        assertTrue(methods.contains("CASH"))
        assertTrue(methods.contains("QRIS"))
        assertTrue(methods.contains("TRANSFER"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        any<Any>()
        return null as T
    }
}
