package com.mediaxa.business.suite.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mediaxa.business.suite.data.local.dao.*
import com.mediaxa.business.suite.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Database(
    entities = [
        User::class,
        StoreSetting::class,
        Category::class,
        Menu::class,
        Ingredient::class,
        MenuRecipe::class,
        StockMovement::class,
        Transaction::class,
        TransactionItem::class,
        Payment::class,
        VoidLog::class,
        BackupLog::class,
        AuditLog::class,
        Expense::class,
        PurchaseExpense::class,
        PurchaseExpenseItem::class,
        StockOpname::class,
        StockOpnameItem::class,
        WasteLog::class,
        CashShift::class,
        DailyClosing::class,
        Customer::class,
        LoyaltyPointHistory::class,
        PromotionRule::class,
        SyncQueueItem::class  // Phase 9: Offline-First sync outbox
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun storeSettingDao(): StoreSettingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun menuDao(): MenuDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun menuRecipeDao(): MenuRecipeDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun voidLogDao(): VoidLogDao
    abstract fun backupLogDao(): BackupLogDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun salesSummaryDao(): SalesSummaryDao
    abstract fun inventoryLiteDao(): InventoryLiteDao
    abstract fun financeDao(): FinanceDao
    abstract fun customerDao(): CustomerDao
    abstract fun loyaltyDao(): LoyaltyDao
    abstract fun promotionDao(): PromotionDao
    abstract fun syncQueueDao(): SyncQueueDao  // Phase 9

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create expenses
                db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `lastSyncedAt` INTEGER, `expenseDate` INTEGER NOT NULL, `category` TEXT NOT NULL, `amount` REAL NOT NULL, `notes` TEXT, `userUuid` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_uuid` ON `expenses` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_storeId_expenseDate` ON `expenses` (`storeId`, `expenseDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")

                // 2. Create purchase_expenses
                db.execSQL("CREATE TABLE IF NOT EXISTS `purchase_expenses` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `lastSyncedAt` INTEGER, `purchaseDate` INTEGER NOT NULL, `purchasePlaceName` TEXT, `paymentMethod` TEXT NOT NULL, `notes` TEXT, `totalAmount` REAL NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_expenses_uuid` ON `purchase_expenses` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_expenses_storeId_purchaseDate` ON `purchase_expenses` (`storeId`, `purchaseDate`)")

                // 3. Create purchase_expense_items
                db.execSQL("CREATE TABLE IF NOT EXISTS `purchase_expense_items` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `lastSyncedAt` INTEGER, `purchaseExpenseUuid` TEXT NOT NULL, `ingredientUuid` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `totalPrice` REAL NOT NULL, `unitPrice` REAL NOT NULL, `batchNumber` TEXT, `expiredDate` INTEGER, FOREIGN KEY(`purchaseExpenseUuid`) REFERENCES `purchase_expenses`(`uuid`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_expense_items_uuid` ON `purchase_expense_items` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_expense_items_purchaseExpenseUuid` ON `purchase_expense_items` (`purchaseExpenseUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_expense_items_ingredientUuid` ON `purchase_expense_items` (`ingredientUuid`)")

                // 4. Create stock_opnames
                db.execSQL("CREATE TABLE IF NOT EXISTS `stock_opnames` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `lastSyncedAt` INTEGER, `opnameDate` INTEGER NOT NULL, `userUuid` TEXT NOT NULL, `notes` TEXT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_opnames_uuid` ON `stock_opnames` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_opnames_storeId_opnameDate` ON `stock_opnames` (`storeId`, `opnameDate`)")

                // 5. Create stock_opname_items
                db.execSQL("CREATE TABLE IF NOT EXISTS `stock_opname_items` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `lastSyncedAt` INTEGER, `opnameUuid` TEXT NOT NULL, `ingredientUuid` TEXT NOT NULL, `systemStock` REAL NOT NULL, `physicalStock` REAL NOT NULL, `diffStock` REAL NOT NULL, `notes` TEXT NOT NULL, FOREIGN KEY(`opnameUuid`) REFERENCES `stock_opnames`(`uuid`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_opname_items_uuid` ON `stock_opname_items` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_opname_items_opnameUuid` ON `stock_opname_items` (`opnameUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_opname_items_ingredientUuid` ON `stock_opname_items` (`ingredientUuid`)")

                // 6. Create waste_logs
                db.execSQL("CREATE TABLE IF NOT EXISTS `waste_logs` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `lastSyncedAt` INTEGER, `wasteDate` INTEGER NOT NULL, `ingredientUuid` TEXT NOT NULL, `quantity` REAL NOT NULL, `reason` TEXT NOT NULL, `calculatedCost` REAL NOT NULL, `userUuid` TEXT NOT NULL, `notes` TEXT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_waste_logs_uuid` ON `waste_logs` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_waste_logs_storeId_wasteDate` ON `waste_logs` (`storeId`, `wasteDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_waste_logs_ingredientUuid` ON `waste_logs` (`ingredientUuid`)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add attachmentPath to expenses table
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `attachmentPath` TEXT")

                // 2. Create cash_shifts table
                db.execSQL("CREATE TABLE IF NOT EXISTS `cash_shifts` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `cashierUuid` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `openingCash` REAL NOT NULL, `closingCash` REAL, `expectedCash` REAL, `actualCash` REAL, `cashDifference` REAL, `status` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_shifts_uuid` ON `cash_shifts` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_shifts_storeId_status` ON `cash_shifts` (`storeId`, `status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_shifts_storeId_startTime` ON `cash_shifts` (`storeId`, `startTime`)")

                // 3. Create daily_closings table
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_closings` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `dateStr` TEXT NOT NULL, `openingBalance` REAL NOT NULL, `revenue` REAL NOT NULL, `hpp` REAL NOT NULL, `grossProfit` REAL NOT NULL, `operationalExpense` REAL NOT NULL, `wasteCost` REAL NOT NULL, `netProfit` REAL NOT NULL, `cashInflow` REAL NOT NULL, `cashOutflow` REAL NOT NULL, `closingBalance` REAL NOT NULL, `cashRevenue` REAL NOT NULL, `qrisRevenue` REAL NOT NULL, `transferRevenue` REAL NOT NULL, `totalTransactions` INTEGER NOT NULL, `averageTicket` REAL NOT NULL, `closedByUserUuid` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_closings_uuid` ON `daily_closings` (`uuid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_closings_storeId_dateStr` ON `daily_closings` (`storeId`, `dateStr`)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create customers table
                db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `customerCode` TEXT NOT NULL, `customerName` TEXT NOT NULL, `phone` TEXT, `email` TEXT, `birthday` INTEGER, `gender` TEXT, `address` TEXT, `notes` TEXT, `joinDate` INTEGER NOT NULL, `membershipLevel` TEXT NOT NULL, `totalSpending` REAL NOT NULL, `lastVisit` INTEGER, `favoriteMenuUuid` TEXT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_uuid` ON `customers` (`uuid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_storeId_customerCode` ON `customers` (`storeId`, `customerCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_storeId_phone` ON `customers` (`storeId`, `phone`)")

                // 2. Create loyalty_point_history table
                db.execSQL("CREATE TABLE IF NOT EXISTS `loyalty_point_history` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `customerUuid` TEXT NOT NULL, `transactionUuid` TEXT, `points` INTEGER NOT NULL, `activityType` TEXT NOT NULL, `expiryTime` INTEGER, `notes` TEXT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_loyalty_point_history_uuid` ON `loyalty_point_history` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loyalty_point_history_storeId_customerUuid` ON `loyalty_point_history` (`storeId`, `customerUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loyalty_point_history_transactionUuid` ON `loyalty_point_history` (`transactionUuid`)")

                // 3. Create promotion_rules table
                db.execSQL("CREATE TABLE IF NOT EXISTS `promotion_rules` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `storeId` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `name` TEXT NOT NULL, `promoType` TEXT NOT NULL, `value` REAL NOT NULL, `buyMenuUuid` TEXT, `buyQuantity` INTEGER, `getMenuUuid` TEXT, `getQuantity` INTEGER, `minPurchaseAmount` REAL, `isActive` INTEGER NOT NULL, `startDate` INTEGER, `endDate` INTEGER, `startHour` INTEGER, `endHour` INTEGER, `applicableDays` TEXT, `targetMembershipLevels` TEXT, `targetCategoryUuid` TEXT, `targetMenuUuid` TEXT, `promoCode` TEXT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_promotion_rules_uuid` ON `promotion_rules` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_promotion_rules_storeId_isActive` ON `promotion_rules` (`storeId`, `isActive`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_promotion_rules_promoCode` ON `promotion_rules` (`promoCode`)")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add loyalty columns to transactions
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `customerUuid` TEXT")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `pointsEarned` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `pointsRedeemed` INTEGER")

                // 2. Add loyalty settings to store_settings
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `loyaltyPointsPerAmount` REAL NOT NULL DEFAULT 10000.0")
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `loyaltyPointsValue` REAL NOT NULL DEFAULT 100.0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Phase 9: Create sync_queue outbox table for Offline-First cloud sync
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `uuid` TEXT NOT NULL,
                        `storeId` INTEGER NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `maxRetries` INTEGER NOT NULL DEFAULT 5,
                        `nextRetryAt` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `lastAttemptAt` INTEGER,
                        `errorMessage` TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status_nextRetryAt` ON `sync_queue` (`status`, `nextRetryAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_entityType_uuid_operation` ON `sync_queue` (`entityType`, `uuid`, `operation`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_storeId_status` ON `sync_queue` (`storeId`, `status`)")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `bankName` TEXT")
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `bankAccountNumber` TEXT")
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `bankAccountHolderName` TEXT")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pos_umkm_offline.db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                // NOTE: fallbackToDestructiveMigration() removed in Phase 9.
                // All schema changes must be expressed as explicit migrations.
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun hashString(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            val adminUser = User(
                username = "admin",
                passwordHash = hashString("admin123"),
                pin = hashString("1234"),
                role = "ADMIN"
            )
            val cashierUser = User(
                username = "kasir",
                passwordHash = hashString("kasir123"),
                pin = hashString("4321"),
                role = "CASHIER"
            )
            val supervisorUser = User(
                username = "spv",
                passwordHash = hashString("spv123"),
                pin = hashString("1111"),
                role = "SUPERVISOR"
            )
            db.userDao().insertUser(adminUser)
            db.userDao().insertUser(cashierUser)
            db.userDao().insertUser(supervisorUser)

            val defaultSettings = StoreSetting(
                storeName = "Mediaxa Business Suite",
                address = "Jl. Raya UMKM No. 1",
                phoneNumber = "081234567890",
                receiptFooter = "Terima Kasih Atas Kunjungan Anda",
                adminPin = hashString("1234")
            )
            db.storeSettingDao().insertOrUpdate(defaultSettings)

            val categories = listOf(
                Category(name = "Coffee", displayOrder = 1),
                Category(name = "Non Coffee", displayOrder = 2),
                Category(name = "Juice", displayOrder = 3),
                Category(name = "Food", displayOrder = 4),
                Category(name = "Snack", displayOrder = 5)
            )
            for (category in categories) {
                db.categoryDao().insertCategory(category)
            }
        }
    }
}
