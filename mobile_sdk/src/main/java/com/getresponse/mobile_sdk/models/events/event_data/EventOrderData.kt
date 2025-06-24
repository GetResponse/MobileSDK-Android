package com.getresponse.mobile_sdk.models.events.event_data

import com.getresponse.mobile_sdk.models.events.OrderProduct
import com.getresponse.mobile_sdk.models.events.Shop
import com.google.gson.annotations.SerializedName

data class EventOrderData(
    val shop: Shop? = null,
    val price: Double,
    @SerializedName("cart_id")
    val cartId: String? = null,
    @SerializedName("order_id")
    val orderId: String,
    val currency: String,
    val products: List<OrderProduct>
) : EventData()
