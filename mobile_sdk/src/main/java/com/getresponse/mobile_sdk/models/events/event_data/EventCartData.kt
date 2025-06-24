package com.getresponse.mobile_sdk.models.events.event_data

import com.getresponse.mobile_sdk.models.events.OrderProduct
import com.google.gson.annotations.SerializedName

data class EventCartData(
    val price: Double,
    @SerializedName("cart_id")
    val cartId: String? = null,
    @SerializedName("cart_url")
    val cartUrl: String,
    val currency: String,
    val products: List<OrderProduct>
) : EventData()
