package com.devconsole.auth_sdk.auth.client

import com.devconsole.auth_sdk.auth.network.AuthNetworkDataSource
import com.devconsole.auth_sdk.auth.network.DefaultAuthNetworkDataSource
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.session.SessionData
import com.devconsole.auth_sdk.session.SessionManager

internal class TwoAuthClient(
    private val config: Configuration.TWO.Auth,
    private val sessionManager: SessionManager,
    private val networkDataSource: AuthNetworkDataSource = DefaultAuthNetworkDataSource(),
) {

    fun currentSession(): SessionData? = sessionManager.getSession()

    fun hasTokenExpired(): Boolean = sessionManager.hasTokenExpired()

    suspend fun loginWithOneToken(oneData: ONETokenData): Result<SessionData> {
        val twoLoginRequest = TWOLoginRequest(
            accessToken = oneData.accessToken.toString(),
            brand = config.brand,
            source = config.source,
            respondWithJwt = true,
            deviceId = config.deviceId,
            respondWithUsername = true,
        )

        return networkDataSource.loginTwo(twoLoginRequest, config.authorization, config.baseUrl)
            .mapCatching { twoTokenData ->
                if (twoTokenData.success == true) {
                    createSession(twoTokenData, oneData).also { sessionManager.saveSession(it) }
                } else {
                    throw IllegalStateException("Something went wrong.")
                }
            }
    }

    suspend fun logout(): Result<Unit> {
        val currentSession = currentSession()
        val twoLogoutRequest = TWOLogoutRequest(
            idToken = currentSession?.ONETokenData?.idToken,
            flatToken = currentSession?.TWOTokenData?.encodedJwt,
        )

        return networkDataSource.logoutTwo(twoLogoutRequest, config.authorization, config.baseUrl)
            .mapCatching {
                sessionManager.clearSession()
                Unit
            }
    }

    suspend fun renewSession(): Result<SessionData> {
        val currentSession = currentSession()
            ?: return Result.failure(IllegalStateException("No session found"))

        val twoRenewTokenRequest = TWORenewTokenRequest(
            currentFlatToken = currentSession.TWOTokenData.encodedJwt,
            deviceId = config.deviceId,
        )

        return networkDataSource.renewTwoToken(twoRenewTokenRequest, config.authorization, config.baseUrl)
            .mapCatching { newTokenData ->
                val updatedSession = currentSession.copy(
                    TWOTokenData = currentSession.TWOTokenData.copy(
                        encodedJwt = newTokenData.encodedJwt,
                        sessionToken = newTokenData.sessionToken,
                        sessionTokenExpiry = newTokenData.sessionTokenExpiry,
                    )
                )
                sessionManager.saveSession(updatedSession)
                updatedSession
            }
    }

    suspend fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?,
    ): Result<SessionData> {
        val submitGoogleReceiptDataLinkAccount = SubmitGoogleReceiptDataLinkAccount(
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

        return networkDataSource.submitGoogleReceiptAndLinkAccount(
            submitGoogleReceiptDataLinkAccount,
            config.authorization,
            config.baseUrl,
        ).mapCatching { receiptData ->
            validateReceiptData(receiptData)
        }
    }

    suspend fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?,
    ): Result<SessionData> {
        val submitGoogleData = SubmitGoogleData(
            currentPurchaseToken = currentPurchaseToken,
            previousPurchaseToken = previousPurchaseToken,
            brand = config.brand,
            source = config.source,
            respondWithJwt = true,
            deviceId = config.deviceId,
            packageName = packageName,
            productId = sku,
        )

        return networkDataSource.submitGoogleReceipt(
            submitGoogleData,
            config.authorization,
            config.baseUrl,
        ).mapCatching { receiptData ->
            validateReceiptData(receiptData)
        }
    }

    suspend fun loginWithGoogleReceipt(purchaseToken: String): Result<SessionData> {
        val twoGoogleReceiptLoginRequest = TWOGoogleReceiptLoginRequest(
            purchaseToken = purchaseToken,
            brand = config.brand,
            source = config.source,
            respondWithJwt = true,
            deviceId = config.deviceId,
            respondWithUsername = true,
        )

        return networkDataSource.loginWithGoogleReceipt(
            twoGoogleReceiptLoginRequest,
            config.authorization,
            config.baseUrl,
        ).mapCatching { receiptData ->
            validateReceiptData(receiptData)
        }
    }

    private fun validateReceiptData(receiptData: SubmitReceiptData): SessionData {
        if (receiptData.success == true) {
            return createSessionFromReceipt(receiptData).also { sessionManager.saveSession(it) }
        }

        throw IllegalStateException(receiptData.message ?: "Unable to process receipt")
    }

    private fun createSession(twoTokenData: TWOTokenData, oneData: ONETokenData): SessionData {
        return SessionData(
            authorizationCode = config.authorization,
            ONETokenData = oneData,
            TWOTokenData = twoTokenData,
        )
    }

    private fun createSessionFromReceipt(receiptData: SubmitReceiptData): SessionData {
        return SessionData(
            authorizationCode = config.authorization,
            ONETokenData = ONETokenData(),
            TWOTokenData = TWOTokenData(
                success = receiptData.success,
                status = receiptData.status,
                sessionToken = receiptData.sessionToken,
                sessionTokenExpiry = receiptData.sessionTokenExpiry,
                supportToken = receiptData.supportToken,
                encodedJwt = receiptData.encodedJwt,
                username = receiptData.username,
                processingTime = receiptData.processingTime,
            ),
        )
    }
}
