package com.mediaxa.business.suite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mediaxa.business.suite.MainApplication
import com.mediaxa.business.suite.presentation.navigation.AppNavigation
import com.mediaxa.business.suite.presentation.theme.MediaxaBusinessSuiteTheme
import com.mediaxa.business.suite.presentation.viewmodel.AuthViewModel
import com.mediaxa.business.suite.presentation.viewmodel.ProductViewModel
import com.mediaxa.business.suite.presentation.viewmodel.InventoryViewModel
import com.mediaxa.business.suite.presentation.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as MainApplication
        
        val authViewModel: AuthViewModel by viewModels {
            AuthViewModel.Factory(app.userRepository, app.localDataSource.auditLogDao, applicationContext)
        }
        
        val productViewModel: ProductViewModel by viewModels {
            ProductViewModel.Factory(app.productRepository, app.inventoryRepository)
        }
        
        val inventoryViewModel: InventoryViewModel by viewModels {
            InventoryViewModel.Factory(app.inventoryRepository)
        }

        val posViewModel: PosViewModel by viewModels {
            PosViewModel.Factory(
                app.productRepository,
                app.transactionRepository,
                app.storeSettingRepository,
                app.checkoutService,
                app.customerRepository,
                app.promotionRepository,
                app.loyaltyRepository
            )
        }

        setContent {
            MediaxaBusinessSuiteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authViewModel = authViewModel,
                        productViewModel = productViewModel,
                        inventoryViewModel = inventoryViewModel,
                        posViewModel = posViewModel
                    )
                }
            }
        }
    }
}
