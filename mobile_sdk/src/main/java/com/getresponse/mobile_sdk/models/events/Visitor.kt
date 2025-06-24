package com.getresponse.mobile_sdk.models.events

import com.google.gson.annotations.SerializedName

data class Visitor(
    @SerializedName("installation_uuid")
    val installationUUID: String,
    val ip: String? = null
)
