package com.getresponse.mobile_sdk.models.push_notifications

enum class EventType(private val urlName: String) {
    SHOWED("sh"),
    CLICKED("cl");

    fun getEventUrl(url: String): String {
        return "${url}act=${this.urlName}"
    }
}