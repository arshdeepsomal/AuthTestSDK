package com.devconsole.auth_sdk.data.remote

import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.api.TWOAuthService
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWORenewTokenData
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import retrofit2.Response

internal class TwoAuthRemoteDataSource(
    private val twoConfig: Configuration.TWO.Auth
) {

    private val service = RetrofitManager.getInstance(twoConfig.baseUrl)
        .create(TWOAuthService::class.java)

    suspend fun login(request: TWOLoginRequest): Result<TWOTokenData> {
        return execute { service.login(twoConfig.authorization, request) }
    }

    suspend fun logout(request: TWOLogoutRequest): Result<Unit> {
        return execute { service.logout(twoConfig.authorization, request) }.map { }
    }

    suspend fun renewToken(request: TWORenewTokenRequest): Result<TWORenewTokenData> {
        return execute { service.renewToken(twoConfig.authorization, request) }
    }

    suspend fun loginWithGoogleReceipt(request: TWOGoogleReceiptLoginRequest): Result<SubmitReceiptData> {
        return execute { service.loginWithGoogleReceipt(twoConfig.authorization, request) }
    }

    suspend fun submitGoogleReceipt(request: SubmitGoogleData): Result<SubmitReceiptData> {
        return execute { service.submitGoogleReceipt(twoConfig.authorization, request) }
    }

    suspend fun submitGoogleReceiptAndLinkAccount(request: SubmitGoogleReceiptDataLinkAccount): Result<SubmitReceiptData> {
        return execute { service.submitGoogleReceiptAndLinkAccount(twoConfig.authorization, request) }
    }

    private suspend fun <T> execute(block: suspend () -> Response<T>?): Result<T> {
        return runCatching {
            block() ?: error("Empty response")
        }.mapCatching { response ->
            if (response.isSuccessful) {
                response.body() ?: error("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                error(errorBody.ifBlank { "Network call failed." })
            }
        }
    }
}




