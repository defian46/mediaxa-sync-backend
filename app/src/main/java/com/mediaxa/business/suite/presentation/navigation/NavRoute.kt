package com.mediaxa.business.suite.presentation.navigation

sealed class NavRoute(val route: String) {
    object Splash : NavRoute("splash")
    object Login : NavRoute("login")
    object AdminDashboard : NavRoute("admin_dashboard")
    object CashierDashboard : NavRoute("cashier_dashboard")
    object SupervisorDashboard : NavRoute("supervisor_dashboard")
    
    // Management routes added in Phase 2
    object Categories : NavRoute("categories")
    object Ingredients : NavRoute("ingredients")
    object Menus : NavRoute("menus")
    object AddEditMenu : NavRoute("add_edit_menu/{menuUuid}") {
        fun createRoute(menuUuid: String?) = "add_edit_menu/${menuUuid ?: "NEW"}"
    }
    object RecipeBom : NavRoute("recipe_bom/{menuUuid}") {
        fun createRoute(menuUuid: String) = "recipe_bom/$menuUuid"
    }
    object Pos : NavRoute("pos")
    object Payment : NavRoute("payment")
    object Receipt : NavRoute("receipt/{transactionUuid}") {
        fun createRoute(transactionUuid: String) = "receipt/$transactionUuid"
    }
    object TransactionHistory : NavRoute("transaction_history")
    object AnalyticsDashboard : NavRoute("analytics_dashboard")
    object InventoryDashboardLite : NavRoute("inventory_dashboard_lite")
    object AddPurchaseExpense : NavRoute("add_purchase_expense")
    object AddExpense : NavRoute("add_expense")
    object StockOpname : NavRoute("stock_opname")
    object AddWaste : NavRoute("add_waste")
    object FinanceDashboard : NavRoute("finance_dashboard")
    object ProfitLossReport : NavRoute("profit_loss_report")
    object CashFlow : NavRoute("cash_flow")
    object FinanceExpense : NavRoute("finance_expense")
    object DailyClosing : NavRoute("daily_closing")
    object CashShift : NavRoute("cash_shift")
    
    // New Routes for CRM, Loyalty, Promotion, and Customer Analytics
    object CustomerList : NavRoute("customers")
    object CustomerProfile : NavRoute("customer_profile/{uuid}") {
        fun createRoute(uuid: String) = "customer_profile/$uuid"
    }
    object LoyaltySettings : NavRoute("loyalty_settings/{customerUuid}/{customerName}") {
        fun createRoute(customerUuid: String, customerName: String) = "loyalty_settings/$customerUuid/$customerName"
    }
    object PromotionManager : NavRoute("promotions")
    object CustomerAnalytics : NavRoute("customer_analytics")

    // Phase 9 — Cloud Sync Monitoring
    object SyncMonitor : NavRoute("sync_monitor")

    // Store settings route for UX Fix Sprint
    object StoreSettings : NavRoute("store_settings")
    object SettingsCenter : NavRoute("settings_center")
    object AccountSecurity : NavRoute("account_security")
}
