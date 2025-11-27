package com.devconsole.auth_sdk.network.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitManagerTest {

    @Test
    fun `getInstance configures logging and gson converter`() {
        val retrofit = RetrofitManager.getInstance("https://example.com/")
        val client = retrofit.callFactory() as OkHttpClient

        assertEquals("https://example.com/", retrofit.baseUrl().toString())
        assertTrue(client.interceptors.any { it is HttpLoggingInterceptor })
        assertTrue(retrofit.converterFactories().any { it is GsonConverterFactory })
    }
}
