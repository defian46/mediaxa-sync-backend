package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.entity.LoyaltyPointHistory
import com.mediaxa.business.suite.data.repository.LoyaltyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoyaltyViewModel(
    private val repository: LoyaltyRepository
) : ViewModel() {

    private val _storeId = MutableStateFlow(1L)
    
    private val _currentCustomerUuid = MutableStateFlow<String?>(null)

    val pointHistory = _currentCustomerUuid.flatMapLatest { uuid ->
        if (uuid.isNullOrEmpty()) {
            flowOf(emptyList())
        } else {
            repository.getPointHistoryFlow(_storeId.value, uuid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pointsBalance = _currentCustomerUuid.flatMapLatest { uuid ->
        if (uuid.isNullOrEmpty()) {
            flowOf(0)
        } else {
            repository.getPointsBalanceFlow(_storeId.value, uuid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectCustomer(uuid: String?) {
        _currentCustomerUuid.value = uuid
    }

    fun setStoreId(id: Long) {
        _storeId.value = id
    }

    fun adjustPointsManually(
        customerUuid: String,
        points: Int,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (points == 0) {
            onError("Poin harus lebih besar atau kecil dari 0")
            return
        }

        viewModelScope.launch {
            try {
                val item = LoyaltyPointHistory(
                    storeId = _storeId.value,
                    customerUuid = customerUuid,
                    transactionUuid = null,
                    points = points,
                    activityType = if (points > 0) "EARNED" else "REDEEMED",
                    notes = notes
                )
                repository.insertPointHistory(item)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal menyesuaikan poin")
            }
        }
    }

    class Factory(private val repository: LoyaltyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoyaltyViewModel::class.java)) {
                return LoyaltyViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
