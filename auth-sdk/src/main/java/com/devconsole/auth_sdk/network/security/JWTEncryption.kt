package com.devconsole.auth_sdk.network.security

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.devconsole.auth_sdk.network.data.Claims
import com.devconsole.auth_sdk.utils.ExcludeFromCoverage
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

@ExcludeFromCoverage("Not stable for testing")
internal class JWTEncryption {

    private fun getStringPrivateKey(salt: String, keyResource: String): String? {
        return try {
            var rsaPrivateKey = keyResource
            rsaPrivateKey = rsaPrivateKey
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            rsaPrivateKey = rsaPrivateKey
                .replace("-----END RSA PRIVATE KEY-----", "")
            rsaPrivateKey = rsaPrivateKey
                .replace("\n", "")
            rsaPrivateKey = rsaPrivateKey.replace(salt, "")
            rsaPrivateKey
        } catch (ex: Exception) {
            null
        }
    }

    private fun decodeBase64ToString(encodedString: String): String {
        val decodedBytes = Base64.getDecoder().decode(encodedString)
        return String(decodedBytes, Charsets.UTF_8) // Specify UTF-8 for proper character decoding
    }

    private fun getPrivateKey(salt: String, keyResource: String): PrivateKey? {
        return try {
            val decodedPK = decodeBase64ToString(keyResource)
            val decodedPKUnsalted = getStringPrivateKey(salt, decodedPK) //decodedPK.replace("25cf6a500517cde1d968f23d424a2632", "")
            val keySpec = PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(decodedPKUnsalted)
            )
            val kf: KeyFactory = KeyFactory.getInstance("RSA")
            kf.generatePrivate(keySpec)
        } catch (ex: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createJWT(
        context: Context,
        url: String,
        clientId: String,
        messages: Map<String, Any>,
        claims: Claims,
        keyResource: String,
        salt: String,
    ): String {
        val now = Instant.now()
        val currentDate = Date.from(now)

        val jwtClaimsSet = Jwts.builder()
            .setIssuer(clientId)
            .setAudience(url)
            .setExpiration(Date.from(now.plusSeconds(120)))
            .setNotBefore(currentDate)
            .setIssuedAt(currentDate)
            .setId(UUID.randomUUID().toString())
            .signWith(getPrivateKey(salt, keyResource), SignatureAlgorithm.RS256)
            .setHeaderParam("kid", "media_pk")
            .setHeaderParam("typ", "JWT")

        messages.entries.forEach {
            jwtClaimsSet.claim(it.key, it.value)
        }

        jwtClaimsSet.claim("claims", claims)

        var jwt = ""
        try {
            jwt = jwtClaimsSet.compact()
        } catch (ex: Exception) {
        }

        return jwt
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decodeJWT(jwt: String): String {
        val jwtBody = jwt.split(".")[1]
        return String(Base64.getDecoder().decode(jwtBody))
    }
}