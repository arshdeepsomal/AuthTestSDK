package com.devconsole.auth_sdk.delegate

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.devconsole.auth_sdk.data.Configuration
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
import com.devconsole.auth_sdk.network.security.JWTEncryption
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues.CODE

@RequiresApi(Build.VERSION_CODES.O)
internal class OneAuthClient(
    private val context: Context,
    private val oneConfig: Configuration.ONE.Auth,
    private val twoConfig: Configuration.TWO.Auth,
    authServiceProvider: AuthServiceProvider = DefaultAuthServiceProvider,
    private val networkDataSource: AuthNetworkDataSource = DefaultAuthNetworkDataSource(),
) {

    private val authorizationService = authServiceProvider.provide(context)

    private val authServiceConfig = AuthorizationServiceConfiguration(
        "${oneConfig.baseUrl}$PATH_AUTHORIZE".toUri(),
        "${oneConfig.baseUrl}$PATH_TOKEN".toUri()
    )

    fun buildLoginIntent(): Intent {
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

        return getAuthIntent(authRequest)
    }

    suspend fun buildRegisterIntent(): Result<Intent> {
        return fetchPrivateKeyForRegistration().map { privateKey ->
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

            getAuthIntent(authRequest)
        }
    }

    suspend fun fetchOneToken(intent: Intent): Result<ONETokenData> {
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

    private fun getAuthIntent(authRequest: AuthorizationRequest): Intent {
        return authorizationService.getAuthorizationRequestIntent(
            authRequest,
            authorizationService.createCustomTabsIntentBuilder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .build()
                )
                .build(),
        )
    }
}
