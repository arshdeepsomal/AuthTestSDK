package com.devconsole.auth_sdk.core.session

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.devconsole.auth_sdk.network.security.JWTEncryption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.Date

internal class SessionManagerDelegate(
    val context: Context,
    private val sessionPreferences: SessionPreferences = defaultSessionPreferences(context)
) : SessionStore {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun getSession(): SessionData? {
        var session: SessionData?
        try {
            runBlocking(Dispatchers.IO) {
                session = sessionPreferences.getSessionData().first()
            }
        } catch (e: Exception) {
            return null
        }
        return session
    }

    override fun saveSession(sessionData: SessionData) {
        coroutineScope.launch {
            runCatching {
                sessionPreferences.updateSessionData(sessionData)
            }.fold(
                onSuccess = {},
                onFailure = { error ->
                    error.printStackTrace()
                }
            )
        }
    }

    override fun clearSession() {
        coroutineScope.launch {
            runCatching {
                sessionPreferences.clearSession()
            }.fold(
                onSuccess = {},
                onFailure = { error ->
                    error.printStackTrace()
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun hasTokenExpired(): Boolean {
        val session = getSession()
        if (session != null) {
            if (session.authorizationCode.isEmpty()) return true

            val tWOTokenData = session.TWOTokenData

            val tokenBody = tWOTokenData.encodedJwt?.let { JWTEncryption().decodeJWT(it) }
            val expiredDate = (JSONObject(tokenBody).getInt("exp")).toLong() * 1000

            return Date().after(Date(expiredDate))
        }
        return true
    }
}
