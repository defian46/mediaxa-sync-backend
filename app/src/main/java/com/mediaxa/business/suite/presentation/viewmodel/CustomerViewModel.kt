package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.entity.Customer
import com.mediaxa.business.suite.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _storeId = MutableStateFlow(1L)
    val storeId = _storeId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val customers = combine(_storeId, _searchQuery) { storeId, query ->
        Pair(storeId, query)
    }.flatMapLatest { (storeId, query) ->
        if (query.isEmpty()) {
            repository.getAllCustomersFlow(storeId)
        } else {
            repository.searchCustomersFlow(storeId, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topCustomers = _storeId.flatMapLatest { storeId ->
        repository.getTopCustomersFlow(storeId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _favoriteMenuUuid = MutableStateFlow<String?>(null)
    val favoriteMenuUuid = _favoriteMenuUuid.asStateFlow()

    private val _newCustomersCount = MutableStateFlow(0)
    val newCustomersCount = _newCustomersCount.asStateFlow()

    private val _activeCustomersCount = MutableStateFlow(0)
    val activeCustomersCount = _activeCustomersCount.asStateFlow()

    init {
        loadDashboardStats()
    }

    fun setStoreId(id: Long) {
        _storeId.value = id
        loadDashboardStats()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCustomer(uuid: String?) {
        if (uuid == null) {
            _selectedCustomer.value = null
            _favoriteMenuUuid.value = null
        } else {
            viewModelScope.launch {
                val customer = repository.getCustomerByUuid(uuid)
                _selectedCustomer.value = customer
                if (customer != null) {
                    _favoriteMenuUuid.value = repository.getFavoriteMenuUuid(_storeId.value, uuid)
                }
            }
        }
    }

    fun saveCustomer(
        uuid: String?,
        code: String,
        name: String,
        phone: String?,
        email: String?,
        birthday: Long?,
        gender: String?,
        address: String?,
        notes: String?,
        membershipLevel: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank() || code.isBlank()) {
            onError("Nama dan Kode Anggota wajib diisi")
            return
        }

        viewModelScope.launch {
            try {
                if (uuid == null) {
                    // Check duplicate code
                    val existing = repository.getCustomerByCode(_storeId.value, code)
                    if (existing != null) {
                        onError("Kode Anggota sudah terdaftar")
                        return@launch
                    }

                    val newCustomer = Customer(
                        storeId = _storeId.value,
                        customerCode = code,
                        customerName = name,
                        phone = phone,
                        email = email,
                        birthday = birthday,
                        gender = gender,
                        address = address,
                        notes = notes,
                        membershipLevel = membershipLevel
                    )
                    repository.insertCustomer(newCustomer)
                } else {
                    val existing = repository.getCustomerByUuid(uuid)
                    if (existing != null) {
                        val updated = existing.copy(
                            customerCode = code,
                            customerName = name,
                            phone = phone,
                            email = email,
                            birthday = birthday,
                            gender = gender,
                            address = address,
                            notes = notes,
                            membershipLevel = membershipLevel,
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = "PENDING_UPDATE"
                        )
                        repository.updateCustomer(updated)
                    }
                }
                loadDashboardStats()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal menyimpan customer")
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            val softDeleted = customer.copy(
                isDeleted = 1,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING_DELETE"
            )
            repository.updateCustomer(softDeleted)
            loadDashboardStats()
        }
    }

    private fun loadDashboardStats() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
            _newCustomersCount.value = repository.getNewCustomersCount(_storeId.value, thirtyDaysAgo)
            _activeCustomersCount.value = repository.getActiveCustomersCount(_storeId.value, thirtyDaysAgo)
        }
    }

    class Factory(private val repository: CustomerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
                return CustomerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
