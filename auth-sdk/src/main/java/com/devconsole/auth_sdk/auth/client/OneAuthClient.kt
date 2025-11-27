package com.devconsole.auth_sdk.auth.client

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.devconsole.auth_sdk.auth.delegate.AuthServiceProvider
import com.devconsole.auth_sdk.auth.delegate.DefaultAuthServiceProvider
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.network.Constants.GRANT_TYPE
import com.devconsole.auth_sdk.network.Constants.LOGIN_SCOPES
import com.devconsole.auth_sdk.network.Constants.PATH_AUTHORIZE
import com.devconsole.auth_sdk.network.Constants.PATH_TOKEN
import com.devconsole.auth_sdk.network.Constants.PROMPT
import com.devconsole.auth_sdk.network.Constants.QUERY
import com.devconsole.auth_sdk.network.Constants.REGISTER_SCOPES
import com.devconsole.auth_sdk.network.Constants.REGISTER_STATE
import com.devconsole.auth_sdk.network.api.ONEAuthService
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.data.Claims
import com.devconsole.auth_sdk.network.data.ClaimsRequest
import com.devconsole.auth_sdk.network.data.ClaimsUserInfo
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
import com.devconsole.auth_sdk.network.security.JWTEncryption
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues.CODE

internal class OneAuthClient(
    private val context: Context,
    private val config: Configuration.ONE.Auth,
    authServiceProvider: AuthServiceProvider = DefaultAuthServiceProvider,
) {

    private val authorizationService = authServiceProvider.provide(context)

    private val authServiceConfig = AuthorizationServiceConfiguration(
        "${config.baseUrl}$PATH_AUTHORIZE".toUri(),
        "${config.baseUrl}$PATH_TOKEN".toUri()
    )

    fun buildLoginIntent(): Intent = buildIntent(createLoginRequest())

    fun buildRegisterIntent(privateKey: String): Intent {
        val requestParams = mutableMapOf<String, String>()
        requestParams["request"] = JWTEncryption().createJWT(
            context = context,
            url = PATH_TOKEN,
            clientId = config.clientId,
            messages = registerExtraParams(),
            claims = Claims(ClaimsRequest(nonce = config.nounce), ClaimsUserInfo(), "sdsfsdf"),
            keyResource = privateKey,
            salt = config.salt
        )

        val request = AuthorizationRequest.Builder(
            authServiceConfig,
            config.clientId,
            CODE,
            config.redirectUri.toUri()
        ).setScope(LOGIN_SCOPES)
            .setNonce(config.nounce)
            .setState(config.nounce)
            .setResponseMode(QUERY)
            .setAdditionalParameters(requestParams)
            .build()

        return buildIntent(request)
    }

    suspend fun exchangeToken(intent: Intent): Result<ONETokenData> {
        AuthorizationException.fromIntent(intent)?.let { return Result.failure(it) }
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
            redirectUri = config.redirectUri,
            clientId = config.clientId,
            clientSecret = config.clientSecret,
            scope = LOGIN_SCOPES
        )

        val tokenResponse = RetrofitManager.getInstance(config.baseUrl)
            .create(ONEAuthService::class.java)
            .getToken(tokenRequest)

        val tokenData = tokenResponse?.body()?.takeIf { tokenResponse.isSuccessful }
            ?: return Result.failure(Exception(tokenResponse?.errorBody().toString()))

        return Result.success(tokenData)
    }

    private fun buildIntent(authRequest: AuthorizationRequest): Intent {
        return authorizationService.getAuthorizationRequestIntent(
            authRequest,
            authorizationService.createCustomTabsIntentBuilder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().build())
                .build()
        )
    }

    private fun createLoginRequest(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            authServiceConfig,
            config.clientId,
            CODE,
            config.redirectUri.toUri()
        ).setScope(LOGIN_SCOPES)
            .setNonce(config.nounce)
            .setState(config.nounce)
            .setResponseMode(QUERY)
            .build()
    }

    private fun registerExtraParams(): Map<String, String> {
        return mapOf(
            "response_type" to CODE,
            "response_mode" to QUERY,
            "client_id" to config.clientId,
            "redirect_uri" to config.redirectUri,
            "scope" to REGISTER_SCOPES,
            "state" to REGISTER_STATE,
            "nonce" to config.nounce,
            "prompt" to PROMPT,
            "max_age" to com.devconsole.auth_sdk.network.Constants.MAX_AGE.toString(),
        )
    }
}
