package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWORenewTokenRequest(
    @SerializedName("current_flat_token")
    val currentFlatToken: String?,

    @SerializedName("device_id")
    val deviceId: String?
)
