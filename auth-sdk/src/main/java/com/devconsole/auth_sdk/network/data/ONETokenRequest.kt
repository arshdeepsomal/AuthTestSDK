package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class ONETokenRequest(
    @SerializedName("code")
    val code: String,

    @SerializedName("grant_type")
    val grantType: String,

    @SerializedName("redirect_uri")
    val redirectUri: String,

    @SerializedName("client_id")
    val clientId: String,

    @SerializedName("client_secret")
    val clientSecret: String,

    @SerializedName("scope")
    val scope: String,

    @SerializedName("code_verifier")
    val codeVerifier: String,
)
