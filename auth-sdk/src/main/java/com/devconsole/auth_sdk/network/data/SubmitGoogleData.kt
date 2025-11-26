package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class SubmitGoogleData(
    @SerializedName("current_purchase_token")
    val currentPurchaseToken: String?,

    @SerializedName("previous_purchase_token")
    val previousPurchaseToken: String?,

    @SerializedName("package_name")
    val packageName: String?,

    @SerializedName("product_id")
    val productId: String?,

    @SerializedName("brand")
    val brand: String?,

    @SerializedName("source")
    val source: String?,

    @SerializedName("respond_with_jwt")
    val respondWithJwt: Boolean,

    @SerializedName("device_id")
    val deviceId: String?
)
