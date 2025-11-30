package com.devconsole.auth_sdk.network.api

import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWORenewTokenData
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TWOAuthService {

    @POST("user/login")
    suspend fun login(
        @Header("Authorization") clientApiKey: String?,
        @Body body: TWOLoginRequest
    ): Response<TWOTokenData>?

    @POST("user/logout")
    suspend fun logout(
        @Header("Authorization") clientApiKey: String?,
        @Body body: TWOLogoutRequest
    ): Response<Any>?

    @POST("/user/renew_token")
    suspend fun renewToken(
        @Header("Authorization") clientApiKey: String?,
        @Body body: TWORenewTokenRequest
    ): Response<TWORenewTokenData>?

    @POST("/iap/googleplay_receipt_login")
    suspend fun loginWithGoogleReceipt(
        @Header("Authorization") clientApiKey: String?,
        @Body body: TWOGoogleReceiptLoginRequest
    ): Response<SubmitReceiptData>?

    @POST("/iap/submit_googleplay_receipt_and_link_account")
    suspend fun submitGoogleReceiptAndLinkAccount(
        @Header("Authorization") clientApiKey: String?,
        @Body body: SubmitGoogleReceiptDataLinkAccount
    ): Response<SubmitReceiptData>?

    @POST("/iap/submit_googleplay_receipt")
    suspend fun submitGoogleReceipt(
        @Header("Authorization") clientApiKey: String?,
        @Body body: SubmitGoogleData
    ): Response<SubmitReceiptData>?
}
