package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class ONETokenData(

    @SerializedName("access_token")
    val accessToken: String? = null,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("id_token")
    val idToken: String? = null,

    @SerializedName("token_type")
    val tokenType: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Long? = null,
)
