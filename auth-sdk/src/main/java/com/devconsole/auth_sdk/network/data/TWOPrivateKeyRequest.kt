package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWOPrivateKeyRequest(
    @SerializedName("client_id")
    val clientId: String?
)
