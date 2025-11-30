package com.devconsole.auth_sdk

import android.content.Context
import androidx.activity.result.ActivityResult
import com.devconsole.auth_sdk.data.AuthState
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.delegate.AuthDelegateProvider
import com.devconsole.auth_sdk.delegate.DefaultDelegateProvider
import com.devconsole.auth_sdk.session.SessionData
import com.devconsole.auth_sdk.session.SessionManager
import kotlinx.coroutines.flow.StateFlow

class AuthManager(
    private val context: Context,
    ONEConfig: Configuration.ONE.Auth,
    TWOConfig: Configuration.TWO.Auth,
    delegateProvider: AuthDelegateProvider = DefaultDelegateProvider,
) {

    private val authApi: AuthApi by lazy { delegateProvider.provide()(context, ONEConfig, TWOConfig) }

    private val sessionManager = SessionManager(context)

    fun fetchAuthState(): StateFlow<AuthState> {
        return authApi.state
    }

    fun login() {
        authApi.login()
    }

    fun register() {
        authApi.register()
    }

    fun logout() {
        authApi.logout()
    }

    fun handleONEResponse(result: ActivityResult) {
        authApi.handleIntentResult(result)
    }

    fun getCurrentSession(): SessionData? {
        return sessionManager.getSession()
    }

    fun fetchSessionState(): StateFlow<Boolean> {
        return authApi.sessionState
    }

    fun loginWithGoogleReceipt(purchaseToken: String) {
        authApi.loginWithGoogleReceipt(purchaseToken)
    }

    fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?
    ) {
        authApi.submitGoogleReceiptAndLinkAccount(
            purchaseToken = purchaseToken, sku = sku,
            username = username, password = password,
            packageName = packageName, accountToken = accountToken
        )
    }

    fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?
    ) {
        authApi.submitGoogleReceipt(currentPurchaseToken, previousPurchaseToken, sku, packageName)
    }

    fun refreshSession(): Boolean {
        return authApi.refreshToken()
    }
}
