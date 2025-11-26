package com.devconsole.auth_sdk.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.devconsole.auth_sdk.network.security.encryptedGsonSerializer
import kotlinx.coroutines.flow.Flow

private const val SESSION_FILE_NAME = "user_data"

val Context.sessionDataStore: DataStore<SessionData?> by dataStore(
    fileName = SESSION_FILE_NAME,
    serializer = encryptedGsonSerializer(
        defaultValue = null
    )
)

class SessionPreferences(val context: Context) {

    suspend fun updateSessionData(newValue: SessionData) {
        context.sessionDataStore.updateData { newValue }
    }

    fun getSessionData(): Flow<SessionData?> = context.sessionDataStore.data

    suspend fun clearSession() {
        context.sessionDataStore.updateData {
            null
        }
    }
}
