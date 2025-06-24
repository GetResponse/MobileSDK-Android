package com.getresponse.mobile_sdk.models.push_notifications

import com.google.gson.annotations.SerializedName

data class ConsentModel(
    val lang: String,
    @SerializedName(value = "external_id")
    val externalId: String,
    val email: String?,
    @SerializedName(value = "fcm_token")
    val fcmToken: String,
    val platform: String = "android"
)