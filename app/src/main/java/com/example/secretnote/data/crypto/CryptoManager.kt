package com.example.secretnote.data.crypto

data class EncryptedData(
    val ciphertextBase64: String,
    val ivBase64: String
)

class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface CryptoManager {
    @Throws(CryptoException::class)
    fun encrypt(plaintext: String): EncryptedData

    @Throws(CryptoException::class)
    fun decrypt(encryptedData: EncryptedData): String
}
