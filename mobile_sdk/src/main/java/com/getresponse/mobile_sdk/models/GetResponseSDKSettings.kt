package com.getresponse.mobile_sdk.models

import androidx.annotation.DrawableRes
import com.getresponse.mobile_sdk.models.push_notifications.AndroidChannel

data class GetResponseSDKSettings(
    private val enablePushNotifications: Boolean = true, private val enableWebEvents: Boolean = true,
    @DrawableRes val notificationIcon: Int? = null,
    val channelsConfig: List<AndroidChannel> = emptyList(),
    val enableDebug: Boolean = false,
)
