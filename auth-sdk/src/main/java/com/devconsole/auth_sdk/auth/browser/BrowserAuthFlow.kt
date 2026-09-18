package com.devconsole.auth_sdk.auth.browser

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.devconsole.auth_sdk.core.DefaultDispatcherProvider
import com.devconsole.auth_sdk.core.DispatcherProvider
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.data.remote.OneAuthRemoteDataSource
import com.devconsole.auth_sdk.delegate.AuthServiceProvider
import com.devconsole.auth_sdk.delegate.DefaultAuthServiceProvider
import com.devconsole.auth_sdk.network.Constants.GRANT_TYPE
import com.devconsole.auth_sdk.network.Constants.LOGIN_SCOPES
import com.devconsole.auth_sdk.network.Constants.MAX_AGE
import com.devconsole.auth_sdk.network.Constants.PATH_AUTHORIZE
import com.devconsole.auth_sdk.network.Constants.PATH_TOKEN
import com.devconsole.auth_sdk.network.Constants.PROMPT
import com.devconsole.auth_sdk.network.Constants.QUERY
import com.devconsole.auth_sdk.network.Constants.REGISTER_SCOPES
import com.devconsole.auth_sdk.network.Constants.REGISTER_STATE
import com.devconsole.auth_sdk.network.data.Claims
import com.devconsole.auth_sdk.network.data.ClaimsRequest
import com.devconsole.auth_sdk.network.data.ClaimsUserInfo
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
import com.devconsole.auth_sdk.network.security.JWTEncryption
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues.CODE

internal class BrowserAuthFlow(
    private val context: Context,
    private val oneConfig: Configuration.ONE.Auth,
    private val remoteDataSource: OneAuthRemoteDataSource,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    authServiceProvider: AuthServiceProvider = DefaultAuthServiceProvider,
    private val jwtEncryption: JWTEncryption = JWTEncryption()
) {

    private val authorizationService = authServiceProvider.provide(context)
    private val serviceConfiguration = AuthorizationServiceConfiguration(
        "${oneConfig.baseUrl}$PATH_AUTHORIZE".toUri(),
        "${oneConfig.baseUrl}$PATH_TOKEN".toUri()
    )

    fun createLoginIntent(): Intent {
        val request = AuthorizationRequest.Builder(
            serviceConfiguration,
            oneConfig.clientId,
            CODE,
            oneConfig.redirectUri.toUri()
        ).setScope(LOGIN_SCOPES)
            .setNonce(oneConfig.nounce)
            .setState(oneConfig.nounce)
            .setResponseMode(QUERY)
            .build()

        return buildIntent(request)
    }

    suspend fun createRegisterIntent(): Result<Intent> {
        return withContext(dispatcherProvider.io) {
            runCatching {
                val tokenForPrivateKey = remoteDataSource.fetchPrivateKeyToken().getOrThrow()
                val privateKey = remoteDataSource.fetchPrivateKey(tokenForPrivateKey).getOrThrow()
                val additionalParameters = mapOf("request" to buildRegisterJwt(privateKey))

                val request = AuthorizationRequest.Builder(
                    serviceConfiguration,
                    oneConfig.clientId,
                    CODE,
                    oneConfig.redirectUri.toUri()
                ).setScope(LOGIN_SCOPES)
                    .setNonce(oneConfig.nounce)
                    .setState(oneConfig.nounce)
                    .setResponseMode(QUERY)
                    .setAdditionalParameters(additionalParameters)
                    .build()

                buildIntent(request)
            }
        }
    }

    suspend fun exchangeToken(result: ActivityResult): Result<ONETokenData> {
        return withContext(dispatcherProvider.io) {
            runCatching {
                val intent = result.data ?: error("null result")
                AuthorizationException.fromIntent(intent)?.let { throw it }
                val response = AuthorizationResponse.fromIntent(intent) ?: error("null response")

                val codeVerifier = response.createTokenExchangeRequest().codeVerifier
                    ?: error("null code verifier")
                val authorizationCode = response.authorizationCode ?: error("null authorization code")

                val tokenRequest = ONETokenRequest(
                    code = authorizationCode,
                    codeVerifier = codeVerifier,
                    grantType = GRANT_TYPE,
                    redirectUri = oneConfig.redirectUri,
                    clientId = oneConfig.clientId,
                    clientSecret = oneConfig.clientSecret,
                    scope = LOGIN_SCOPES
                )

                remoteDataSource.exchangeToken(tokenRequest).getOrThrow()
            }
        }
    }

    private fun buildIntent(request: AuthorizationRequest): Intent {
        return authorizationService.getAuthorizationRequestIntent(
            request,
            authorizationService.createCustomTabsIntentBuilder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder().build()
                )
                .build()
        )
    }

    private fun buildRegisterJwt(privateKey: String): String {
        val extraParams = mutableMapOf(
            "response_type" to CODE,
            "response_mode" to QUERY,
            "client_id" to oneConfig.clientId,
            "redirect_uri" to oneConfig.redirectUri,
            "scope" to REGISTER_SCOPES,
            "state" to REGISTER_STATE,
            "nonce" to oneConfig.nounce,
            "prompt" to PROMPT,
            "max_age" to MAX_AGE
        )

        val claims = Claims(
            request = ClaimsRequest(nonce = oneConfig.nounce),
            userInfo = ClaimsUserInfo(),
            account = "sdsfsdf"
        )

        return jwtEncryption.createJWT(
            context = context,
            url = PATH_TOKEN,
            clientId = oneConfig.clientId,
            messages = extraParams,
            claims = claims,
            keyResource = privateKey,
            salt = oneConfig.salt
        )
    }

    fun clear() {
        authorizationService.dispose()
    }
}

