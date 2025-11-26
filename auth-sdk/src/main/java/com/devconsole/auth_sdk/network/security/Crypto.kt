package com.devconsole.auth_sdk.network.security

interface Crypto {
    /**
     * @return [IV][CIPHERTEXT] combined.
     */
    fun encrypt(plain: ByteArray): ByteArray

    /**
     * @param encrypted [IV][CIPHERTEXT] combined.
     */
    fun decrypt(encrypted: ByteArray): ByteArray
}