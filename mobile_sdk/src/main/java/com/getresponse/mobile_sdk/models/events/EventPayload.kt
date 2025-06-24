package com.getresponse.mobile_sdk.models.events

import com.getresponse.mobile_sdk.models.events.event_data.EventData
import com.google.gson.annotations.SerializedName

data class EventPayload<T : EventData>(
    val version: String = "1.0",
    @SerializedName("occurred_on")
    val occurredOn: String,
    val time: Int? = null,
    val app: AppInfo,
    val visitor: Visitor,
    val channel: String = Channel.MOBILE.value,
    @SerializedName("user_uuid")
    val userUUID: String,
    val path: String = "",
    val event: Event<T>,
    val tags: List<String>? = null,
    val geo: GeoLocation? = null
)

