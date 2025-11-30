package com.devconsole.auth_sdk.delegate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.devconsole.auth_sdk.AuthApi
import com.devconsole.auth_sdk.data.AuthState
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.data.ONEAuthException
import com.devconsole.auth_sdk.network.Constants.GRANT_TYPE
import com.devconsole.auth_sdk.network.Constants.LOGIN_SCOPES
import com.devconsole.auth_sdk.network.Constants.MAX_AGE
import com.devconsole.auth_sdk.network.Constants.PATH_AUTHORIZE
import com.devconsole.auth_sdk.network.Constants.PATH_TOKEN
import com.devconsole.auth_sdk.network.Constants.PROMPT
import com.devconsole.auth_sdk.network.Constants.QUERY
import com.devconsole.auth_sdk.network.Constants.REGISTER_SCOPES
import com.devconsole.auth_sdk.network.Constants.REGISTER_STATE
import com.devconsole.auth_sdk.network.api.AuthServiceProvider
import com.devconsole.auth_sdk.network.api.DefaultAuthServiceProvider
import com.devconsole.auth_sdk.network.data.Claims
import com.devconsole.auth_sdk.network.data.ClaimsRequest
import com.devconsole.auth_sdk.network.data.ClaimsUserInfo
import com.devconsole.auth_sdk.network.data.ONEGetTokenForPKRequest
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.security.JWTEncryption
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
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues.CODE

@RequiresApi(Build.VERSION_CODES.O)
internal class ONEAuthDelegate(
    val context: Context,
    private val oneConfig: Configuration.ONE.Auth,
    private val twoConfig: Configuration.TWO.Auth,
    authServiceProvider: AuthServiceProvider = DefaultAuthServiceProvider,
    sessionDelegateProvider: SessionDelegateProvider = DefaultSessionDelegateProvider,
    private val networkDataSource: AuthNetworkDataSource = DefaultAuthNetworkDataSource(),
) : AuthApi {

    private val sessionManager = SessionManager(context, sessionDelegateProvider)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    override val state: StateFlow<AuthState> get() = _state

    private val _sessionState = MutableStateFlow(!isSessionExpired())
    override val sessionState: StateFlow<Boolean> get() = _sessionState

    private val authorizationService = authServiceProvider.provide(context)

    private val authServiceConfig = AuthorizationServiceConfiguration(
        "${oneConfig.baseUrl}$PATH_AUTHORIZE".toUri(),
        "${oneConfig.baseUrl}$PATH_TOKEN".toUri()
    )

    override fun login() {
        setLoadingState()
        val authRequest = AuthorizationRequest.Builder(
            authServiceConfig,
            oneConfig.clientId,
            CODE,
            oneConfig.redirectUri.toUri()
        ).setScope(LOGIN_SCOPES)
            .setNonce(oneConfig.nounce)
            .setState(oneConfig.nounce)
            .setResponseMode(QUERY)
            .build()

        _state.value = AuthState.LaunchIntent(getAuthIntent(authRequest))
    }

    override fun register() {
        setLoadingState()
        coroutineScope.launch {
            fetchPrivateKeyForRegistration().onSuccess { privateKey ->
                callRegister(privateKey)
            }.onFailure { error ->
                emitError(error)
            }
        }
    }

    private fun callRegister(privateKey: String) {
        val extraParams = mutableMapOf(
            "response_type" to CODE,
            "response_mode" to QUERY,
            "client_id" to oneConfig.clientId,
            "redirect_uri" to oneConfig.redirectUri,
            "scope" to REGISTER_SCOPES,
            "state" to REGISTER_STATE,
            "nonce" to oneConfig.nounce,
            "prompt" to PROMPT,
            "max_age" to MAX_AGE,
        )

        val requestParams = mutableMapOf<String, String>()
        requestParams["request"] = JWTEncryption().createJWT(
            context = context,
            url = PATH_TOKEN,
            clientId = oneConfig.clientId,
            messages = extraParams,
            claims = Claims(ClaimsRequest(nonce = oneConfig.nounce), ClaimsUserInfo(), "sdsfsdf"),
            keyResource = privateKey,
            salt = oneConfig.salt
        )

        val authRequest = AuthorizationRequest.Builder(
            authServiceConfig,
            oneConfig.clientId,
            CODE,
            oneConfig.redirectUri.toUri()
        ).setScope(LOGIN_SCOPES)
            .setNonce(oneConfig.nounce)
            .setState(oneConfig.nounce)
            .setResponseMode(QUERY)
            .setAdditionalParameters(requestParams)
            .build()

        _state.value = AuthState.LaunchIntent(getAuthIntent(authRequest))
    }

    fun getAuthIntent(authRequest: AuthorizationRequest): Intent {
        return authorizationService.getAuthorizationRequestIntent(
            authRequest,
            authorizationService.createCustomTabsIntentBuilder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .build()
                )
                .build()
        )
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
                    fetchOneToken(resultData)
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

    private suspend fun fetchOneToken(intent: Intent): Result<ONETokenData> {
        AuthorizationException.fromIntent(intent)?.let {
            return Result.failure(it)
        }
        val response = AuthorizationResponse.fromIntent(intent)
            ?: return Result.failure(Exception("null response"))
        val codeVerifier = response.createTokenExchangeRequest().codeVerifier
            ?: return Result.failure(Exception("null code verifier"))
        val authorizationCode = response.authorizationCode
            ?: return Result.failure(Exception("null authorization code"))

        val tokenRequest = ONETokenRequest(
            code = authorizationCode,
            codeVerifier = codeVerifier,
            grantType = GRANT_TYPE,
            redirectUri = oneConfig.redirectUri,
            clientId = oneConfig.clientId,
            clientSecret = oneConfig.clientSecret,
            scope = LOGIN_SCOPES
        )

        return networkDataSource.requestOneToken(tokenRequest, oneConfig.baseUrl)
    }

    private fun inittwoLogin(oneData: ONETokenData) {
        val twoLoginRequest = TWOLoginRequest(
            accessToken = oneData.accessToken.toString(),
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            respondWithUsername = true
        )
        coroutineScope.launch {
            networkDataSource.loginTwo(twoLoginRequest, twoConfig.authorization, twoConfig.baseUrl)
                .onSuccess { twoTokenData ->
                    handleTwoLoginSuccess(oneData, twoTokenData)
                }
                .onFailure { emitError(it) }
        }
    }

    private fun handleTwoLoginSuccess(oneData: ONETokenData, twoTokenData: TWOTokenData) {
        if (twoTokenData.success == true) {
            persistSession(createSession(twoTokenData, oneData))
        } else {
            emitError(Exception("Something went wrong."))
            setSessionState(false)
        }
    }

    override fun logout() {
        setLoadingState()
        val currentSession = sessionManager.getSession()
        val twoLogoutRequest = TWOLogoutRequest(
            idToken = currentSession?.ONETokenData?.idToken,
            flatToken = currentSession?.TWOTokenData?.encodedJwt
        )
        coroutineScope.launch {
            networkDataSource.logoutTwo(twoLogoutRequest, twoConfig.authorization, twoConfig.baseUrl)
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
        val twoRenewTokenRequest = TWORenewTokenRequest(
            currentFlatToken = currentSession.TWOTokenData.encodedJwt,
            deviceId = twoConfig.deviceId
        )

        val renewResult = try {
            runBlocking(Dispatchers.IO) {
                networkDataSource.renewTwoToken(twoRenewTokenRequest, twoConfig.authorization, twoConfig.baseUrl)
            }
        } catch (e: Exception) {
            emitError(e)
            setSessionState(false)
            return false
        }

        return renewResult.fold(
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
        val submitGoogleReceiptDataLinkAccount = SubmitGoogleReceiptDataLinkAccount(
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

        launchReceiptRequest {
            networkDataSource.submitGoogleReceiptAndLinkAccount(
                submitGoogleReceiptDataLinkAccount,
                twoConfig.authorization,
                twoConfig.baseUrl
            )
        }
    }

    override fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?,
    ) {
        val submitGoogleData = SubmitGoogleData(
            currentPurchaseToken = currentPurchaseToken,
            previousPurchaseToken = previousPurchaseToken,
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            packageName = packageName,
            productId = sku
        )

        launchReceiptRequest {
            networkDataSource.submitGoogleReceipt(
                submitGoogleData,
                twoConfig.authorization,
                twoConfig.baseUrl
            )
        }
    }

    override fun loginWithGoogleReceipt(purchaseToken: String) {
        val twoGoogleReceiptLoginRequest = TWOGoogleReceiptLoginRequest(
            purchaseToken = purchaseToken,
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            respondWithUsername = true
        )

        launchReceiptRequest {
            networkDataSource.loginWithGoogleReceipt(
                twoGoogleReceiptLoginRequest,
                twoConfig.authorization,
                twoConfig.baseUrl
            )
        }
    }

    private fun handleReceiptResult(receiptResult: Result<SubmitReceiptData>) {
        receiptResult.onSuccess { twoTokenData ->
            if (twoTokenData.success == true) {
                persistSession(createSessionFromReceipt(twoTokenData))
            } else {
                emitError(Exception(twoTokenData.message))
            }
        }.onFailure {
            emitError(it)
        }
    }

    private fun launchReceiptRequest(block: suspend () -> Result<SubmitReceiptData>) {
        setLoadingState()
        coroutineScope.launch {
            handleReceiptResult(block())
        }
    }

    private fun setLoadingState() {
        _state.value = AuthState.Loading
    }

    private fun persistSession(sessionData: SessionData) {
        sessionManager.saveSession(sessionData)
        _state.value = AuthState.AuthSuccess(sessionData)
        setSessionState(true)
    }

    private fun createSession(twoTokenData: TWOTokenData, oneData: ONETokenData = ONETokenData()): SessionData {
        return SessionData(
            authorizationCode = twoConfig.authorization,
            ONETokenData = oneData,
            TWOTokenData = twoTokenData
        )
    }

    private fun createSessionFromReceipt(receiptData: SubmitReceiptData): SessionData {
        return SessionData(
            authorizationCode = twoConfig.authorization,
            ONETokenData = ONETokenData(),
            TWOTokenData = TWOTokenData(
                success = receiptData.success,
                status = receiptData.status,
                sessionToken = receiptData.sessionToken,
                sessionTokenExpiry = receiptData.sessionTokenExpiry,
                supportToken = receiptData.supportToken,
                encodedJwt = receiptData.encodedJwt,
                username = receiptData.username,
                processingTime = receiptData.processingTime
            )
        )
    }

    private fun emitError(error: Throwable) {
        _state.value = AuthState.Error(error)
    }

    private fun isSessionExpired(): Boolean {
        if (sessionManager.hasTokenExpired()) {
            return !refreshToken()
        }
        return false
    }

    private suspend fun fetchPrivateKeyForRegistration(): Result<String> {
        val tokenResult = networkDataSource.fetchPrivateKeyToken(
            ONEGetTokenForPKRequest(
                clientId = twoConfig.brand,
                clientSecret = oneConfig.privateKeyAuthorization
            ),
            oneConfig.privateKeyBaseURL
        )

        return tokenResult.fold(
            onSuccess = { tokenData ->
                if (tokenData.success == true && !tokenData.accessToken.isNullOrBlank()) {
                    networkDataSource.fetchPrivateKey(
                        authorization = "Bearer ${tokenData.accessToken}",
                        clientId = twoConfig.brand,
                        baseUrl = oneConfig.privateKeyBaseURL
                    ).mapCatching { data ->
                        data.privateKey ?: throw IllegalStateException(data.error ?: "Unable to fetch private key")
                    }
                } else {
                    Result.failure(IllegalStateException(tokenData.error ?: "Unable to fetch token for private key"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}
