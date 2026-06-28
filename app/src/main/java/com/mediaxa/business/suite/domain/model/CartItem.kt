package com.mediaxa.business.suite.domain.model

import com.mediaxa.business.suite.data.local.entity.Menu

data class CartItem(
    val menu: Menu,
    val quantity: Int,
    val note: String? = null
)
