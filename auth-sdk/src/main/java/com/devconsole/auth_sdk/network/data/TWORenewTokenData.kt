package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWORenewTokenData(
    @SerializedName("encoded_jwt")
    val encodedJwt: String?,

    @SerializedName("session_token")
    val sessionToken: String?,

    @SerializedName("session_token_expiry")
    val sessionTokenExpiry: Long? = null,
)
