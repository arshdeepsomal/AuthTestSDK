package com.devconsole.auth_sdk.auth.network

import com.devconsole.auth_sdk.network.api.ONEAuthService
import com.devconsole.auth_sdk.network.api.TWOAuthService
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.data.ONEGetTokenForPKData
import com.devconsole.auth_sdk.network.data.ONEGetTokenForPKRequest
import com.devconsole.auth_sdk.network.data.ONEPrivateKeyData
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
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

internal interface AuthNetworkDataSource {
    suspend fun requestOneToken(request: ONETokenRequest, baseUrl: String): Result<ONETokenData>
    suspend fun loginTwo(request: TWOLoginRequest, authorization: String?, baseUrl: String): Result<TWOTokenData>
    suspend fun logoutTwo(request: TWOLogoutRequest, authorization: String?, baseUrl: String): Result<Unit>
    suspend fun renewTwoToken(request: TWORenewTokenRequest, authorization: String?, baseUrl: String): Result<TWORenewTokenData>
    suspend fun loginWithGoogleReceipt(request: TWOGoogleReceiptLoginRequest, authorization: String?, baseUrl: String): Result<SubmitReceiptData>
    suspend fun submitGoogleReceipt(request: SubmitGoogleData, authorization: String?, baseUrl: String): Result<SubmitReceiptData>
    suspend fun submitGoogleReceiptAndLinkAccount(request: SubmitGoogleReceiptDataLinkAccount, authorization: String?, baseUrl: String): Result<SubmitReceiptData>
    suspend fun fetchPrivateKeyToken(request: ONEGetTokenForPKRequest, baseUrl: String): Result<ONEGetTokenForPKData>
    suspend fun fetchPrivateKey(authorization: String, clientId: String, baseUrl: String): Result<ONEPrivateKeyData>
}

internal class DefaultAuthNetworkDataSource : AuthNetworkDataSource {

    override suspend fun requestOneToken(
        request: ONETokenRequest,
        baseUrl: String
    ): Result<ONETokenData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(ONEAuthService::class.java)
            .getToken(request)
            .toResult("null one token response")
    }

    override suspend fun loginTwo(
        request: TWOLoginRequest,
        authorization: String?,
        baseUrl: String
    ): Result<TWOTokenData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(TWOAuthService::class.java)
            .login(authorization, request)
            .toResult("null two login response")
    }

    override suspend fun logoutTwo(
        request: TWOLogoutRequest,
        authorization: String?,
        baseUrl: String
    ): Result<Unit> {
        return RetrofitManager.getInstance(baseUrl)
            .create(TWOAuthService::class.java)
            .logout(authorization, request)
            .toUnitResult("null two logout response")
    }

    override suspend fun renewTwoToken(
        request: TWORenewTokenRequest,
        authorization: String?,
        baseUrl: String
    ): Result<TWORenewTokenData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(TWOAuthService::class.java)
            .renewToken(authorization, request)
            .toResult("null renew token response")
    }

    override suspend fun loginWithGoogleReceipt(
        request: TWOGoogleReceiptLoginRequest,
        authorization: String?,
        baseUrl: String
    ): Result<SubmitReceiptData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(TWOAuthService::class.java)
            .loginWithGoogleReceipt(authorization, request)
            .toResult("null google receipt login response")
    }

    override suspend fun submitGoogleReceipt(
        request: SubmitGoogleData,
        authorization: String?,
        baseUrl: String
    ): Result<SubmitReceiptData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(TWOAuthService::class.java)
            .submitGoogleReceipt(authorization, request)
            .toResult("null google receipt submit response")
    }

    override suspend fun submitGoogleReceiptAndLinkAccount(
        request: SubmitGoogleReceiptDataLinkAccount,
        authorization: String?,
        baseUrl: String
    ): Result<SubmitReceiptData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(TWOAuthService::class.java)
            .submitGoogleReceiptAndLinkAccount(authorization, request)
            .toResult("null google receipt link response")
    }

    override suspend fun fetchPrivateKeyToken(
        request: ONEGetTokenForPKRequest,
        baseUrl: String
    ): Result<ONEGetTokenForPKData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(ONEAuthService::class.java)
            .getTokenForPrivateKey(request)
            .toResult("null private key token response")
    }

    override suspend fun fetchPrivateKey(
        authorization: String,
        clientId: String,
        baseUrl: String
    ): Result<ONEPrivateKeyData> {
        return RetrofitManager.getInstance(baseUrl)
            .create(ONEAuthService::class.java)
            .getPrivateKey(authorization, clientId)
            .toResult("null private key response")
    }
}

private fun <T> Response<T>?.toResult(nullMessage: String): Result<T> {
    val response = this ?: return Result.failure(IllegalStateException(nullMessage))
    if (response.isSuccessful) {
        response.body()?.let { return Result.success(it) }
    }
    return Result.failure(IllegalStateException(response.errorBody().toString()))
}

private fun Response<Any>?.toUnitResult(nullMessage: String): Result<Unit> {
    val response = this ?: return Result.failure(IllegalStateException(nullMessage))
    return if (response.isSuccessful) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalStateException(response.errorBody().toString()))
    }
}
