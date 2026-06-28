package com.mediaxa.business.suite.data.local.datasource

import com.mediaxa.business.suite.data.local.database.AppDatabase

class LocalDataSource(private val db: AppDatabase) {
    val userDao = db.userDao()
    val storeSettingDao = db.storeSettingDao()
    val categoryDao = db.categoryDao()
    val menuDao = db.menuDao()
    val ingredientDao = db.ingredientDao()
    val menuRecipeDao = db.menuRecipeDao()
    val stockMovementDao = db.stockMovementDao()
    val transactionDao = db.transactionDao()
    val transactionItemDao = db.transactionItemDao()
    val paymentDao = db.paymentDao()
    val voidLogDao = db.voidLogDao()
    val backupLogDao = db.backupLogDao()
    val auditLogDao = db.auditLogDao()
    val salesSummaryDao = db.salesSummaryDao()
    val inventoryLiteDao = db.inventoryLiteDao()
    val financeDao = db.financeDao()
    val customerDao = db.customerDao()
    val loyaltyDao = db.loyaltyDao()
    val promotionDao = db.promotionDao()
    val syncQueueDao = db.syncQueueDao()  // Phase 9: Cloud sync outbox
}

