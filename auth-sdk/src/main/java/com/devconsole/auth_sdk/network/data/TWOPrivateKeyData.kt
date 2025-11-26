package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class TWOPrivateKeyData(
    @SerializedName("private_key")
    val privateKey: String?,

    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("error")
    val error: String?
)
