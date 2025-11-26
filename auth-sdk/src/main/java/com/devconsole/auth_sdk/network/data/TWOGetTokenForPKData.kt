package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWOGetTokenForPKData(
    @SerializedName("access_token")
    val accessToken: String?,

    @SerializedName("expires_in")
    val expiresIn: Int?,

    @SerializedName("grant_type")
    val grantType: String?,

    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("error")
    val error: String?
)
