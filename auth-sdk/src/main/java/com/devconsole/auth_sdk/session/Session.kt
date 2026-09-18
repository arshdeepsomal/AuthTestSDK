package com.devconsole.auth_sdk.session

import kotlinx.coroutines.flow.Flow

internal interface Session {
    fun getSession(): SessionData?
    fun saveSession(sessionData: SessionData)
    fun clearSession()
    fun hasTokenExpired(): Boolean
    fun sessionStream(): Flow<SessionData?>
}
