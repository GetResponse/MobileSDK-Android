package com.getresponse.mobile_sdk.models.events

data class AppInfo(
    val os: String,
    val uuid: String,
    val lang: String,
    val device: String = "mobile"
)
