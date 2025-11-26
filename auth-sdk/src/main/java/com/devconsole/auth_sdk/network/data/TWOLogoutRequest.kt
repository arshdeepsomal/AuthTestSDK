package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWOLogoutRequest(
    @SerializedName("id_token")
    val idToken: String?,

    @SerializedName("flat_token")
    val flatToken: String?
)
