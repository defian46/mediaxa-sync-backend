package com.mediaxa.business.suite.data.repository

import androidx.room.withTransaction
import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.domain.model.CartItem
import java.text.SimpleDateFormat
import java.util.*

sealed class StockValidationResult {
    object Valid : StockValidationResult()
    data class LackingIngredients(val list: List<LackingInfo>) : StockValidationResult()
}
data class LackingInfo(
    val ingredientName: String, 
    val required: Double, 
    val available: Double, 
    val unit: String
)

sealed class CheckoutResult {
    data class Success(val transactionUuid: String) : CheckoutResult()
    data class Failure(val errorMsg: String) : CheckoutResult()
}

interface TransactionRunner {
    suspend fun <R> run(block: suspend () -> R): R
}

class RoomTransactionRunner(private val db: AppDatabase) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R {
        return db.withTransaction(block)
    }
}

class CheckoutService(
    private val database: AppDatabase,
    private val localDataSource: LocalDataSource,
    private val transactionRunner: TransactionRunner = RoomTransactionRunner(database)
) {

    suspend fun validateStock(cart: List<CartItem>): StockValidationResult {
        if (cart.isEmpty()) return StockValidationResult.Valid

        // Batch fetch recipes and ingredients to prevent N+1 queries
        val menuUuids = cart.map { it.menu.uuid }.distinct()
        val allRecipes = localDataSource.menuRecipeDao.getRecipesForMenus(menuUuids)
        val recipesByMenu = allRecipes.groupBy { it.menuUuid }

        val ingredientRequirements = mutableMapOf<String, Double>()
        for (cartItem in cart) {
            val recipes = recipesByMenu[cartItem.menu.uuid] ?: emptyList()
            for (recipe in recipes) {
                val totalNeeded = recipe.quantityNeeded * cartItem.quantity
                ingredientRequirements[recipe.ingredientUuid] = 
                    ingredientRequirements.getOrDefault(recipe.ingredientUuid, 0.0) + totalNeeded
            }
        }

        val ingredientUuids = ingredientRequirements.keys.toList()
        val ingredientsList = if (ingredientUuids.isNotEmpty()) {
            localDataSource.ingredientDao.getIngredientsByUuids(ingredientUuids)
        } else {
            emptyList()
        }
        val ingredientsMap = ingredientsList.associateBy { it.uuid }

        val lackingList = mutableListOf<LackingInfo>()
        for ((ingUuid, reqQty) in ingredientRequirements) {
            val ingredient = ingredientsMap[ingUuid]
            if (ingredient == null) {
                lackingList.add(LackingInfo("Bahan Baku Tidak Ditemukan", reqQty, 0.0, ""))
                continue
            }
            if (ingredient.availableStock < reqQty) {
                lackingList.add(
                    LackingInfo(
                        ingredientName = ingredient.name,
                        required = reqQty,
                        available = ingredient.availableStock,
                        unit = ingredient.unit
                    )
                )
            }
        }

        return if (lackingList.isEmpty()) {
            StockValidationResult.Valid
        } else {
            StockValidationResult.LackingIngredients(lackingList)
        }
    }

    suspend fun executeCheckout(
        cart: List<CartItem>,
        discount: Double,
        cashierUuid: String,
        cashierName: String,
        paymentMethod: String,
        amountReceived: Double,
        storeId: Long = 1,
        deviceId: String = "DEV-01",
        customerUuid: String? = null,
        pointsToRedeem: Int? = null
    ): CheckoutResult {
        if (cart.isEmpty()) {
            return CheckoutResult.Failure("Keranjang kosong")
        }

        return try {
            transactionRunner.run {
                // 1. Validate stock INSIDE the database transaction block (holds locks and prevents concurrent race conditions)
                val menuUuids = cart.map { it.menu.uuid }.distinct()
                val allRecipes = localDataSource.menuRecipeDao.getRecipesForMenus(menuUuids)
                val recipesByMenu = allRecipes.groupBy { it.menuUuid }

                val ingredientRequirements = mutableMapOf<String, Double>()
                for (cartItem in cart) {
                    val recipes = recipesByMenu[cartItem.menu.uuid] ?: emptyList()
                    for (recipe in recipes) {
                        val totalNeeded = recipe.quantityNeeded * cartItem.quantity
                        ingredientRequirements[recipe.ingredientUuid] = 
                            ingredientRequirements.getOrDefault(recipe.ingredientUuid, 0.0) + totalNeeded
                    }
                }

                val ingredientUuids = ingredientRequirements.keys.toList()
                val ingredientsList = if (ingredientUuids.isNotEmpty()) {
                    localDataSource.ingredientDao.getIngredientsByUuids(ingredientUuids)
                } else {
                    emptyList()
                }
                val ingredientsMap = ingredientsList.associateBy { it.uuid }

                val lackingList = mutableListOf<LackingInfo>()
                for ((ingUuid, reqQty) in ingredientRequirements) {
                    val ingredient = ingredientsMap[ingUuid]
                    if (ingredient == null) {
                        lackingList.add(LackingInfo("Bahan Baku Tidak Ditemukan", reqQty, 0.0, ""))
                        continue
                    }
                    if (ingredient.availableStock < reqQty) {
                        lackingList.add(
                            LackingInfo(
                                ingredientName = ingredient.name,
                                required = reqQty,
                                available = ingredient.availableStock,
                                unit = ingredient.unit
                            )
                        )
                    }
                }

                if (lackingList.isNotEmpty()) {
                    val lackMsg = lackingList.joinToString(", ") { 
                        "${it.ingredientName} (Butuh ${it.required} ${it.unit}, Tersedia ${it.available} ${it.unit})" 
                    }
                    throw IllegalStateException("Stok tidak cukup: $lackMsg")
                }

                // 2. Load Store Settings for Taxes
                val settings = localDataSource.storeSettingDao.getSettings()
                val isTaxEnabled = settings?.isTaxEnabled == true
                val isServiceChargeEnabled = settings?.isServiceChargeEnabled == true

                val subtotal = cart.sumOf { it.menu.price * it.quantity }
                val pointsPerAmount = settings?.loyaltyPointsPerAmount ?: 10000.0
                val pointsValue = settings?.loyaltyPointsValue ?: 100.0
                val ptsRedeemed = pointsToRedeem ?: 0
                val pointsDiscount = ptsRedeemed * pointsValue

                val subtotalAfterDiscount = (subtotal - discount - pointsDiscount).coerceAtLeast(0.0)
                val tax = if (isTaxEnabled) subtotalAfterDiscount * 0.10 else 0.0
                val serviceCharge = if (isServiceChargeEnabled) subtotalAfterDiscount * 0.05 else 0.0
                val total = subtotalAfterDiscount + tax + serviceCharge
                
                if (paymentMethod == "CASH" && amountReceived < total) {
                    throw IllegalStateException("Uang diterima kurang")
                }

                var totalHpp = 0.0
                val txUuid = UUID.randomUUID().toString()

                // Generate order ID TRX-YYYYMMDD-0001
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val dateStr = dateFormat.format(Date())
                val count = localDataSource.transactionDao.getTransactionCount()
                val transactionNumber = "TRX-$dateStr-${String.format("%04d", count + 1)}"

                val change = if (paymentMethod == "CASH") {
                    amountReceived - total
                } else {
                    0.0
                }

                // Map items and calculate item-level HPP snapshots (Phase 3.5)
                val txItems = mutableListOf<TransactionItem>()
                for (cartItem in cart) {
                    val recipes = recipesByMenu[cartItem.menu.uuid] ?: emptyList()
                    var itemHppUnit = 0.0
                    for (recipe in recipes) {
                        val ingredient = ingredientsMap[recipe.ingredientUuid]
                        if (ingredient != null) {
                            itemHppUnit += ingredient.unitPrice * recipe.quantityNeeded
                        }
                    }
                    val itemTotalHpp = itemHppUnit * cartItem.quantity
                    totalHpp += itemTotalHpp

                    val itemSubtotal = cartItem.menu.price * cartItem.quantity
                    val costPrice = itemHppUnit
                    val sellingPrice = cartItem.menu.price
                    val itemGrossProfit = itemSubtotal - itemTotalHpp
                    val marginPercent = if (sellingPrice > 0) ((sellingPrice - costPrice) / sellingPrice) * 100 else 0.0

                    txItems.add(
                        TransactionItem(
                            uuid = UUID.randomUUID().toString(),
                            storeId = storeId,
                            deviceId = deviceId,
                            transactionUuid = txUuid,
                            menuUuid = cartItem.menu.uuid,
                            menuName = cartItem.menu.name,
                            quantity = cartItem.quantity,
                            price = cartItem.menu.price,
                            subtotal = itemSubtotal,
                            sellingPrice = sellingPrice,
                            costPrice = costPrice,
                            grossProfit = itemGrossProfit,
                            marginPercent = marginPercent,
                            syncStatus = SyncStatus.PENDING_CREATE.name
                        )
                    )
                }

                val grossProfit = total - totalHpp

                // Calculate points earned/redeemed if customer is selected
                var pointsEarned = 0
                if (customerUuid != null) {
                    val customer = localDataSource.customerDao.getCustomerByUuid(customerUuid)
                    if (customer != null) {
                        // Check if points redemption is valid
                        if (ptsRedeemed > 0) {
                            val currentBalance = localDataSource.loyaltyDao.getPointsBalance(storeId, customerUuid)
                            if (currentBalance < ptsRedeemed) {
                                throw IllegalStateException("Poin tidak mencukupi. Saldo: $currentBalance, Diminta: $ptsRedeemed")
                            }
                            
                            // Insert negative ledger entry
                            val redeemHistory = LoyaltyPointHistory(
                                storeId = storeId,
                                deviceId = deviceId,
                                customerUuid = customerUuid,
                                transactionUuid = txUuid,
                                points = -ptsRedeemed,
                                activityType = "REDEEMED",
                                notes = "Redeem $ptsRedeemed poin untuk diskon transaksi $transactionNumber"
                            )
                            localDataSource.loyaltyDao.insertPointHistory(redeemHistory)
                        }
                        
                        // Calculate points earned
                        pointsEarned = (subtotalAfterDiscount / pointsPerAmount).toInt()
                        if (pointsEarned > 0) {
                            val earnHistory = LoyaltyPointHistory(
                                storeId = storeId,
                                deviceId = deviceId,
                                customerUuid = customerUuid,
                                transactionUuid = txUuid,
                                points = pointsEarned,
                                activityType = "EARNED",
                                notes = "Dapatkan $pointsEarned poin dari transaksi $transactionNumber"
                            )
                            localDataSource.loyaltyDao.insertPointHistory(earnHistory)
                        }
                    }
                }

                // 1. Save Transaction record
                val transaction = Transaction(
                    uuid = txUuid,
                    storeId = storeId,
                    deviceId = deviceId,
                    transactionNumber = transactionNumber,
                    cashierUuid = cashierUuid,
                    cashierName = cashierName,
                    discount = discount + pointsDiscount,
                    subtotal = subtotal,
                    total = total,
                    transactionHpp = totalHpp,
                    grossProfit = grossProfit,
                    paymentMethod = paymentMethod,
                    amountReceived = if (paymentMethod == "CASH") amountReceived else total,
                    changeAmount = change,
                    status = "PAID",
                    customerUuid = customerUuid,
                    pointsEarned = if (pointsEarned > 0) pointsEarned else null,
                    pointsRedeemed = if (ptsRedeemed > 0) ptsRedeemed else null,
                    syncStatus = SyncStatus.PENDING_CREATE.name
                )
                localDataSource.transactionDao.insertTransaction(transaction)

                // 2. Save items & payments
                localDataSource.transactionItemDao.insertTransactionItems(txItems)

                val payment = Payment(
                    uuid = UUID.randomUUID().toString(),
                    storeId = storeId,
                    deviceId = deviceId,
                    transactionUuid = txUuid,
                    method = paymentMethod,
                    amount = total,
                    syncStatus = SyncStatus.PENDING_CREATE.name
                )
                localDataSource.paymentDao.insertPayments(listOf(payment))

                // 3. Deduct stock and write movements
                for (cartItem in cart) {
                    val recipes = recipesByMenu[cartItem.menu.uuid] ?: emptyList()
                    for (recipe in recipes) {
                        val qtyDeducted = recipe.quantityNeeded * cartItem.quantity
                        
                        // Deduct stock in ingredients table
                        localDataSource.ingredientDao.adjustStock(
                            uuid = recipe.ingredientUuid,
                            amount = -qtyDeducted,
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING_UPDATE.name
                        )

                        // Insert StockMovement SALES_USAGE
                        val movement = StockMovement(
                            uuid = UUID.randomUUID().toString(),
                            storeId = storeId,
                            deviceId = deviceId,
                            ingredientUuid = recipe.ingredientUuid,
                            quantity = -qtyDeducted,
                            type = "SALES_USAGE",
                            note = "Transaksi $transactionNumber",
                            userUuid = cashierUuid,
                            syncStatus = SyncStatus.PENDING_CREATE.name
                        )
                        localDataSource.stockMovementDao.insertMovement(movement)
                    }
                }

                // Update customer aggregates if customer is selected
                if (customerUuid != null) {
                    val customer = localDataSource.customerDao.getCustomerByUuid(customerUuid)
                    if (customer != null) {
                        val newTotalSpending = customer.totalSpending + total
                        val newLastVisit = System.currentTimeMillis()
                        
                        val updatedCustTemp = customer.copy(
                            totalSpending = newTotalSpending,
                            lastVisit = newLastVisit,
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = "PENDING_UPDATE"
                        )
                        localDataSource.customerDao.updateCustomer(updatedCustTemp)
                        
                        // Calculate favoriteMenuUuid
                        val favMenuUuid = localDataSource.customerDao.getFavoriteMenuUuid(storeId, customerUuid)
                        val finalUpdatedCust = updatedCustTemp.copy(
                            favoriteMenuUuid = favMenuUuid
                        )
                        localDataSource.customerDao.updateCustomer(finalUpdatedCust)
                    }
                }

                // 4. Save Audit Log for Transaction
                val auditLog = AuditLog(
                    userUuid = cashierUuid,
                    username = cashierName,
                    action = "CHECKOUT",
                    entity = "Transaction",
                    entityId = txUuid,
                    oldValue = null,
                    newValue = "TRX NO: $transactionNumber, TOTAL: $total"
                )
                localDataSource.auditLogDao.insertLog(auditLog)

                CheckoutResult.Success(txUuid)
            }
        } catch (e: Exception) {
            CheckoutResult.Failure("Transaksi gagal: ${e.message}")
        }
    }
}
