package com.getresponse.mobile_sdk.models.events.event_data

import com.getresponse.mobile_sdk.models.events.Category
import com.getresponse.mobile_sdk.models.events.Product
import com.getresponse.mobile_sdk.models.events.Shop

data class EventItemData(
    val shop: Shop? = null,
    val product: Product,
    val categories: List<Category>? = null,
) : EventData()
