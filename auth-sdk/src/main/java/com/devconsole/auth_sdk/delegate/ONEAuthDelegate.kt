package com.devconsole.auth_sdk.delegate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import com.devconsole.auth_sdk.AuthApi
import com.devconsole.auth_sdk.auth.OneAuthClient
import com.devconsole.auth_sdk.auth.SessionController
import com.devconsole.auth_sdk.auth.TwoAuthClient
import com.devconsole.auth_sdk.data.AuthState
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.data.ONEAuthException
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.session.SessionData
import com.devconsole.auth_sdk.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationException

@RequiresApi(Build.VERSION_CODES.O)
internal class ONEAuthDelegate(
    private val context: Context,
    private val oneConfig: Configuration.ONE.Auth,
    private val twoConfig: Configuration.TWO.Auth,
) : AuthApi {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    override val state: StateFlow<AuthState> = _state

    private val sessionManager = SessionManager(context)
    private val sessionController = SessionController(sessionManager)
    override val sessionState: StateFlow<Boolean> = sessionController.sessionState

    private val oneAuthClient = OneAuthClient(context, oneConfig)
    private val twoAuthClient = TwoAuthClient(twoConfig)

    init {
        sessionController.setSessionState(!isSessionExpired())
    }

    override fun login() {
        _state.value = AuthState.Loading
        _state.value = AuthState.LaunchIntent(oneAuthClient.buildLoginIntent())
    }

    override fun register() {
        _state.value = AuthState.Loading
        coroutineScope.launch {
            runCatching { buildRegisterIntent() }
                .onSuccess { _state.value = AuthState.LaunchIntent(it) }
                .onFailure { _state.value = AuthState.Error(it) }
        }
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
                .onFailure { _state.value = AuthState.Error(it) }
        }
    }

    override fun logout() {
        _state.value = AuthState.Loading
        val currentSession = sessionController.currentSession()
        val logoutRequest = TWOLogoutRequest(
            idToken = currentSession?.ONETokenData?.idToken,
            flatToken = currentSession?.TWOTokenData?.encodedJwt
        )
        coroutineScope.launch {
            twoAuthClient.logout(logoutRequest).onSuccess { handleLogout() }.onFailure { handleLogout() }
        }
    }

    private fun handleLogout() {
        sessionController.clear()
        _state.value = AuthState.LogoutSuccess
    }

    override fun refreshToken(): Boolean {
        val currentSession = sessionController.currentSession() ?: return false

        _state.value = AuthState.Loading
        var refreshSuccess = false

        runBlocking(Dispatchers.IO) {
            twoAuthClient.renewToken(currentSession.TWOTokenData.encodedJwt)
                .onSuccess { newTokenData ->
                    val updatedSession = currentSession.copy(
                        TWOTokenData = currentSession.TWOTokenData.copy(
                            encodedJwt = newTokenData.encodedJwt,
                            sessionToken = newTokenData.sessionToken,
                            sessionTokenExpiry = newTokenData.sessionTokenExpiry
                        )
                    )
                    sessionController.save(updatedSession)
                    _state.value = AuthState.AuthSuccess(updatedSession)
                    refreshSuccess = true
                }.onFailure {
                    _state.value = AuthState.Error(it)
                    sessionController.setSessionState(false)
                }
        }

        return refreshSuccess
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
            ).handleReceiptResponse()
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
            ).handleReceiptResponse()
        }
    }

    override fun loginWithGoogleReceipt(purchaseToken: String) {
        _state.value = AuthState.Loading

        coroutineScope.launch {
            twoAuthClient.loginWithGoogleReceipt(purchaseToken).handleReceiptResponse()
        }
    }

    private suspend fun Result<SubmitReceiptData>.handleReceiptResponse() {
        onSuccess { receiptData ->
            if (receiptData.success == true) {
                saveSession(receiptData.toTwoTokenData(), ONETokenData())
            } else {
                _state.value = AuthState.Error(Exception(receiptData.message))
            }
        }.onFailure { _state.value = AuthState.Error(it) }
    }

    private fun saveSession(twoTokenData: TWOTokenData, oneTokenData: ONETokenData) {
        val sessionData = SessionData(
            authorizationCode = twoConfig.authorization,
            ONETokenData = oneTokenData,
            TWOTokenData = twoTokenData,
        )
        sessionController.save(sessionData)
        _state.value = AuthState.AuthSuccess(sessionData)
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
            processingTime = processingTime,
        )
    }

    private fun isSessionExpired(): Boolean {
        return if (sessionManager.hasTokenExpired()) {
            !refreshToken()
        } else {
            false
        }
    }
}
