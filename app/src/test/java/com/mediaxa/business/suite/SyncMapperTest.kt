package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.remote.mapper.*
import com.mediaxa.business.suite.data.remote.dto.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [SyncMappers] — verifies correct field mapping between entities and DTOs.
 */
class SyncMapperTest {

    @Test
    fun transactionToDtoMapsAllFieldsCorrectly() {
        val entity = Transaction(
            localId = 1,
            uuid = "txn-uuid",
            storeId = 2L,
            deviceId = "DEV-ABC",
            updatedAt = 999L,
            transactionNumber = "TRX-0001",
            cashierUuid = "cashier-1",
            cashierName = "Budi",
            discount = 5000.0,
            subtotal = 100000.0,
            total = 95000.0,
            transactionHpp = 40000.0,
            grossProfit = 55000.0,
            paymentMethod = "CASH",
            amountReceived = 100000.0,
            changeAmount = 5000.0,
            status = "PAID",
            customerUuid = "cust-1",
            pointsEarned = 9,
            pointsRedeemed = 0
        )
        val dto = entity.toDto()

        assertEquals(entity.uuid, dto.uuid)
        assertEquals(entity.storeId, dto.storeId)
        assertEquals(entity.deviceId, dto.deviceId)
        assertEquals(entity.transactionNumber, dto.transactionNumber)
        assertEquals(entity.cashierUuid, dto.cashierUuid)
        assertEquals(entity.discount, dto.discount, 0.001)
        assertEquals(entity.total, dto.total, 0.001)
        assertEquals(entity.transactionHpp, dto.transactionHpp, 0.001)
        assertEquals(entity.customerUuid, dto.customerUuid)
        assertEquals(entity.pointsEarned, dto.pointsEarned)
        assertEquals(entity.updatedAt, dto.updatedAt)
    }

    @Test
    fun menuToDtoMapsAllFieldsCorrectly() {
        val entity = Menu(
            uuid = "menu-uuid",
            storeId = 1L,
            deviceId = "DEV-01",
            name = "Kopi Hitam",
            categoryUuid = "cat-1",
            price = 15000.0,
            promoPrice = 12000.0,
            isActive = true,
            description = "Kopi arabica pilihan",
            estimatedHpp = 5000.0,
            estimatedMargin = 0.67,
            updatedAt = 12345L
        )
        val dto = entity.toDto()

        assertEquals(entity.uuid, dto.uuid)
        assertEquals(entity.name, dto.name)
        assertEquals(entity.categoryUuid, dto.categoryUuid)
        assertEquals(entity.price, dto.price, 0.001)
        assertEquals(entity.promoPrice, dto.promoPrice)
        assertEquals(entity.isActive, dto.isActive)
        assertEquals(entity.estimatedHpp, dto.estimatedHpp, 0.001)
        assertEquals(entity.updatedAt, dto.updatedAt)
    }

    @Test
    fun ingredientToDtoMapsAllFieldsCorrectly() {
        val entity = Ingredient(
            uuid = "ing-uuid",
            storeId = 1L,
            deviceId = "DEV-01",
            name = "Gula Pasir",
            unit = "kg",
            purchasePrice = 14000.0,
            packageSize = 1.0,
            unitPrice = 14000.0,
            availableStock = 5.0,
            minStock = 1.0,
            updatedAt = 54321L
        )
        val dto = entity.toDto()

        assertEquals(entity.uuid, dto.uuid)
        assertEquals(entity.name, dto.name)
        assertEquals(entity.unit, dto.unit)
        assertEquals(entity.purchasePrice, dto.purchasePrice, 0.001)
        assertEquals(entity.availableStock, dto.availableStock, 0.001)
        assertEquals(entity.minStock, dto.minStock, 0.001)
        assertEquals(entity.updatedAt, dto.updatedAt)
    }

    @Test
    fun customerToDtoConvertsIsDeletedIntToBoolean() {
        val activeCustomer = Customer(
            uuid = "cust-1",
            customerCode = "C001",
            customerName = "Siti",
            phone = "0812",
            email = null,
            birthday = null,
            gender = null,
            address = null,
            notes = null,
            isDeleted = 0
        )
        val deletedCustomer = activeCustomer.copy(isDeleted = 1)

        assertFalse(activeCustomer.toDto().isDeleted)
        assertTrue(deletedCustomer.toDto().isDeleted)
    }

    @Test
    fun expenseToDtoMapsAllRequiredFields() {
        val entity = Expense(
            uuid = "exp-uuid",
            storeId = 1L,
            deviceId = "DEV-01",
            expenseDate = 1000L,
            category = "RENT",
            amount = 500000.0,
            notes = "Bayar sewa bulan Juni",
            userUuid = "user-1",
            paymentMethod = "TRANSFER",
            updatedAt = 2000L
        )
        val dto = entity.toDto()

        assertEquals(entity.uuid, dto.uuid)
        assertEquals(entity.category, dto.category)
        assertEquals(entity.amount, dto.amount, 0.001)
        assertEquals(entity.paymentMethod, dto.paymentMethod)
        assertEquals(entity.notes, dto.notes)
        assertEquals(entity.updatedAt, dto.updatedAt)
    }

    @Test
    fun stockMovementToDtoMapsMovementTypeAndIngredientUuid() {
        val entity = StockMovement(
            uuid = "mov-uuid",
            storeId = 1L,
            deviceId = "DEV-01",
            ingredientUuid = "ing-1",
            quantity = 2.5,
            type = "OUT",
            note = "Penjualan",
            userUuid = "user-1",
            updatedAt = 3000L
        )
        val dto = entity.toDto()

        assertEquals(entity.ingredientUuid, dto.ingredientUuid)
        assertEquals(entity.type, dto.movementType)
        assertEquals(entity.quantity, dto.quantity, 0.001)
        assertEquals(entity.note, dto.notes)
    }
}
