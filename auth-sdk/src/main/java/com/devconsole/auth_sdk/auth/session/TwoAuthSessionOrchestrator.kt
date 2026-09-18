package com.devconsole.auth_sdk.auth.session

import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.data.remote.TwoAuthRemoteDataSource
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.session.SessionController
import com.devconsole.auth_sdk.session.SessionData

internal class TwoAuthSessionOrchestrator(
    private val twoConfig: Configuration.TWO.Auth,
    private val remoteDataSource: TwoAuthRemoteDataSource,
    private val sessionController: SessionController
) {

    fun currentSession(): SessionData? = sessionController.currentSession()

    suspend fun completeLogin(oneTokenData: ONETokenData): Result<SessionData> {
        val accessToken = oneTokenData.accessToken
            ?: return Result.failure(IllegalStateException("Missing access token"))
        val request = TWOLoginRequest(
            accessToken = accessToken,
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            respondWithUsername = true
        )

        return remoteDataSource.login(request).map { tokenData ->
            SessionData(
                authorizationCode = twoConfig.authorization,
                ONETokenData = oneTokenData,
                TWOTokenData = tokenData
            ).also(sessionController::onSessionEstablished)
        }
    }

    suspend fun logout(): Result<Unit> {
        val session = sessionController.currentSession()
        val request = TWOLogoutRequest(
            idToken = session?.ONETokenData?.idToken,
            flatToken = session?.TWOTokenData?.encodedJwt
        )

        val result = remoteDataSource.logout(request)
        sessionController.clearSession()
        return result
    }

    suspend fun refreshSession(): Result<SessionData> {
        val session = sessionController.currentSession()
            ?: return Result.failure(IllegalStateException("No active session"))
        val request = TWORenewTokenRequest(
            currentFlatToken = session.TWOTokenData.encodedJwt,
            deviceId = twoConfig.deviceId
        )

        return remoteDataSource.renewToken(request).map { renewData ->
            val updatedSession = session.copy(
                TWOTokenData = session.TWOTokenData.copy(
                    encodedJwt = renewData.encodedJwt,
                    sessionToken = renewData.sessionToken,
                    sessionTokenExpiry = renewData.sessionTokenExpiry
                )
            )
            sessionController.onSessionEstablished(updatedSession)
            updatedSession
        }
    }

    suspend fun loginWithGoogleReceipt(purchaseToken: String): Result<SessionData> {
        val request = TWOGoogleReceiptLoginRequest(
            purchaseToken = purchaseToken,
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            respondWithUsername = true
        )

        return remoteDataSource.loginWithGoogleReceipt(request).toSessionResult()
    }

    suspend fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?
    ): Result<SessionData> {
        val request = SubmitGoogleData(
            currentPurchaseToken = currentPurchaseToken,
            previousPurchaseToken = previousPurchaseToken,
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            packageName = packageName,
            productId = sku
        )

        return remoteDataSource.submitGoogleReceipt(request).toSessionResult()
    }

    suspend fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?
    ): Result<SessionData> {
        val request = SubmitGoogleReceiptDataLinkAccount(
            purchaseToken = purchaseToken,
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            username = username,
            password = password,
            accountToken = accountToken,
            packageName = packageName,
            productId = sku
        )

        return remoteDataSource.submitGoogleReceiptAndLinkAccount(request).toSessionResult()
    }

    private fun Result<SubmitReceiptData>.toSessionResult(): Result<SessionData> {
        return mapCatching { receipt ->
            if (receipt.success == true) {
                SessionData(
                    authorizationCode = twoConfig.authorization,
                    ONETokenData = ONETokenData(),
                    TWOTokenData = receipt.toTwoTokenData()
                ).also(sessionController::onSessionEstablished)
            } else {
                error(receipt.message ?: "Receipt flow failed.")
            }
        }
    }

    private fun SubmitReceiptData.toTwoTokenData(): TWOTokenData {
        return TWOTokenData(
            success = success,
            status = status,
            sessionToken = sessionToken,
            sessionTokenExpiry = sessionTokenExpiry,
            supportToken = supportToken,
            encodedJwt = encodedJwt,
            username = username,
            processingTime = processingTime
        )
    }
}

