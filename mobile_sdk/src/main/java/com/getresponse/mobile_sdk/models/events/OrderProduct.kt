package com.getresponse.mobile_sdk.models.events

data class OrderProduct(
    val product: Product,
    val categories: List<Category>? = null,
    val quantity: Int
)
