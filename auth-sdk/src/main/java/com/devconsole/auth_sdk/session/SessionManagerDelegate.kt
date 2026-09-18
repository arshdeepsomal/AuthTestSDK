package com.devconsole.auth_sdk.session

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class SessionManagerDelegate(val context: Context) : Session {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val sessionPreferences = SessionPreferences(context)
    override fun sessionStream(): Flow<SessionData?> = sessionPreferences.getSessionData()

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

    override fun hasTokenExpired(): Boolean {
        val session = getSession() ?: return true
        return SessionExpirationEvaluator.isExpired(session)
    }
}
