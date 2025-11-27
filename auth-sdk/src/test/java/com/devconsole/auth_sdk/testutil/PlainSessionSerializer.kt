package com.devconsole.auth_sdk.testutil

import androidx.datastore.core.Serializer
import com.devconsole.auth_sdk.core.session.SessionData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

fun plainSessionSerializer(gson: Gson = Gson()): Serializer<SessionData?> = object : Serializer<SessionData?> {

    override val defaultValue: SessionData? = null

    override suspend fun readFrom(input: InputStream): SessionData? {
        val bytes = withContext(Dispatchers.IO) { input.use { it.readBytes() } }
        if (bytes.isEmpty()) return null
        val json = bytes.decodeToString()
        if (json.isBlank() || json == "null") return null
        return gson.fromJson(json, object : TypeToken<SessionData?>() {}.type)
    }

    override suspend fun writeTo(t: SessionData?, output: OutputStream) {
        val json = if (t == null) "null" else gson.toJson(t)
        withContext(Dispatchers.IO) { output.use { it.write(json.toByteArray()) } }
    }
}

