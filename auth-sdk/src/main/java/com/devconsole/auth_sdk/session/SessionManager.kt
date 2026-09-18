package com.devconsole.auth_sdk.session

import android.content.Context
import kotlinx.coroutines.flow.Flow

internal class SessionManager(val context: Context) {

    private var session: Session = DefaultSessionDelegateProvider.provide()(context)

    fun getSession(): SessionData? {
        return session.getSession()
    }

    fun hasTokenExpired(): Boolean {
        return session.hasTokenExpired()
    }

    fun sessionStream(): Flow<SessionData?> {
        return session.sessionStream()
    }

    internal fun saveSession(sessionData: SessionData) {
        session.saveSession(sessionData)
    }

    internal fun clearSession() {
        session.clearSession()
    }
}
