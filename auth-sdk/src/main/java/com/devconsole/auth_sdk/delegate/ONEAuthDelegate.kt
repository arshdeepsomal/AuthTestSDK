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
import com.devconsole.auth_sdk.network.security.JWTEncryption
import com.devconsole.auth_sdk.network.api.TWOAuthService
import com.devconsole.auth_sdk.network.api.ONEAuthService
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.data.Claims
import com.devconsole.auth_sdk.network.data.ClaimsRequest
import com.devconsole.auth_sdk.network.data.ClaimsUserInfo
import com.devconsole.auth_sdk.network.data.ONEGetTokenForPKRequest
import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWORenewTokenData
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.session.SessionData
import com.devconsole.auth_sdk.session.SessionManager
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
) : AuthApi {

    private val sessionManager = SessionManager(context)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    override val state: StateFlow<AuthState> get() = _state

    private val _sessionState = MutableStateFlow(!isSessionExpired())
    override val sessionState: StateFlow<Boolean> get() = _sessionState

    private val authorizationService = DefaultAuthServiceProvider.provide(context)

    private val authServiceConfig = AuthorizationServiceConfiguration(
        "${oneConfig.baseUrl}$PATH_AUTHORIZE".toUri(),
        "${oneConfig.baseUrl}$PATH_TOKEN".toUri()
    )

    /**
     * This method call setup the [AuthorizationRequest] for SignIn/ login and
     * then updates the [_state] to [AuthState.LaunchIntent].
     */
    override fun login() {
        _state.value = AuthState.Loading
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

        val authIntent = getAuthIntent(authRequest)

        _state.value = AuthState.LaunchIntent(authIntent)
    }

    /**
     * This method call setup the [AuthorizationRequest] for SignUp/ register and
     * then updates the [_state] to [AuthState.LaunchIntent].
     */
    override fun register() {
        _state.value = AuthState.Loading

        // get token for private key
        val accessToken = getTokenForPrivateKey()

        // get private key for registration
        val privateKey = getTokenForPrivateKey(accessToken)

        callRegister(privateKey)
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

        val authIntent = getAuthIntent(authRequest)
        _state.value = AuthState.LaunchIntent(authIntent)

    }

    /**
     * This method builds the [Intent] to open a [CustomTabsIntent].
     * This is called for both SignIn and SignUp, and based on [AuthorizationRequest] it opens the
     * requested page in custom tab.
     *
     * The toolbar color can be override by adding  custom color value to your colors.xml file.
     * For eg: <color name="custom_tab_tool_bar_color" override="true">#FF6200EE</color>
     */
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

    /**
     * This method handles the [ActivityResult] and if the [resultData] is not null, it will try
     * to call the one login api.
     *
     * If the Api call is success then the two login api will be called.
     * If the parsing of [resultData] fails, it gives three types of [AuthorizationException].
     *    1. With errorDescription = "register" -> This will call the register flow.
     *    2. With errorDescription = "signIn" -> This will call the login flow.
     *    3. With errorDescription = "cancel_register" -> This will cancel the flow and get back to
     *    the screen.
     */
    override fun handleIntentResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val resultData = result.data

                if (resultData == null) {
                    _state.value = AuthState.Error(Exception("null result"))
                    return
                }

                AuthorizationException.fromIntent(resultData)?.let {
                    when (it.errorDescription) {
                        "register" -> register()
                        "signIn" -> login()
                        "username" -> login()
                        "cancel_register" -> _state.value = AuthState.Error(it)
                        else -> {
                            _state.value = AuthState.Error(
                                ONEAuthException(
                                    errorCode = it.code,
                                    errorDescription = it.errorDescription
                                )
                            )
                        }
                    }
                    return
                }

                _state.value = AuthState.Loading

                coroutineScope.launch {
                    runCatching {
                        getoneToken(resultData).getOrThrow()
                    }.onSuccess { data ->
                        Log.i("success ", (data as ONETokenData).accessToken.toString())
                        inittwoLogin(data)
                    }.onFailure { exception ->
                        _state.value = AuthState.Error(exception)
                    }
                }
            }
        }
    }

    /**
     * This method call the api for getting access token from the one.
     * Firstly, it parses for any kind of [AuthorizationException], if there is an it returns
     * the [Result] as failure.
     * Secondly, it calls the one api for the token. If this call gets succesfully completed then
     * returns the [Result] as success along with the [ONETokenData] object.
     */
    private suspend fun getoneToken(intent: Intent): Result<Any> {
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

        val tokenResponse = RetrofitManager.getInstance(oneConfig.baseUrl)
            .create(ONEAuthService::class.java).getToken(tokenRequest)
        val tokenData = tokenResponse?.body()?.takeIf { tokenResponse.isSuccessful }
            ?: run {
                val errorBody = tokenResponse?.errorBody().toString()
                return Result.failure(Exception(errorBody))
            }
        return Result.success(tokenData)
    }

    /**
     * The method call the api of two to fetch the flat_token.
     * This call uses the assess token generated in the one api call and performs the api call.
     * On successful call, the session is created and saved locally into [SessionData] object.
     * [SessionData] is the combination of the [twoConfig.authorization], [ONETokenData] and
     * [TWOTokenData].
     */
    private fun inittwoLogin(oneData: ONETokenData) {
        val TWOLoginRequest = TWOLoginRequest(
            accessToken = oneData.accessToken.toString(),
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            respondWithUsername = true
        )
        coroutineScope.launch {
            runCatching {
                RetrofitManager.getInstance(twoConfig.baseUrl)
                    .create(TWOAuthService::class.java)
                    .login(twoConfig.authorization, TWOLoginRequest)
            }.onSuccess { twoResponse ->
                val twoTokenData = twoResponse?.body()?.takeIf { twoResponse.isSuccessful }
                    ?: run {
                        val errorBody = twoResponse?.errorBody().toString()
                        _state.value = AuthState.Error(Exception(errorBody))
                        return@launch
                    }

                if (twoTokenData.success == true) {
                    val sessionData = SessionData(
                        authorizationCode = twoConfig.authorization,
                        ONETokenData = oneData,
                        TWOTokenData = twoTokenData
                    )
                    sessionManager.saveSession(sessionData)
                    _state.value = AuthState.AuthSuccess(sessionData)
                    setSessionState(true)
                } else {
                    _state.value = AuthState.Error(Exception("Something went wrong."))
                    setSessionState(false)
                }
            }.onFailure {
                _state.value = AuthState.Error(it)
            }
        }
    }

    /**
     * This method call the logout api of two. This will remove the current session from the
     * custom tab as well.
     * And also will clear the session from the local storage as well.
     */
    override fun logout() {
        _state.value = AuthState.Loading
        val currentSession = sessionManager.getSession()
        val TWOLogoutRequest = TWOLogoutRequest(
            idToken = currentSession?.ONETokenData?.idToken,
            flatToken = currentSession?.TWOTokenData?.encodedJwt
        )
        coroutineScope.launch {
            runCatching {
                RetrofitManager.getInstance(twoConfig.baseUrl)
                    .create(TWOAuthService::class.java)
                    .logout(currentSession?.authorizationCode, TWOLogoutRequest)
            }.onSuccess {
                handleLogout()
            }.onFailure {
                handleLogout()
            }
        }
    }

    /**
     * This method is used to clear the session from the local storage and also set the session
     * state to false.
     */
    private fun handleLogout() {
        sessionManager.clearSession()
        _state.value = AuthState.LogoutSuccess
        setSessionState(false)
    }

    /**
     * This method call the api to refresh the two token. If the result is success then the token
     * data is updated in the local storage as well and if it fail will return back without saving
     * the token data.
     */
    override fun refreshToken(): Boolean {
        val currentSession = sessionManager.getSession() ?: return false

        _state.value = AuthState.Loading
        val TWORenewTokenRequest = TWORenewTokenRequest(
            currentFlatToken = currentSession.TWOTokenData.encodedJwt,
            deviceId = twoConfig.deviceId
        )
        var newTokenData: TWORenewTokenData?
        try {
            runBlocking(Dispatchers.IO) {
                newTokenData = RetrofitManager.getInstance(twoConfig.baseUrl)
                    .create(TWOAuthService::class.java)
                    .renewToken(twoConfig.authorization, TWORenewTokenRequest)?.body()
            }
        } catch (e: Exception) {
            _state.value = AuthState.Error(e)
            setSessionState(false)
            return false
        }

        newTokenData?.let {
            currentSession.TWOTokenData.copy(
                encodedJwt = it.encodedJwt,
                sessionToken = it.sessionToken,
                sessionTokenExpiry = it.sessionTokenExpiry
            )
            sessionManager.saveSession(currentSession)
            _state.value = AuthState.AuthSuccess(currentSession)
            setSessionState(true)
            return true
        }
        _state.value = AuthState.Error(Exception("Token Renew Failed"))
        setSessionState(false)
        return false
    }

    /**
     * This method is used to set the session state.
     */
    private fun setSessionState(state: Boolean) {
        try {
            _sessionState.value = state
        } catch (e: Exception) {
            Log.i("Delegate", "Session state not initialized yet")
        }
    }

    /**
     * Used for initial purchases, submit receipt and link account all in one API call
     */
    override fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String?,
        password: String?,
        packageName: String?,
        accountToken: String?
    ) {
        _state.value = AuthState.Loading

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

        coroutineScope.launch {
            runCatching {
                RetrofitManager.getInstance(twoConfig.baseUrl)
                    .create(TWOAuthService::class.java)
                    .submitGoogleReceiptAndLinkAccount(
                        twoConfig.authorization, submitGoogleReceiptDataLinkAccount
                    )
            }.onSuccess { twoReceiptLoginResponse ->
                val twoTokenData = twoReceiptLoginResponse?.body()?.takeIf {
                    twoReceiptLoginResponse.isSuccessful
                } ?: run {
                    val errorBody = twoReceiptLoginResponse?.errorBody().toString()
                    _state.value = AuthState.Error(Exception(errorBody))
                    return@launch
                }

                if (twoTokenData.success == true) {
                    val sessionData = SessionData(
                        authorizationCode = twoConfig.authorization,
                        ONETokenData = ONETokenData(),
                        TWOTokenData = TWOTokenData(
                            success = twoTokenData.success,
                            status = twoTokenData.status,
                            sessionToken = twoTokenData.sessionToken,
                            sessionTokenExpiry = twoTokenData.sessionTokenExpiry,
                            supportToken = twoTokenData.supportToken,
                            encodedJwt = twoTokenData.encodedJwt,
                            username = twoTokenData.username,
                            processingTime = twoTokenData.processingTime
                        )
                    )
                    sessionManager.saveSession(sessionData)
                    _state.value = AuthState.AuthSuccess(sessionData)
                } else {
                    _state.value = AuthState.Error(Exception(twoTokenData.message))
                }
            }.onFailure {
                _state.value = AuthState.Error(it)
            }
        }
    }

    /**
     * Submit Amazon IAP purchase receipt for two-Auth registration
     */
    override fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String?
    ) {
        _state.value = AuthState.Loading

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

        coroutineScope.launch {
            runCatching {
                RetrofitManager.getInstance(twoConfig.baseUrl)
                    .create(TWOAuthService::class.java)
                    .submitGoogleReceipt(twoConfig.authorization, submitGoogleData)
            }.onSuccess { twoReceiptLoginResponse ->
                val twoTokenData = twoReceiptLoginResponse?.body()?.takeIf {
                    twoReceiptLoginResponse.isSuccessful
                } ?: run {
                    val errorBody = twoReceiptLoginResponse?.errorBody().toString()
                    _state.value = AuthState.Error(Exception(errorBody))
                    return@launch
                }

                if (twoTokenData.success == true) {
                    val sessionData = SessionData(
                        authorizationCode = twoConfig.authorization,
                        ONETokenData = ONETokenData(),
                        TWOTokenData = TWOTokenData(
                            success = twoTokenData.success,
                            status = twoTokenData.status,
                            sessionToken = twoTokenData.sessionToken,
                            sessionTokenExpiry = twoTokenData.sessionTokenExpiry,
                            supportToken = twoTokenData.supportToken,
                            encodedJwt = twoTokenData.encodedJwt,
                            username = twoTokenData.username,
                            processingTime = twoTokenData.processingTime
                        )
                    )
                    sessionManager.saveSession(sessionData)
                    _state.value = AuthState.AuthSuccess(sessionData)
                } else {
                    _state.value = AuthState.Error(Exception(twoTokenData.message))
                }
            }.onFailure {
                _state.value = AuthState.Error(it)
            }
        }
    }

    /**
     * Googleplay users who purchase an IAP will need to occasionally log into two-Auth.
     * This will happen through the google receipt login.
     */
    override fun loginWithGoogleReceipt(purchaseToken: String) {
        _state.value = AuthState.Loading

        val TWOGoogleReceiptLoginRequest = TWOGoogleReceiptLoginRequest(
            purchaseToken = purchaseToken,
            brand = twoConfig.brand,
            source = twoConfig.source,
            respondWithJwt = true,
            deviceId = twoConfig.deviceId,
            respondWithUsername = true
        )

        coroutineScope.launch {
            runCatching {
                RetrofitManager.getInstance(twoConfig.baseUrl)
                    .create(TWOAuthService::class.java)
                    .loginWithGoogleReceipt(twoConfig.authorization, TWOGoogleReceiptLoginRequest)
            }.onSuccess { twoReceiptLoginResponse ->
                val twoTokenData = twoReceiptLoginResponse?.body()?.takeIf {
                    twoReceiptLoginResponse.isSuccessful
                } ?: run {
                    val errorBody = twoReceiptLoginResponse?.errorBody().toString()
                    _state.value = AuthState.Error(Exception(errorBody))
                    return@launch
                }

                if (twoTokenData.success == true) {
                    val sessionData = SessionData(
                        authorizationCode = twoConfig.authorization,
                        ONETokenData = ONETokenData(),
                        TWOTokenData = TWOTokenData(
                            success = twoTokenData.success,
                            status = twoTokenData.status,
                            sessionToken = twoTokenData.sessionToken,
                            sessionTokenExpiry = twoTokenData.sessionTokenExpiry,
                            supportToken = twoTokenData.supportToken,
                            encodedJwt = twoTokenData.encodedJwt,
                            username = twoTokenData.username,
                            processingTime = twoTokenData.processingTime
                        )
                    )
                    sessionManager.saveSession(sessionData)
                    _state.value = AuthState.AuthSuccess(sessionData)
                } else {
                    _state.value = AuthState.Error(Exception(twoTokenData.message))
                }
            }.onFailure {
                _state.value = AuthState.Error(it)
            }
        }
    }

    private fun isSessionExpired(): Boolean {
        if (sessionManager.hasTokenExpired()) {
            return !refreshToken()
        }
        return false
    }

    private fun getTokenForPrivateKey() : String {

        var newTokenForPKData = ""
        try {
            runBlocking(Dispatchers.IO) {
                val newTokenData = RetrofitManager.getInstance(oneConfig.privateKeyBaseURL)
                    .create(ONEAuthService::class.java).getTokenForPrivateKey(
                        ONEGetTokenForPKRequest(clientId = twoConfig.brand,
                            clientSecret = oneConfig.privateKeyAuthorization))

                try {
                    if (newTokenData?.body()?.success == true) {
                        println(newTokenData.body()?.accessToken)
                        newTokenForPKData = newTokenData.body()?.accessToken.toString()
                    } else
                        println(newTokenData?.body()?.error)
                } catch (e: Exception) {
                    println(newTokenData?.body()?.error)
                }
            }
        } catch (e: Exception) {
            _state.value = AuthState.Error(e)
        }

        return newTokenForPKData
    }

    private fun getTokenForPrivateKey(token: String) : String {

        var privateKey = ""
        try {
            runBlocking(Dispatchers.IO) {
                val newPKData = RetrofitManager.getInstance(oneConfig.privateKeyBaseURL)
                    .create(ONEAuthService::class.java).getPrivateKey("Bearer $token",
                        clientId = twoConfig.brand)

                try {
                    if (newPKData?.body()?.success == true) {
                        println(newPKData.body()?.privateKey)
                        privateKey = newPKData.body()?.privateKey.toString()
                    } else
                        println(newPKData?.body()?.error)
                } catch (e: Exception) {
                    println(newPKData?.body()?.error)
                }
            }
        } catch (e: Exception) {
            _state.value = AuthState.Error(e)
        }

        return privateKey
    }
}
