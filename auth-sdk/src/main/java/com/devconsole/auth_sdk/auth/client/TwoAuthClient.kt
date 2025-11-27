package com.devconsole.auth_sdk.auth.client

import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.api.TWOAuthService
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.network.data.TWOGetTokenForPKRequest
import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWORenewTokenData
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.data.SubmitReceiptData

internal class TwoAuthClient(
    private val config: Configuration.TWO.Auth,
) {

    suspend fun loginWithOneToken(accessToken: String): Result<TWOTokenData> {
        val request = TWOLoginRequest(
            accessToken = accessToken,
            brand = config.brand,
            source = config.source,
            respondWithJwt = true,
            deviceId = config.deviceId,
            respondWithUsername = true,
        )

        val response = RetrofitManager.getInstance(config.baseUrl)
            .create(TWOAuthService::class.java)
            .login(config.authorization, request)

        val tokenData = response?.body()?.takeIf { response.isSuccessful }
            ?: return Result.failure(Exception(response?.errorBody().toString()))

        return Result.success(tokenData)
    }

    suspend fun logout(request: TWOLogoutRequest): Result<Unit> {
        return runCatching {
            RetrofitManager.getInstance(config.baseUrl)
                .create(TWOAuthService::class.java)
                .logout(config.authorization, request)
            Unit
        }
    }

    suspend fun renewToken(currentToken: String?): Result<TWORenewTokenData> {
        val request = TWORenewTokenRequest(
            currentFlatToken = currentToken,
            deviceId = config.deviceId,
        )

        val response = RetrofitManager.getInstance(config.baseUrl)
            .create(TWOAuthService::class.java)
            .renewToken(config.authorization, request)

        val tokenData = response?.body()?.takeIf { response.isSuccessful }
            ?: return Result.failure(Exception(response?.errorBody().toString()))

        return Result.success(tokenData)
    }

    suspend fun fetchPrivateKeyToken(clientSecret: String): Result<String> {
        val response = RetrofitManager.getInstance(config.baseUrl)
            .create(TWOAuthService::class.java)
            .getTokenForPrivateKey(
                TWOGetTokenForPKRequest(
                    clientId = config.brand,
                    clientSecret = clientSecret,
                )
            )

        val token = response?.body()?.takeIf { response.isSuccessful && it.success == true }
            ?: return Result.failure(Exception(response?.body()?.error))

        return Result.success(token.accessToken.orEmpty())
    }

    suspend fun fetchPrivateKey(bearerToken: String): Result<String> {
        val response = RetrofitManager.getInstance(config.baseUrl)
            .create(TWOAuthService::class.java)
            .getPrivateKey("Bearer $bearerToken", clientId = config.brand)

        val token = response?.body()?.takeIf { response.isSuccessful && it.success == true }
            ?: return Result.failure(Exception(response?.body()?.error))

        return Result.success(token.privateKey.orEmpty())
    }

    suspend fun loginWithGoogleReceipt(purchaseToken: String): Result<SubmitReceiptData> {
        val request = TWOGoogleReceiptLoginRequest(
            purchaseToken = purchaseToken,
            brand = config.brand,
            source = config.source,
            respondWithJwt = true,
            deviceId = config.deviceId,
            respondWithUsername = true,
        )

        return handleReceiptResponse {
            RetrofitManager.getInstance(config.baseUrl)
                .create(TWOAuthService::class.java)
                .loginWithGoogleReceipt(config.authorization, request)
        }
    }

    suspend fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?,
    ): Result<SubmitReceiptData> {
        val request = SubmitGoogleReceiptDataLinkAccount(
            purchaseToken = purchaseToken,
            brand = config.brand,
            source = config.source,
            respondWithJwt = true,
            deviceId = config.deviceId,
            username = username,
            password = password,
            accountToken = accountToken,
            packageName = packageName,
            productId = sku,
        )

        return handleReceiptResponse {
            RetrofitManager.getInstance(config.baseUrl)
                .create(TWOAuthService::class.java)
                .submitGoogleReceiptAndLinkAccount(config.authorization, request)
        }
    }

    suspend fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?,
    ): Result<SubmitReceiptData> {
        val request = SubmitGoogleData(
            currentPurchaseToken = currentPurchaseToken,
            previousPurchaseToken = previousPurchaseToken,
            brand = config.brand,
            source = config.source,
            respondWithJwt = true,
            deviceId = config.deviceId,
            packageName = packageName,
            productId = sku,
        )

        return handleReceiptResponse {
            RetrofitManager.getInstance(config.baseUrl)
                .create(TWOAuthService::class.java)
                .submitGoogleReceipt(config.authorization, request)
        }
    }

    private suspend fun handleReceiptResponse(call: suspend () -> retrofit2.Response<SubmitReceiptData>?): Result<SubmitReceiptData> {
        val response = call.invoke()
        val receiptData = response?.body()?.takeIf { response.isSuccessful }
            ?: return Result.failure(Exception(response?.errorBody().toString()))
        return Result.success(receiptData)
    }
}
