package com.devconsole.auth_sdk.network.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CryptoManager(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val keystoreProvider: String = ANDROID_KEYSTORE_PROVIDER,
    transformation: String = TRANSFORMATION
) : Crypto {

    private val cipher: Cipher = try {
        Cipher.getInstance(transformation)
    } catch (_: Exception) {
        Cipher.getInstance(FALLBACK_TRANSFORMATION)
    }

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(keystoreProvider).apply { load(null) }
    } catch (_: Exception) {
        null
    }

    private val fallbackKey: SecretKey by lazy {
        KeyGenerator.getInstance(ALGORITHM).apply { init(256) }.generateKey()
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore
            ?.getEntry(DEFAULT_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        if (keyStore == null) return fallbackKey

        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, keystoreProvider)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setRandomizedEncryptionRequired(false) // we provide our own IV
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv

        val cipherBytes = cipher.doFinal(plain)

        // [IV][CIPHERTEXT]
        return iv + cipherBytes
    }

    override fun decrypt(encrypted: ByteArray): ByteArray {
        val blockSize = cipher.blockSize

        require(encrypted.size > blockSize) {
            "Encrypted data is too short to contain IV + ciphertext"
        }

        val iv = encrypted.copyOfRange(0, blockSize)
        val cipherBytes = encrypted.copyOfRange(blockSize, encrypted.size)

        cipher.init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
        return cipher.doFinal(cipherBytes)
    }

    companion object {
        private const val DEFAULT_KEY_ALIAS = "alias"
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"

        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val FALLBACK_TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/PKCS5Padding"
    }
}

