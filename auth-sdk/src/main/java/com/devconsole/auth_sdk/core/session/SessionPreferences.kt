package com.devconsole.auth_sdk.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val SESSION_FILE_NAME = "user_data"

class SessionPreferences(
    context: Context,
    serializer: Serializer<SessionData?>,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val dataStore: DataStore<SessionData?> = DataStoreFactory.create(
        serializer = serializer,
        scope = coroutineScope,
        produceFile = { context.dataStoreFile(SESSION_FILE_NAME) }
    )

    suspend fun updateSessionData(newValue: SessionData) {
        dataStore.updateData { newValue }
    }

    fun getSessionData(): Flow<SessionData?> = dataStore.data

    suspend fun clearSession() {
        dataStore.updateData {
            null
        }
    }
}
