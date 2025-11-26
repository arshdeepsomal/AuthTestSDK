package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class SubmitGoogleReceiptDataLinkAccount(
    @SerializedName("username")
    val username: String?,

    @SerializedName("password")
    val password: String?,

    @SerializedName("account_token")
    val accountToken: String?,

    @SerializedName("purchase_token")
    val purchaseToken: String,

    @SerializedName("package_name")
    val packageName: String?,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("brand")
    val brand: String,

    @SerializedName("source")
    val source: String,

    @SerializedName("respond_with_jwt")
    val respondWithJwt: Boolean,

    @SerializedName("device_id")
    val deviceId: String?
)
