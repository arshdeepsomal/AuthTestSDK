package com.devconsole.auth_sdk.network.security

import androidx.datastore.core.Serializer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

inline fun <reified T> encryptedGsonSerializer(
    defaultValue: T,                     // can be null
    cryptoManager: CryptoManager = CryptoManager(),
    gson: Gson = Gson(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Serializer<T> = object : Serializer<T> {

    override val defaultValue: T = defaultValue

    override suspend fun readFrom(input: InputStream): T {
        val encryptedBytes = withContext(ioDispatcher) { input.use { it.readBytes() } }
        if (encryptedBytes.isEmpty()) return defaultValue

        val decrypted = cryptoManager.decrypt(encryptedBytes)
        val jsonString = decrypted.decodeToString().trim()

        if (jsonString.isEmpty() || jsonString == "null") {
            @Suppress("UNCHECKED_CAST")
            return null as T        // defaultValue will typically be null
        }

        return gson.fromJson(jsonString, object : TypeToken<T>() {}.type)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        val jsonString = if (t == null) "null" else gson.toJson(t)
        val plainBytes = jsonString.toByteArray()
        val encryptedBytes = cryptoManager.encrypt(plainBytes)
        withContext(ioDispatcher) { output.use { it.write(encryptedBytes) } }
    }
}