package com.devconsole.auth_sdk.auth

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import com.devconsole.auth_sdk.AuthApi
import com.devconsole.auth_sdk.auth.browser.BrowserAuthFlow
import com.devconsole.auth_sdk.auth.session.TwoAuthSessionOrchestrator
import com.devconsole.auth_sdk.auth.state.AuthStatePublisher
import com.devconsole.auth_sdk.core.DefaultDispatcherProvider
import com.devconsole.auth_sdk.core.DispatcherProvider
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.data.ONEAuthException
import com.devconsole.auth_sdk.data.remote.OneAuthRemoteDataSource
import com.devconsole.auth_sdk.data.remote.TwoAuthRemoteDataSource
import com.devconsole.auth_sdk.delegate.DefaultAuthServiceProvider
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.session.SessionController
import com.devconsole.auth_sdk.session.SessionData
import com.devconsole.auth_sdk.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationException

internal class DefaultAuthController(
    context: Context,
    oneConfig: Configuration.ONE.Auth,
    twoConfig: Configuration.TWO.Auth,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : AuthApi {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    private val sessionController = SessionController(SessionManager(context), dispatcherProvider, scope)
    private val authStatePublisher = AuthStatePublisher()

    private val oneRemoteDataSource = OneAuthRemoteDataSource(oneConfig, twoConfig)
    private val browserAuthFlow = BrowserAuthFlow(
        context = context,
        oneConfig = oneConfig,
        remoteDataSource = oneRemoteDataSource,
        dispatcherProvider = dispatcherProvider,
        authServiceProvider = DefaultAuthServiceProvider
    )
    private val twoRemoteDataSource = TwoAuthRemoteDataSource(twoConfig)
    private val sessionOrchestrator = TwoAuthSessionOrchestrator(
        twoConfig = twoConfig,
        remoteDataSource = twoRemoteDataSource,
        sessionController = sessionController
    )

    override val state = authStatePublisher.state
    override val sessionState = sessionController.sessionState

    override fun login() {
        authStatePublisher.loading()
        authStatePublisher.launch(browserAuthFlow.createLoginIntent())
    }

    override fun register() {
        authStatePublisher.loading()
        scope.launch {
            browserAuthFlow.createRegisterIntent()
                .onSuccess(authStatePublisher::launch)
                .onFailure(authStatePublisher::error)
        }
    }

    override fun handleIntentResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        val resultData = result.data ?: run {
            authStatePublisher.error(IllegalStateException("null result"))
            return
        }

        AuthorizationException.fromIntent(resultData)?.let { exception ->
            handleAuthorizationException(exception)
            return
        }

        authStatePublisher.loading()
        scope.launch {
            browserAuthFlow.exchangeToken(result)
                .onSuccess { oneToken ->
                    completeTwoLogin(oneToken)
                }
                .onFailure(authStatePublisher::error)
        }
    }

    override fun logout() {
        authStatePublisher.loading()
        scope.launch {
            sessionOrchestrator.logout()
                .onFailure { /* swallow to keep logout ux identical */ }
            authStatePublisher.logout()
        }
    }

    override fun refreshToken(): Boolean {
        authStatePublisher.loading()
        return runBlocking(dispatcherProvider.io) {
            sessionOrchestrator.refreshSession()
                .onSuccess(authStatePublisher::success)
                .onFailure {
                    sessionController.markSessionInactive()
                    authStatePublisher.error(it)
                }
                .isSuccess
        }
    }

    override fun loginWithGoogleReceipt(purchaseToken: String) {
        authStatePublisher.loading()
        scope.launch {
            sessionOrchestrator.loginWithGoogleReceipt(purchaseToken)
                .onSuccess(authStatePublisher::success)
                .onFailure(authStatePublisher::error)
        }
    }

    override fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?
    ) {
        authStatePublisher.loading()
        scope.launch {
            sessionOrchestrator.submitGoogleReceiptAndLinkAccount(
                purchaseToken = purchaseToken,
                sku = sku,
                username = username,
                password = password,
                packageName = packageName,
                accountToken = accountToken
            ).onSuccess(authStatePublisher::success)
                .onFailure(authStatePublisher::error)
        }
    }

    override fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?
    ) {
        authStatePublisher.loading()
        scope.launch {
            sessionOrchestrator.submitGoogleReceipt(
                currentPurchaseToken = currentPurchaseToken,
                previousPurchaseToken = previousPurchaseToken,
                sku = sku,
                packageName = packageName
            ).onSuccess(authStatePublisher::success)
                .onFailure(authStatePublisher::error)
        }
    }

    override fun currentSession(): SessionData? = sessionOrchestrator.currentSession()

    private suspend fun completeTwoLogin(oneTokenData: ONETokenData) {
        sessionOrchestrator.completeLogin(oneTokenData)
            .onSuccess(authStatePublisher::success)
            .onFailure(authStatePublisher::error)
    }

    private fun handleAuthorizationException(exception: AuthorizationException) {
        when (exception.errorDescription) {
            "register" -> register()
            "signIn", "username" -> login()
            "cancel_register" -> authStatePublisher.error(exception)
            else -> authStatePublisher.error(
                ONEAuthException(
                    errorCode = exception.code,
                    errorDescription = exception.errorDescription
                )
            )
        }
    }

    override fun clear() {
        browserAuthFlow.clear()
        scope.cancel()
    }

    override fun loginAnonymous() {
        // added in auth controllers
    }
}

