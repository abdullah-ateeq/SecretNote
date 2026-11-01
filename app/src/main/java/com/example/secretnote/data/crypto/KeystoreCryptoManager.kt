package com.example.secretnote.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreCryptoManager(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : CryptoManager {

    companion object {
        const val DEFAULT_KEY_ALIAS = "SecretNote_Master_Key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            ?: throw CryptoException("Failed to retrieve secret key for alias: $keyAlias")
        return entry.secretKey
    }

    override fun encrypt(plaintext: String): EncryptedData {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv ?: throw CryptoException("Cipher initialization did not produce an IV")
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            EncryptedData(
                ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
                ivBase64 = Base64.getEncoder().encodeToString(iv)
            )
        } catch (e: Exception) {
            throw CryptoException("Failed to encrypt note payload: ${e.message}", e)
        }
    }

    override fun decrypt(encryptedData: EncryptedData): String {
        return try {
            val secretKey = getOrCreateSecretKey()
            val iv = Base64.getDecoder().decode(encryptedData.ivBase64)
            val ciphertext = Base64.getDecoder().decode(encryptedData.ciphertextBase64)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw CryptoException("Failed to decrypt note payload: ${e.message}", e)
        }
    }
}
