package com.getresponse.mobile_sdk

import com.google.gson.annotations.SerializedName

data class AndroidChannel(val id: String, val name: String, val importance: Int)

data class ConsentModel(
    val lang: String,
    @SerializedName(value = "external_id")
    val externalId: String,
    val email: String?,
    @SerializedName(value = "fcm_token")
    val fcmToken: String,
    val platform: String = "android"
)

enum class EventType(private val urlName: String) {
    SHOWED("sh"),
    CLICKED("cl");

    fun getEventUrl(url: String): String {
        return "${url}act=${this.urlName}"
    }
}