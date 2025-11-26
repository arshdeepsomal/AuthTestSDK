package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWOLoginRequest(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("brand")
    val brand: String,

    @SerializedName("source")
    val source: String,

    @SerializedName("respond_with_jwt")
    val respondWithJwt: Boolean,

    @SerializedName("device_id")
    val deviceId: String?,

    @SerializedName("respond_with_username")
    val respondWithUsername: Boolean
)
