package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.entity.Menu
import com.mediaxa.business.suite.data.repository.ProductRepository
import com.mediaxa.business.suite.data.repository.TransactionRepository
import com.mediaxa.business.suite.data.repository.StoreSettingRepository
import com.mediaxa.business.suite.data.local.entity.Customer
import com.mediaxa.business.suite.data.local.entity.PromotionRule
import com.mediaxa.business.suite.data.repository.CustomerRepository
import com.mediaxa.business.suite.data.repository.PromotionRepository
import com.mediaxa.business.suite.data.repository.LoyaltyRepository
import com.mediaxa.business.suite.data.repository.CheckoutService
import com.mediaxa.business.suite.data.repository.CheckoutResult
import com.mediaxa.business.suite.data.repository.StockValidationResult
import com.mediaxa.business.suite.domain.model.CartItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PosViewModel(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val storeSettingRepository: StoreSettingRepository,
    private val checkoutService: CheckoutService,
    private val customerRepository: CustomerRepository,
    private val promotionRepository: PromotionRepository,
    private val loyaltyRepository: LoyaltyRepository
) : ViewModel() {

    val categories = productRepository.getActiveCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryUuid = MutableStateFlow<String?>(null)
    val selectedCategoryUuid = _selectedCategoryUuid.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredMenus = combine(
        productRepository.getActiveMenusFlow(),
        _selectedCategoryUuid,
        _searchQuery
    ) { menus, categoryUuid, query ->
        menus.filter { menu ->
            val matchesCategory = categoryUuid == null || menu.categoryUuid == categoryUuid
            val matchesQuery = query.isEmpty() || menu.name.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems = _cartItems.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount = _discountAmount.asStateFlow()

    val storeSetting = storeSettingRepository.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _appliedVoucherCode = MutableStateFlow<String?>(null)
    val appliedVoucherCode = _appliedVoucherCode.asStateFlow()

    private val _redeemedPoints = MutableStateFlow(0)
    val redeemedPoints = _redeemedPoints.asStateFlow()

    val activePromotions = promotionRepository.getActivePromotionRulesFlow(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    val posCustomers = customerSearchQuery.flatMapLatest { query ->
        if (query.isEmpty()) {
            customerRepository.getAllCustomersFlow(1L)
        } else {
            customerRepository.searchCustomersFlow(1L, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCustomerPoints = _selectedCustomer.flatMapLatest { customer ->
        if (customer == null) {
            flowOf(0)
        } else {
            loyaltyRepository.getPointsBalanceFlow(1L, customer.uuid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pointsDiscount = combine(_redeemedPoints, storeSetting) { pts, setting ->
        val pointsValue = setting?.loyaltyPointsValue ?: 100.0
        pts * pointsValue
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val promotionDiscount = combine(
        _cartItems,
        activePromotions,
        _selectedCustomer,
        _appliedVoucherCode
    ) { cart, rules, customer, voucher ->
        calculatePromotionDiscount(cart, rules, customer, voucher)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val effectiveDiscount = combine(_discountAmount, promotionDiscount, pointsDiscount) { manual, promo, pts ->
        manual + promo + pts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val transactions = transactionRepository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtotal = _cartItems.map { items ->
        items.sumOf { it.menu.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val taxAmount = combine(subtotal, effectiveDiscount, storeSetting) { sub, disc, setting ->
        val subAfterDisc = (sub - disc).coerceAtLeast(0.0)
        if (setting?.isTaxEnabled == true) subAfterDisc * TAX_RATE else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val serviceChargeAmount = combine(subtotal, effectiveDiscount, storeSetting) { sub, disc, setting ->
        val subAfterDisc = (sub - disc).coerceAtLeast(0.0)
        if (setting?.isServiceChargeEnabled == true) subAfterDisc * SERVICE_CHARGE_RATE else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val total = combine(subtotal, effectiveDiscount, taxAmount, serviceChargeAmount) { sub, disc, tax, service ->
        ((sub - disc).coerceAtLeast(0.0) + tax + service)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun selectCategory(categoryUuid: String?) {
        _selectedCategoryUuid.value = categoryUuid
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(menu: Menu) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.menu.uuid == menu.uuid }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(CartItem(menu, 1))
        }
        _cartItems.value = current
    }

    fun increaseQty(menu: Menu) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.menu.uuid == menu.uuid }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1)
            _cartItems.value = current
        }
    }

    fun decreaseQty(menu: Menu) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.menu.uuid == menu.uuid }
        if (index != -1) {
            val existing = current[index]
            if (existing.quantity > 1) {
                current[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    fun updateCartItemNote(menu: Menu, note: String?) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.menu.uuid == menu.uuid }
        if (index != -1) {
            current[index] = current[index].copy(note = note)
            _cartItems.value = current
        }
    }

    fun removeFromCart(menu: Menu) {
        _cartItems.value = _cartItems.value.filterNot { it.menu.uuid == menu.uuid }
    }

    fun setDiscount(amount: Double) {
        _discountAmount.value = amount
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        _redeemedPoints.value = 0
    }

    fun setAppliedVoucherCode(code: String?) {
        _appliedVoucherCode.value = code
    }

    fun setRedeemedPoints(points: Int) {
        _redeemedPoints.value = points
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _discountAmount.value = 0.0
        _selectedCustomer.value = null
        _appliedVoucherCode.value = null
        _redeemedPoints.value = 0
    }

    fun checkStock(onResult: (StockValidationResult) -> Unit) {
        viewModelScope.launch {
            val result = checkoutService.validateStock(_cartItems.value)
            onResult(result)
        }
    }

    fun checkout(
        cashierUuid: String,
        cashierName: String,
        paymentMethod: String,
        amountReceived: Double,
        onResult: (CheckoutResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = checkoutService.executeCheckout(
                cart = _cartItems.value,
                discount = _discountAmount.value,
                cashierUuid = cashierUuid,
                cashierName = cashierName,
                paymentMethod = paymentMethod,
                amountReceived = amountReceived,
                storeId = 1L,
                customerUuid = _selectedCustomer.value?.uuid,
                pointsToRedeem = _redeemedPoints.value
            )
            if (result is CheckoutResult.Success) {
                clearCart()
            }
            onResult(result)
        }
    }

    private fun calculatePromotionDiscount(
        cart: List<CartItem>,
        rules: List<PromotionRule>,
        customer: Customer?,
        voucherCode: String?
    ): Double {
        if (cart.isEmpty()) return 0.0
        val subtotal = cart.sumOf { it.menu.price * it.quantity }
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        val currentDay = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "SUNDAY"
            java.util.Calendar.MONDAY -> "MONDAY"
            java.util.Calendar.TUESDAY -> "TUESDAY"
            java.util.Calendar.WEDNESDAY -> "WEDNESDAY"
            java.util.Calendar.THURSDAY -> "THURSDAY"
            java.util.Calendar.FRIDAY -> "FRIDAY"
            java.util.Calendar.SATURDAY -> "SATURDAY"
            else -> ""
        }

        var totalDiscount = 0.0

        for (rule in rules) {
            if (rule.startDate != null && now < rule.startDate) continue
            if (rule.endDate != null && now > rule.endDate) continue
            if (rule.startHour != null && currentHour < rule.startHour) continue
            if (rule.endHour != null && currentHour > rule.endHour) continue

            if (!rule.applicableDays.isNullOrEmpty()) {
                val days = rule.applicableDays.split(",").map { it.trim().uppercase() }
                if (!days.contains(currentDay)) continue
            }

            if (!rule.targetMembershipLevels.isNullOrEmpty()) {
                val memberTier = customer?.membershipLevel?.uppercase() ?: "BRONZE"
                val targetTiers = rule.targetMembershipLevels.split(",").map { it.trim().uppercase() }
                if (!targetTiers.contains(memberTier)) continue
            }

            if (!rule.promoCode.isNullOrEmpty()) {
                if (rule.promoCode != voucherCode) continue
            }

            when (rule.promoType) {
                "BUY_X_GET_Y" -> {
                    val buyMenuUuid = rule.buyMenuUuid
                    val buyQty = rule.buyQuantity ?: 1
                    val getMenuUuid = rule.getMenuUuid
                    val getQty = rule.getQuantity ?: 1
                    
                    if (buyMenuUuid != null && getMenuUuid != null) {
                        val buyCartItem = cart.firstOrNull { it.menu.uuid == buyMenuUuid }
                        val getCartItem = cart.firstOrNull { it.menu.uuid == getMenuUuid }
                        if (buyCartItem != null && getCartItem != null) {
                            val eligibleFreeQty = (buyCartItem.quantity / buyQty) * getQty
                            val actualFreeQty = eligibleFreeQty.coerceAtMost(getCartItem.quantity)
                            totalDiscount += actualFreeQty * getCartItem.menu.price
                        }
                    }
                }
                "PERCENTAGE_DISCOUNT" -> {
                    if (rule.minPurchaseAmount != null && subtotal < rule.minPurchaseAmount) continue
                    
                    val targetMenuUuid = rule.targetMenuUuid
                    val targetCatUuid = rule.targetCategoryUuid
                    
                    if (targetMenuUuid != null) {
                        val targetItem = cart.firstOrNull { it.menu.uuid == targetMenuUuid }
                        if (targetItem != null) {
                            totalDiscount += (targetItem.menu.price * targetItem.quantity) * (rule.value / 100.0)
                        }
                    } else if (targetCatUuid != null) {
                        val categoryItems = cart.filter { it.menu.categoryUuid == targetCatUuid }
                        totalDiscount += categoryItems.sumOf { it.menu.price * it.quantity } * (rule.value / 100.0)
                    } else {
                        totalDiscount += subtotal * (rule.value / 100.0)
                    }
                }
                "NOMINAL_DISCOUNT" -> {
                    if (rule.minPurchaseAmount != null && subtotal < rule.minPurchaseAmount) continue
                    
                    val targetMenuUuid = rule.targetMenuUuid
                    val targetCatUuid = rule.targetCategoryUuid
                    
                    if (targetMenuUuid != null) {
                        val targetItem = cart.firstOrNull { it.menu.uuid == targetMenuUuid }
                        if (targetItem != null) {
                            totalDiscount += rule.value * targetItem.quantity
                        }
                    } else if (targetCatUuid != null) {
                        val categoryItems = cart.filter { it.menu.categoryUuid == targetCatUuid }
                        totalDiscount += rule.value * categoryItems.sumOf { it.quantity }
                    } else {
                        totalDiscount += rule.value
                    }
                }
            }
        }
        return totalDiscount.coerceAtMost(subtotal)
    }

    class Factory(
        private val productRepository: ProductRepository,
        private val transactionRepository: TransactionRepository,
        private val storeSettingRepository: StoreSettingRepository,
        private val checkoutService: CheckoutService,
        private val customerRepository: CustomerRepository,
        private val promotionRepository: PromotionRepository,
        private val loyaltyRepository: LoyaltyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
                return PosViewModel(
                    productRepository,
                    transactionRepository,
                    storeSettingRepository,
                    checkoutService,
                    customerRepository,
                    promotionRepository,
                    loyaltyRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        const val TAX_RATE = 0.10
        const val SERVICE_CHARGE_RATE = 0.05
    }
}
