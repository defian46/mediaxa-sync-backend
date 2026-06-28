package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuthenticationTest {

    @Test
    fun testPasswordHashing() {
        val rawPassword = "admin123"
        val hashed1 = AppDatabase.hashString(rawPassword)
        val hashed2 = AppDatabase.hashString(rawPassword)

        assertEquals("Hashes of same password should match", hashed1, hashed2)
        assertEquals("SHA-256 length should be 64 characters", 64, hashed1.length)
        
        val rawPassword2 = "kasir123"
        val hashed3 = AppDatabase.hashString(rawPassword2)
        assertNotEquals("Hashes of different passwords should not match", hashed1, hashed3)
    }
}
