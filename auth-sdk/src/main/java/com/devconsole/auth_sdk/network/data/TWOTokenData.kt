package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWOTokenData(

    @SerializedName("success")
    val success: Boolean? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("session_token")
    val sessionToken: String? = null,

    @SerializedName("session_token_expiry")
    val sessionTokenExpiry: Long? = null,

    @SerializedName("support_token")
    val supportToken: String? = null,

    @SerializedName("encoded_jwt")
    val encodedJwt: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("processing_time_ms")
    val processingTime: String? = null
)
