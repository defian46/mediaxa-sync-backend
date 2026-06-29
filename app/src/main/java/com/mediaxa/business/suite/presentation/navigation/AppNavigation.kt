package com.mediaxa.business.suite.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.mediaxa.business.suite.presentation.screen.*
import com.mediaxa.business.suite.presentation.viewmodel.AuthViewModel
import com.mediaxa.business.suite.presentation.viewmodel.ProductViewModel
import com.mediaxa.business.suite.presentation.viewmodel.InventoryViewModel
import com.mediaxa.business.suite.presentation.viewmodel.PosViewModel
import com.mediaxa.business.suite.presentation.viewmodel.DashboardViewModel
import com.mediaxa.business.suite.presentation.viewmodel.DashboardViewModelFactory
import com.mediaxa.business.suite.presentation.viewmodel.InventoryLiteViewModel
import com.mediaxa.business.suite.presentation.viewmodel.FinanceViewModel
import com.mediaxa.business.suite.presentation.viewmodel.CustomerViewModel
import com.mediaxa.business.suite.presentation.viewmodel.LoyaltyViewModel
import com.mediaxa.business.suite.presentation.viewmodel.PromotionViewModel
import com.mediaxa.business.suite.presentation.viewmodel.SyncMonitorViewModel
import com.mediaxa.business.suite.presentation.viewmodel.MainViewModel
import com.mediaxa.business.suite.MainApplication
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    inventoryViewModel: InventoryViewModel,
    posViewModel: PosViewModel,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext as MainApplication
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            salesRepository = context.salesRepository,
            analyticsRepository = context.analyticsRepository,
            inventoryRepository = context.inventoryRepository
        )
    )

    val inventoryLiteViewModel: InventoryLiteViewModel = viewModel(
        factory = InventoryLiteViewModel.Factory(
            inventoryRepository = context.inventoryRepository,
            purchaseExpenseRepository = context.purchaseExpenseRepository,
            expenseRepository = context.expenseRepository,
            stockOpnameRepository = context.stockOpnameRepository,
            wasteRepository = context.wasteRepository,
            inventoryLiteRepository = context.inventoryLiteRepository
        )
    )

    val financeViewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModel.Factory(
            repository = context.financeRepository
        )
    )

    val customerViewModel: CustomerViewModel = viewModel(
        factory = CustomerViewModel.Factory(
            repository = context.customerRepository
        )
    )

    val loyaltyViewModel: LoyaltyViewModel = viewModel(
        factory = LoyaltyViewModel.Factory(
            repository = context.loyaltyRepository
        )
    )

    val promotionViewModel: PromotionViewModel = viewModel(
        factory = PromotionViewModel.Factory(
            repository = context.promotionRepository
        )
    )

    val syncMonitorViewModel: SyncMonitorViewModel = viewModel(
        factory = SyncMonitorViewModel.Factory(
            context = context,
            localDataSource = context.localDataSource,
            syncEngine = context.syncEngine
        )
    )

    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(
            repository = context.storeSettingRepository
        )
    )

    NavHost(
        navController = navController,
        startDestination = NavRoute.Splash.route
    ) {
        composable(NavRoute.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(NavRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { user ->
                    val dest = when (user.role) {
                        "ADMIN", "ADMINISTRATOR" -> NavRoute.AdminDashboard.route
                        "CASHIER" -> NavRoute.CashierDashboard.route
                        "SUPERVISOR" -> NavRoute.SupervisorDashboard.route
                        else -> NavRoute.Login.route
                    }
                    navController.navigate(dest) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.AdminDashboard.route) {
            val currentUser = authViewModel.currentUser.value
            if (currentUser != null) {
                AdminDashboardScreen(
                    user = currentUser,
                    mainViewModel = mainViewModel,
                    syncMonitorViewModel = syncMonitorViewModel,
                    dashboardViewModel = dashboardViewModel,
                    productViewModel = productViewModel,
                    customerViewModel = customerViewModel,
                    financeViewModel = financeViewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onManageCategories = { navController.navigate(NavRoute.Categories.route) },
                    onManageIngredients = { navController.navigate(NavRoute.InventoryDashboardLite.route) },
                    onManageMenus = { navController.navigate(NavRoute.Menus.route) },
                    onNavigateToPos = { navController.navigate(NavRoute.Pos.route) },
                    onNavigateToHistory = { navController.navigate(NavRoute.TransactionHistory.route) },
                    onNavigateToAnalytics = { navController.navigate(NavRoute.AnalyticsDashboard.route) },
                    onNavigateToFinance = { navController.navigate(NavRoute.FinanceDashboard.route) },
                    onNavigateToCrm = { navController.navigate(NavRoute.CustomerList.route) },
                    onNavigateToPromotions = { navController.navigate(NavRoute.PromotionManager.route) },
                    onNavigateToCustomerAnalytics = { navController.navigate(NavRoute.CustomerAnalytics.route) },
                    onNavigateToSyncMonitor = { navController.navigate(NavRoute.SyncMonitor.route) },
                    onNavigateToStoreSettings = { navController.navigate(NavRoute.StoreSettings.route) },
                    onNavigateToSettingsCenter = { navController.navigate(NavRoute.SettingsCenter.route) },
                    onNavigateToBelanja = { navController.navigate(NavRoute.AddPurchaseExpense.route) },
                    onNavigateToPengeluaran = { navController.navigate(NavRoute.AddExpense.route) },
                    onNavigateToClosing = { navController.navigate(NavRoute.DailyClosing.route) }
                )
            } else {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        composable(NavRoute.CashierDashboard.route) {
            val currentUser = authViewModel.currentUser.value
            if (currentUser != null) {
                CashierDashboardScreen(
                    user = currentUser,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToPos = { navController.navigate(NavRoute.Pos.route) },
                    onNavigateToHistory = { navController.navigate(NavRoute.TransactionHistory.route) },
                    onNavigateToSettingsCenter = { navController.navigate(NavRoute.SettingsCenter.route) }
                )
            } else {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        composable(NavRoute.SupervisorDashboard.route) {
            val currentUser = authViewModel.currentUser.value
            if (currentUser != null) {
                SupervisorDashboardScreen(
                    user = currentUser,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToPos = { navController.navigate(NavRoute.Pos.route) },
                    onNavigateToHistory = { navController.navigate(NavRoute.TransactionHistory.route) },
                    onNavigateToSettingsCenter = { navController.navigate(NavRoute.SettingsCenter.route) }
                )
            } else {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        composable(NavRoute.Categories.route) {
            CategoryScreen(
                viewModel = productViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.Ingredients.route) {
            IngredientScreen(
                viewModel = inventoryViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.Menus.route) {
            MenuManagementScreen(
                viewModel = productViewModel,
                onBackClick = { navController.navigateUp() },
                onAddMenuClick = { navController.navigate(NavRoute.AddEditMenu.createRoute(null)) },
                onEditMenuClick = { uuid -> navController.navigate(NavRoute.AddEditMenu.createRoute(uuid)) },
                onManageRecipeClick = { uuid -> navController.navigate(NavRoute.RecipeBom.createRoute(uuid)) }
            )
        }

        composable(
            route = NavRoute.AddEditMenu.route,
            arguments = listOf(navArgument("menuUuid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("menuUuid")
            val realUuid = if (uuid == "NEW") null else uuid
            AddEditMenuScreen(
                viewModel = productViewModel,
                menuUuid = realUuid,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(
            route = NavRoute.RecipeBom.route,
            arguments = listOf(navArgument("menuUuid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("menuUuid") ?: ""
            RecipeBomScreen(
                productViewModel = productViewModel,
                inventoryViewModel = inventoryViewModel,
                menuUuid = uuid,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.Pos.route) {
            PosScreen(
                viewModel = posViewModel,
                onBackClick = { navController.navigateUp() },
                onNavigateToPayment = { navController.navigate(NavRoute.Payment.route) }
            )
        }

        composable(NavRoute.Payment.route) {
            val currentUser = authViewModel.currentUser.value
            if (currentUser != null) {
                PaymentScreen(
                    viewModel = posViewModel,
                    currentUser = currentUser,
                    onBackClick = { navController.navigateUp() },
                    onCheckoutSuccess = { txUuid ->
                        navController.navigate(NavRoute.Receipt.createRoute(txUuid)) {
                            popUpTo(NavRoute.Pos.route) { inclusive = true }
                        }
                    }
                )
            } else {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        composable(
            route = NavRoute.Receipt.route,
            arguments = listOf(navArgument("transactionUuid") { type = NavType.StringType })
        ) { backStackEntry ->
            val txUuid = backStackEntry.arguments?.getString("transactionUuid") ?: ""
            val context = LocalContext.current
            val app = context.applicationContext as MainApplication
            ReceiptScreen(
                transactionUuid = txUuid,
                transactionRepository = app.transactionRepository,
                storeSettingRepository = app.storeSettingRepository,
                onFinishClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoute.TransactionHistory.route) {
            TransactionHistoryScreen(
                viewModel = posViewModel,
                onBackClick = { navController.navigateUp() },
                onTransactionClick = { txUuid ->
                    navController.navigate(NavRoute.Receipt.createRoute(txUuid))
                }
            )
        }

        composable(NavRoute.AnalyticsDashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToPos = { navController.navigate(NavRoute.Pos.route) },
                onNavigateToInventory = { navController.navigate(NavRoute.InventoryDashboardLite.route) },
                onNavigateToHistory = { navController.navigate(NavRoute.TransactionHistory.route) }
            )
        }

        composable(NavRoute.InventoryDashboardLite.route) {
            InventoryDashboardLiteScreen(
                viewModel = inventoryLiteViewModel,
                onBackClick = { navController.navigateUp() },
                onNavigateToPurchase = { navController.navigate(NavRoute.AddPurchaseExpense.route) },
                onNavigateToExpense = { navController.navigate(NavRoute.AddExpense.route) },
                onNavigateToOpname = { navController.navigate(NavRoute.StockOpname.route) },
                onNavigateToWaste = { navController.navigate(NavRoute.AddWaste.route) },
                onManageIngredients = { navController.navigate(NavRoute.Ingredients.route) }
            )
        }

        composable(NavRoute.AddPurchaseExpense.route) {
            AddPurchaseExpenseScreen(
                viewModel = inventoryLiteViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.AddExpense.route) {
            AddExpenseScreen(
                viewModel = inventoryLiteViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.StockOpname.route) {
            StockOpnameScreen(
                viewModel = inventoryLiteViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.AddWaste.route) {
            AddWasteScreen(
                viewModel = inventoryLiteViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.FinanceDashboard.route) {
            FinanceDashboardScreen(
                viewModel = financeViewModel,
                onBackClick = { navController.navigateUp() },
                onViewPLClick = { navController.navigate(NavRoute.ProfitLossReport.route) },
                onViewCashFlowClick = { navController.navigate(NavRoute.CashFlow.route) },
                onViewExpenseClick = { navController.navigate(NavRoute.FinanceExpense.route) },
                onViewDailyClosingClick = { navController.navigate(NavRoute.DailyClosing.route) },
                onViewCashShiftClick = { navController.navigate(NavRoute.CashShift.route) }
            )
        }

        composable(NavRoute.DailyClosing.route) {
            DailyClosingScreen(
                viewModel = financeViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.CashShift.route) {
            CashShiftScreen(
                viewModel = financeViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.ProfitLossReport.route) {
            ProfitLossReportScreen(
                viewModel = financeViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.CashFlow.route) {
            CashFlowScreen(
                viewModel = financeViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.FinanceExpense.route) {
            FinanceExpenseScreen(
                viewModel = financeViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.CustomerList.route) {
            CustomerScreen(
                viewModel = customerViewModel,
                onBackClick = { navController.navigateUp() },
                onCustomerClick = { uuid ->
                    navController.navigate(NavRoute.CustomerProfile.createRoute(uuid))
                }
            )
        }

        composable(
            route = NavRoute.CustomerProfile.route,
            arguments = listOf(navArgument("uuid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
            CustomerProfileScreen(
                customerUuid = uuid,
                viewModel = customerViewModel,
                onBackClick = { navController.navigateUp() },
                onManageLoyalty = { customerUuid, customerName ->
                    navController.navigate(NavRoute.LoyaltySettings.createRoute(customerUuid, customerName))
                }
            )
        }

        composable(
            route = NavRoute.LoyaltySettings.route,
            arguments = listOf(
                navArgument("customerUuid") { type = NavType.StringType },
                navArgument("customerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val customerUuid = backStackEntry.arguments?.getString("customerUuid") ?: ""
            val customerName = backStackEntry.arguments?.getString("customerName") ?: ""
            LoyaltySettingsScreen(
                customerUuid = customerUuid,
                customerName = customerName,
                viewModel = loyaltyViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.PromotionManager.route) {
            PromotionScreen(
                viewModel = promotionViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.CustomerAnalytics.route) {
            CustomerAnalyticsScreen(
                viewModel = customerViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        // Phase 9 — Cloud Sync Monitor
        composable(NavRoute.SyncMonitor.route) {
            SyncMonitorScreen(
                viewModel = syncMonitorViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(NavRoute.StoreSettings.route) {
            StoreSettingScreen(
                viewModel = mainViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(NavRoute.SettingsCenter.route) {
            SettingsCenterScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.navigateUp() },
                onNavigateToAccountSecurity = { navController.navigate(NavRoute.AccountSecurity.route) },
                onNavigateToStoreSettings = { navController.navigate(NavRoute.StoreSettings.route) }
            )
        }

        composable(NavRoute.AccountSecurity.route) {
            AccountSecurityScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}
