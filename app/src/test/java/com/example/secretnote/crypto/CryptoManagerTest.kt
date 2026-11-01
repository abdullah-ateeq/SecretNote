package com.example.secretnote.crypto

import com.example.secretnote.data.crypto.CryptoException
import com.example.secretnote.data.crypto.CryptoManager
import com.example.secretnote.data.crypto.EncryptedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Standard JVM AES-GCM Cipher test implementing CryptoManager to verify
 * cryptographic guarantees: confidentiality, integrity (tamper detection),
 * random IV generation, and plaintext non-persistence.
 */
class TestAesGcmCryptoManager : CryptoManager {
    private val secretKey: SecretKey
    private val secureRandom = SecureRandom()

    init {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        secretKey = keyGen.generateKey()
    }

    override fun encrypt(plaintext: String): EncryptedData {
        return try {
            val iv = ByteArray(12)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            EncryptedData(
                ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
                ivBase64 = Base64.getEncoder().encodeToString(iv)
            )
        } catch (e: Exception) {
            throw CryptoException("Encryption failed: ${e.message}", e)
        }
    }

    override fun decrypt(encryptedData: EncryptedData): String {
        return try {
            val iv = Base64.getDecoder().decode(encryptedData.ivBase64)
            val ciphertext = Base64.getDecoder().decode(encryptedData.ciphertextBase64)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            String(plaintextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw CryptoException("Decryption failed: ${e.message}", e)
        }
    }
}

class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        cryptoManager = TestAesGcmCryptoManager()
    }

    @Test
    fun `encrypt and decrypt returns identical plaintext`() {
        val originalText = "Top secret memo: 42 is the answer to everything."
        val encrypted = cryptoManager.encrypt(originalText)

        assertNotNull(encrypted.ciphertextBase64)
        assertNotNull(encrypted.ivBase64)

        val decrypted = cryptoManager.decrypt(encrypted)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun `plaintext is never stored directly in ciphertext`() {
        val sensitiveContent = "Password123!_StrictlyConfidential"
        val encrypted = cryptoManager.encrypt(sensitiveContent)

        // Verify that the ciphertext does NOT contain the plaintext string or raw bytes
        assertFalse(
            "Ciphertext must never contain raw plaintext",
            encrypted.ciphertextBase64.contains(sensitiveContent)
        )
        assertFalse(
            "IV must never contain raw plaintext",
            encrypted.ivBase64.contains(sensitiveContent)
        )
    }

    @Test
    fun `subsequent encryptions of same text produce unique IVs and ciphertexts`() {
        val sample = "Consistent content"
        val first = cryptoManager.encrypt(sample)
        val second = cryptoManager.encrypt(sample)

        assertNotEquals("Each encryption must use a unique IV", first.ivBase64, second.ivBase64)
        assertNotEquals("Each encryption must produce different ciphertext", first.ciphertextBase64, second.ciphertextBase64)

        assertEquals(sample, cryptoManager.decrypt(first))
        assertEquals(sample, cryptoManager.decrypt(second))
    }

    @Test
    fun `tampered ciphertext throws CryptoException due to GCM authentication tag mismatch`() {
        val secret = "Financial records 2026"
        val encrypted = cryptoManager.encrypt(secret)

        // Tamper with the ciphertext
        val tamperedBytes = Base64.getDecoder().decode(encrypted.ciphertextBase64)
        tamperedBytes[0] = (tamperedBytes[0].toInt() xor 0xFF).toByte()
        val tamperedCiphertext = Base64.getEncoder().encodeToString(tamperedBytes)

        val corruptedData = EncryptedData(
            ciphertextBase64 = tamperedCiphertext,
            ivBase64 = encrypted.ivBase64
        )

        assertThrows(CryptoException::class.java) {
            cryptoManager.decrypt(corruptedData)
        }
    }

    @Test
    fun `empty and multiline strings encrypt and decrypt properly`() {
        val multiline = "Line 1\nLine 2\n\nSpecial characters: @#$%^&*()_+~`|}{[]:;?><,./"
        val encrypted = cryptoManager.encrypt(multiline)
        assertEquals(multiline, cryptoManager.decrypt(encrypted))

        val empty = ""
        val encryptedEmpty = cryptoManager.encrypt(empty)
        assertEquals(empty, cryptoManager.decrypt(encryptedEmpty))
    }
}
