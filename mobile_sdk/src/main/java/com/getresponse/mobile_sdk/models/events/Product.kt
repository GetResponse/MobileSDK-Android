package com.getresponse.mobile_sdk.models.events

data class Product(
    val id: String,
    val sku: String? = null,
    val name: String? = null,
    val vendor: String? = null,
    val price: Double? = null,
    val currency: String? = null,
)