package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class ONEGetTokenForPKRequest(
    @SerializedName("client_id")
    val clientId: String?,

    @SerializedName("client_secret")
    val clientSecret: String?
)
