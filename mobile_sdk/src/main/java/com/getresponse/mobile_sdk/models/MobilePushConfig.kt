package com.getresponse.mobile_sdk.models

data class MobilePushConfig(
    val endpoint: String,
    val auth: AuthConfig,
    val options: List<String>
)
