package com.devconsole.auth_sdk.network.api

import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.ONETokenRequest
import retrofit2.Response
import retrofit2.http.Body
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
}
