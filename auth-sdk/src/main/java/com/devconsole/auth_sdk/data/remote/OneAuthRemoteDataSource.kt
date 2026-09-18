package com.devconsole.auth_sdk.data.remote

import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.network.api.ONEAuthService
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.data.ONEGetTokenForPKRequest
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
import retrofit2.Response

internal class OneAuthRemoteDataSource(
    private val oneConfig: Configuration.ONE.Auth,
    private val twoConfig: Configuration.TWO.Auth,
) {

    private val oneService = RetrofitManager.getInstance(oneConfig.baseUrl)
        .create(ONEAuthService::class.java)
    private val privateKeyService = RetrofitManager.getInstance(oneConfig.privateKeyBaseURL)
        .create(ONEAuthService::class.java)

    suspend fun exchangeToken(request: ONETokenRequest): Result<ONETokenData> {
        return execute { oneService.getToken(request) }
    }

    suspend fun fetchPrivateKeyToken(): Result<String> {
        val request = ONEGetTokenForPKRequest(
            clientId = twoConfig.brand,
            clientSecret = oneConfig.privateKeyAuthorization
        )
        return execute { privateKeyService.getTokenForPrivateKey(request) }.mapCatching { response ->
            response.accessToken?.takeIf { it.isNotBlank() }
                ?: error(response.error ?: "Private key token missing")
        }
    }

    suspend fun fetchPrivateKey(token: String): Result<String> {
        return execute { privateKeyService.getPrivateKey("Bearer $token", twoConfig.brand) }
            .mapCatching { response ->
                response.privateKey?.takeIf { it.isNotBlank() }
                    ?: error(response.error ?: "Private key missing")
            }
    }

    private suspend fun <T> execute(block: suspend () -> Response<T>?): Result<T> {
        return runCatching {
            block() ?: error("Empty response")
        }.mapCatching { response ->
            if (response.isSuccessful) {
                response.body() ?: error("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                error(errorBody.ifBlank { "Network call failed." })
            }
        }
    }
}

