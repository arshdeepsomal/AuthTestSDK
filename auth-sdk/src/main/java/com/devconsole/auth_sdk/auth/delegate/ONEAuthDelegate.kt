package com.devconsole.auth_sdk.auth.delegate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import com.devconsole.auth_sdk.auth.api.AuthApi
import com.devconsole.auth_sdk.auth.client.OneAuthClient
import com.devconsole.auth_sdk.auth.client.TwoAuthClient
import com.devconsole.auth_sdk.auth.handler.ReceiptResultHandler
import com.devconsole.auth_sdk.auth.model.AuthState
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.auth.model.ONEAuthException
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.core.session.SessionData
import com.devconsole.auth_sdk.core.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException

@RequiresApi(Build.VERSION_CODES.O)
internal class ONEAuthDelegate(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val oneConfig: Configuration.ONE.Auth,
    private val twoConfig: Configuration.TWO.Auth,
) : AuthApi {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    override val state: StateFlow<AuthState> = _state

    private val oneAuthClient = OneAuthClient(context, oneConfig)
    private val twoAuthClient = TwoAuthClient(twoConfig)

    private val receiptHandler = ReceiptResultHandler(::saveSession, ::handleError)
    override val sessionState: StateFlow<Boolean> = sessionManager.sessionState

    init {
        coroutineScope.launch {
            val isActive = if (sessionManager.hasTokenExpired()) refreshToken() else true
            sessionManager.setSessionState(isActive)
        }
    }

    override fun login() {
        launchAuthIntent { oneAuthClient.buildLoginIntent() }
    }

    override fun register() {
        launchAuthIntent { buildRegisterIntent() }
    }

    private suspend fun buildRegisterIntent(): Intent {
        val tokenForPrivateKey = twoAuthClient.fetchPrivateKeyToken(oneConfig.clientSecret).getOrThrow()
        val privateKey = twoAuthClient.fetchPrivateKey(tokenForPrivateKey).getOrThrow()
        return oneAuthClient.buildRegisterIntent(privateKey)
    }

    override fun handleIntentResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        handleAuthResult(result.data)
    }

    private fun handleAuthResult(resultData: Intent?) {
        if (resultData == null) {
            _state.value = AuthState.Error(Exception("null result"))
            return
        }

        AuthorizationException.fromIntent(resultData)?.let {
            handleAuthorizationError(it)
            return
        }

        _state.value = AuthState.Loading

        coroutineScope.launch {
            oneAuthClient.exchangeToken(resultData)
                .onSuccess { data -> initializeTwoLogin(data) }
                .onFailure { _state.value = AuthState.Error(it) }
        }
    }

    private fun handleAuthorizationError(exception: AuthorizationException) {
        when (exception.errorDescription) {
            "register" -> register()
            "signIn", "username" -> login()
            "cancel_register" -> _state.value = AuthState.Error(exception)
            else -> _state.value = AuthState.Error(
                ONEAuthException(
                    errorCode = exception.code,
                    errorDescription = exception.errorDescription
                )
            )
        }
    }

    private fun initializeTwoLogin(oneData: ONETokenData) {
        coroutineScope.launch {
            twoAuthClient.loginWithOneToken(oneData.accessToken.toString())
                .onSuccess { twoData -> saveSession(twoData, oneData) }
                .onFailure { handleError(it) }
        }
    }

    override fun logout() {
        _state.value = AuthState.Loading
        val currentSession = sessionManager.getSession()
        val logoutRequest = TWOLogoutRequest(
            idToken = currentSession?.ONETokenData?.idToken,
            flatToken = currentSession?.TWOTokenData?.encodedJwt
        )
        coroutineScope.launch {
            twoAuthClient.logout(logoutRequest).onSuccess { handleLogout() }.onFailure { handleLogout() }
        }
    }

    private fun handleLogout() {
        sessionManager.clearSession()
        _state.value = AuthState.LogoutSuccess
    }

    override suspend fun refreshToken(): Boolean {
        val currentSession = sessionManager.getSession() ?: return false

        _state.value = AuthState.Loading

        return twoAuthClient.renewToken(currentSession.TWOTokenData.encodedJwt)
            .fold(
                onSuccess = { newTokenData ->
                    val updatedSession = currentSession.copy(
                        TWOTokenData = currentSession.TWOTokenData.copy(
                            encodedJwt = newTokenData.encodedJwt,
                            sessionToken = newTokenData.sessionToken,
                            sessionTokenExpiry = newTokenData.sessionTokenExpiry
                        )
                    )
                    sessionManager.saveSession(updatedSession)
                    _state.value = AuthState.AuthSuccess(updatedSession)
                    true
                },
                onFailure = {
                    handleError(it)
                    sessionManager.setSessionState(false)
                    false
                }
            )
    }

    override fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?,
    ) {
        _state.value = AuthState.Loading

        coroutineScope.launch {
            twoAuthClient.submitGoogleReceiptAndLinkAccount(
                purchaseToken = purchaseToken,
                sku = sku,
                username = username,
                password = password,
                packageName = packageName,
                accountToken = accountToken,
            ).let { receiptHandler.handle(it) }
        }
    }

    override fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?,
    ) {
        _state.value = AuthState.Loading

        coroutineScope.launch {
            twoAuthClient.submitGoogleReceipt(
                currentPurchaseToken = currentPurchaseToken,
                previousPurchaseToken = previousPurchaseToken,
                sku = sku,
                packageName = packageName,
            ).let { receiptHandler.handle(it) }
        }
    }

    override fun loginWithGoogleReceipt(purchaseToken: String) {
        _state.value = AuthState.Loading

        coroutineScope.launch {
            twoAuthClient.loginWithGoogleReceipt(purchaseToken).let { receiptHandler.handle(it) }
        }
    }

    private fun saveSession(twoTokenData: TWOTokenData, oneTokenData: ONETokenData) {
        val sessionData = SessionData(
            authorizationCode = twoConfig.authorization,
            ONETokenData = oneTokenData,
            TWOTokenData = twoTokenData,
        )
        sessionManager.saveSession(sessionData)
        _state.value = AuthState.AuthSuccess(sessionData)
    }

    private fun handleError(throwable: Throwable) {
        _state.value = AuthState.Error(throwable)
    }

    private fun launchAuthIntent(intentBuilder: suspend () -> Intent) {
        _state.value = AuthState.Loading
        coroutineScope.launch {
            runCatching { intentBuilder() }
                .onSuccess { _state.value = AuthState.LaunchIntent(it) }
                .onFailure { handleError(it) }
        }
    }
}
