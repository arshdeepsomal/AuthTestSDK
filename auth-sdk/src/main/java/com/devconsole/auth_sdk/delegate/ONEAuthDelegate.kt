package com.devconsole.auth_sdk.delegate

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import com.devconsole.auth_sdk.AuthApi
import com.devconsole.auth_sdk.data.AuthState
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.data.ONEAuthException
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.session.DefaultSessionDelegateProvider
import com.devconsole.auth_sdk.session.SessionData
import com.devconsole.auth_sdk.session.SessionManager
import com.devconsole.auth_sdk.session.SessionDelegateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationException

@RequiresApi(Build.VERSION_CODES.O)
internal class ONEAuthDelegate(
    val context: Context,
    private val oneConfig: Configuration.ONE.Auth,
    private val twoConfig: Configuration.TWO.Auth,
    authServiceProvider: com.devconsole.auth_sdk.network.api.AuthServiceProvider =
        com.devconsole.auth_sdk.network.api.DefaultAuthServiceProvider,
    sessionDelegateProvider: SessionDelegateProvider = DefaultSessionDelegateProvider,
    private val networkDataSource: AuthNetworkDataSource = DefaultAuthNetworkDataSource(),
) : AuthApi {

    private val sessionManager = SessionManager(context, sessionDelegateProvider)
    private val oneAuthClient = OneAuthClient(context, oneConfig, twoConfig, authServiceProvider, networkDataSource)
    private val twoAuthClient = TwoAuthClient(twoConfig, sessionManager, networkDataSource)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    override val state: StateFlow<AuthState> get() = _state

    private val _sessionState = MutableStateFlow(!isSessionExpired())
    override val sessionState: StateFlow<Boolean> get() = _sessionState

    override fun login() {
        setLoadingState()
        _state.value = AuthState.LaunchIntent(oneAuthClient.buildLoginIntent())
    }

    override fun register() {
        setLoadingState()
        coroutineScope.launch {
            oneAuthClient.buildRegisterIntent()
                .onSuccess { _state.value = AuthState.LaunchIntent(it) }
                .onFailure { error -> emitError(error) }
        }
    }

    override fun handleIntentResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val resultData = result.data

                if (resultData == null) {
                    emitError(Exception("null result"))
                    return
                }

                AuthorizationException.fromIntent(resultData)?.let {
                    when (it.errorDescription) {
                        "register" -> register()
                        "signIn" -> login()
                        "username" -> login()
                        "cancel_register" -> emitError(it)
                        else -> emitError(
                            ONEAuthException(
                                errorCode = it.code,
                                errorDescription = it.errorDescription
                            )
                        )
                    }
                    return
                }

                setLoadingState()

                coroutineScope.launch {
                    oneAuthClient.fetchOneToken(resultData)
                        .onSuccess { data ->
                            Log.i("success ", data.accessToken.toString())
                            inittwoLogin(data)
                        }
                        .onFailure { exception ->
                            emitError(exception)
                        }
                }
            }
        }
    }

    private fun inittwoLogin(oneData: ONETokenData) {
        coroutineScope.launch {
            twoAuthClient.loginWithOneToken(oneData)
                .onSuccess { session -> persistSession(session) }
                .onFailure { emitError(it) }
        }
    }

    override fun logout() {
        setLoadingState()
        coroutineScope.launch {
            twoAuthClient.logout()
                .onSuccess { handleLogout() }
                .onFailure { handleLogout() }
        }
    }

    private fun handleLogout() {
        sessionManager.clearSession()
        _state.value = AuthState.LogoutSuccess
        setSessionState(false)
    }

    override fun refreshToken(): Boolean {
        val currentSession = sessionManager.getSession() ?: return false

        setLoadingState()
        val renewResult = try {
            runBlocking(Dispatchers.IO) {
                twoAuthClient.renewSession()
            }
        } catch (e: Exception) {
            emitError(e)
            setSessionState(false)
            return false
        }

        return renewResult.fold(
            onSuccess = { renewedSession ->
                _state.value = AuthState.AuthSuccess(renewedSession)
                setSessionState(true)
                true
            },
            onFailure = {
                emitError(it)
                setSessionState(false)
                false
            }
        )
    }

    private fun setSessionState(state: Boolean) {
        try {
            _sessionState.value = state
        } catch (e: Exception) {
            Log.i("Delegate", "Session state not initialized yet")
        }
    }

    override fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?,
    ) {
        launchReceiptRequest {
            twoAuthClient.submitGoogleReceiptAndLinkAccount(
                purchaseToken,
                sku,
                username,
                password,
                packageName,
                accountToken,
            )
        }
    }

    override fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?,
    ) {
        launchReceiptRequest {
            twoAuthClient.submitGoogleReceipt(
                currentPurchaseToken,
                previousPurchaseToken,
                sku,
                packageName,
            )
        }
    }

    override fun loginWithGoogleReceipt(purchaseToken: String) {
        launchReceiptRequest { twoAuthClient.loginWithGoogleReceipt(purchaseToken) }
    }

    private fun handleReceiptResult(receiptResult: Result<SessionData>) {
        receiptResult.onSuccess { sessionData ->
            persistSession(sessionData)
        }.onFailure {
            emitError(it)
        }
    }

    private fun launchReceiptRequest(block: suspend () -> Result<SessionData>) {
        setLoadingState()
        coroutineScope.launch {
            handleReceiptResult(block())
        }
    }

    private fun setLoadingState() {
        _state.value = AuthState.Loading
    }

    private fun persistSession(sessionData: SessionData) {
        _state.value = AuthState.AuthSuccess(sessionData)
        setSessionState(true)
    }

    private fun emitError(error: Throwable) {
        _state.value = AuthState.Error(error)
    }

    private fun isSessionExpired(): Boolean {
        if (twoAuthClient.hasTokenExpired()) {
            return !refreshToken()
        }
        return false
    }
}
