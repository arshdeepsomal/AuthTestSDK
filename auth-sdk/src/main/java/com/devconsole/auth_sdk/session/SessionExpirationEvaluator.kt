package com.devconsole.auth_sdk.session

import android.util.Base64
import org.json.JSONObject

internal object SessionExpirationEvaluator {

    fun isExpired(session: SessionData): Boolean {
        if (session.authorizationCode.isEmpty()) return true

        val now = System.currentTimeMillis()

        session.TWOTokenData.sessionTokenExpiry?.let { expiry ->
            if (expiry > 0) {
                return now > expiry
            }
        }

        val encodedJwt = session.TWOTokenData.encodedJwt ?: return true
        val expiryFromJwt = decodeExpiry(encodedJwt) ?: return true
        return now > expiryFromJwt
    }

    private fun decodeExpiry(jwt: String): Long? {
        val parts = jwt.split(".")
        if (parts.size < 2) return null

        return runCatching {
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val payloadJson = JSONObject(String(decodedBytes, Charsets.UTF_8))
            payloadJson.optLong("exp", 0L).takeIf { it > 0 }?.times(1000)
        }.getOrNull()
    }
}




