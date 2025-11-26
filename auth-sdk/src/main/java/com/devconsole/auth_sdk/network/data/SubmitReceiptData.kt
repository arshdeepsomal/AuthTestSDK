package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class SubmitReceiptData(

    @SerializedName("success")
    val success: Boolean? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("is_linked")
    val isLinked: Boolean = false,

    @SerializedName("session_token")
    var sessionToken: String? = null,

    @SerializedName("session_token_expiry")
    var sessionTokenExpiry: Long? = null,

    @SerializedName("support_token")
    val supportToken: String? = null,

    @SerializedName("encoded_jwt")
    var encodedJwt: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("error_code")
    val errorCode: Int? = null,

    @SerializedName("user_message")
    val message: String? = null,

    @SerializedName("processing_time_ms")
    val processingTime: String? = null
)
