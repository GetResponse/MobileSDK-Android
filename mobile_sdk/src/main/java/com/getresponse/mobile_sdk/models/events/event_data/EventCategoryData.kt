package com.getresponse.mobile_sdk.models.events.event_data

import com.getresponse.mobile_sdk.models.events.Shop

data class EventCategoryData(
    val shop: Shop? = null,
    val id: String,
    val name: String? = null
) : EventData()
