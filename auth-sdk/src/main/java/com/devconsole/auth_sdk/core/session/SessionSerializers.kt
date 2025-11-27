package com.devconsole.auth_sdk.core.session

import androidx.datastore.core.Serializer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

internal fun plainSessionSerializer(
    gson: Gson = Gson(),
    defaultValue: SessionData? = null
): Serializer<SessionData?> = object : Serializer<SessionData?> {
    override val defaultValue: SessionData? = defaultValue

    override suspend fun readFrom(input: InputStream): SessionData? = withContext(Dispatchers.IO) {
        if (input.available() <= 0) return@withContext defaultValue
        return@withContext gson.fromJson(input.reader(), SessionData::class.java)
    }

    override suspend fun writeTo(t: SessionData?, output: OutputStream) = withContext(Dispatchers.IO) {
        if (t != null) {
            output.writer().use { writer ->
                gson.toJson(t, writer)
            }
        }
    }
}

internal fun defaultSessionPreferences(context: android.content.Context): SessionPreferences {
    return runCatching {
        SessionPreferences(
            context = context,
            serializer = com.devconsole.auth_sdk.network.security.encryptedGsonSerializer(defaultValue = null)
        )
    }.getOrElse {
        SessionPreferences(context = context, serializer = plainSessionSerializer(defaultValue = null))
    }
}
