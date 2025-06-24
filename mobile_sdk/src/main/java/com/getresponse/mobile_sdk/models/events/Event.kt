package com.getresponse.mobile_sdk.models.events

data class Event<T>(
    val version: String,
    val name: String,
    val data: T
)