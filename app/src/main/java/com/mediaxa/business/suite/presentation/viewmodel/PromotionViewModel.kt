package com.mediaxa.business.suite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediaxa.business.suite.data.local.entity.PromotionRule
import com.mediaxa.business.suite.data.repository.PromotionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PromotionViewModel(
    private val repository: PromotionRepository
) : ViewModel() {

    private val _storeId = MutableStateFlow(1L)
    val storeId = _storeId.asStateFlow()

    val allPromotionRules = _storeId.flatMapLatest { storeId ->
        repository.getAllPromotionRulesFlow(storeId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePromotionRules = _storeId.flatMapLatest { storeId ->
        repository.getActivePromotionRulesFlow(storeId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStoreId(id: Long) {
        _storeId.value = id
    }

    fun savePromotionRule(
        uuid: String?,
        name: String,
        promoType: String,
        value: Double,
        buyMenuUuid: String?,
        buyQuantity: Int?,
        getMenuUuid: String?,
        getQuantity: Int?,
        minPurchaseAmount: Double?,
        isActive: Boolean,
        startDate: Long?,
        endDate: Long?,
        startHour: Int?,
        endHour: Int?,
        applicableDays: String?,
        targetMembershipLevels: String?,
        targetCategoryUuid: String?,
        targetMenuUuid: String?,
        promoCode: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank() || value < 0.0) {
            onError("Nama promo wajib diisi dan nilai diskon tidak boleh negatif")
            return
        }

        viewModelScope.launch {
            try {
                if (uuid == null) {
                    val rule = PromotionRule(
                        storeId = _storeId.value,
                        name = name,
                        promoType = promoType,
                        value = value,
                        buyMenuUuid = buyMenuUuid,
                        buyQuantity = buyQuantity,
                        getMenuUuid = getMenuUuid,
                        getQuantity = getQuantity,
                        minPurchaseAmount = minPurchaseAmount,
                        isActive = isActive,
                        startDate = startDate,
                        endDate = endDate,
                        startHour = startHour,
                        endHour = endHour,
                        applicableDays = applicableDays,
                        targetMembershipLevels = targetMembershipLevels,
                        targetCategoryUuid = targetCategoryUuid,
                        targetMenuUuid = targetMenuUuid,
                        promoCode = promoCode
                    )
                    repository.insertPromotionRule(rule)
                } else {
                    val existing = repository.getPromotionRuleByUuid(uuid)
                    if (existing != null) {
                        val updated = existing.copy(
                            name = name,
                            promoType = promoType,
                            value = value,
                            buyMenuUuid = buyMenuUuid,
                            buyQuantity = buyQuantity,
                            getMenuUuid = getMenuUuid,
                            getQuantity = getQuantity,
                            minPurchaseAmount = minPurchaseAmount,
                            isActive = isActive,
                            startDate = startDate,
                            endDate = endDate,
                            startHour = startHour,
                            endHour = endHour,
                            applicableDays = applicableDays,
                            targetMembershipLevels = targetMembershipLevels,
                            targetCategoryUuid = targetCategoryUuid,
                            targetMenuUuid = targetMenuUuid,
                            promoCode = promoCode,
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = "PENDING_UPDATE"
                        )
                        repository.updatePromotionRule(updated)
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal menyimpan aturan promo")
            }
        }
    }

    fun deletePromotionRule(rule: PromotionRule) {
        viewModelScope.launch {
            val softDeleted = rule.copy(
                isDeleted = 1,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING_DELETE"
            )
            repository.updatePromotionRule(softDeleted)
        }
    }

    fun togglePromotionActive(rule: PromotionRule, isActive: Boolean) {
        viewModelScope.launch {
            val updated = rule.copy(
                isActive = isActive,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING_UPDATE"
            )
            repository.updatePromotionRule(updated)
        }
    }

    class Factory(private val repository: PromotionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PromotionViewModel::class.java)) {
                return PromotionViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
