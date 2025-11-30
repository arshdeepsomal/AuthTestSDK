package com.devconsole.auth_sdk.network.api

import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
import com.devconsole.auth_sdk.network.data.ONEGetTokenForPKData
import com.devconsole.auth_sdk.network.data.ONEGetTokenForPKRequest
import com.devconsole.auth_sdk.network.data.ONEPrivateKeyData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ONEAuthService {

    @POST("login/direct/one/token")
    suspend fun getToken(
        @Body body: ONETokenRequest
    ): Response<ONETokenData>?

    @POST("login/direct/one/logout")
    suspend fun logout(
        @Query("client_id") clientId: String
    ): Response<Any>?

    @POST("get_token")
    suspend fun getTokenForPrivateKey(
        @Body body: ONEGetTokenForPKRequest
    ): Response<ONEGetTokenForPKData>?

    @GET("get_brand_pk?")
    suspend fun getPrivateKey(
        @Header("Authorization") clientApiKey: String?,
        @Query("client_id") clientId: String
    ): Response<ONEPrivateKeyData>?

}
