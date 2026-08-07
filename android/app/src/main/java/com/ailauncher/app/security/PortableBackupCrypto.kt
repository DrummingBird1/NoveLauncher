package com.ailauncher.app.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-derived AES-256-GCM for exported backup files.
 *
 * Unlike [SecureCrypto] (AndroidKeyStore-backed, tied to one install), a portable
 * backup must decrypt wherever the user enters the same password — possibly a
 * different device — so the key comes from PBKDF2 over the password, not hardware.
 * Same PBKDF2 iteration count as [com.ailauncher.app.security.AppLockManager]'s
 * credential hashing, for consistency.
 *
 * Envelope format: "NVLBK1:" + Base64(salt(16) || iv(12) || ciphertext). The prefix
 * lets callers tell an encrypted export apart from a legacy plain-JSON one without
 * attempting decryption first.
 */
class PortableBackupCrypto {

    companion object {
        const val PREFIX = "NVLBK1:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256

        fun isEncrypted(content: String): Boolean = content.startsWith(PREFIX)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(plain: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, combined, salt.size + iv.size, ciphertext.size)
        return PREFIX + Base64.getEncoder().encodeToString(combined)
    }

    /** Returns the decrypted plaintext, or null on a wrong password / corrupt envelope. */
    fun decryptOrNull(envelope: String, password: String): String? {
        if (!isEncrypted(envelope)) return null
        return try {
            val combined = Base64.getDecoder().decode(envelope.removePrefix(PREFIX))
            if (combined.size <= SALT_LENGTH + IV_LENGTH) return null
            val salt = combined.copyOfRange(0, SALT_LENGTH)
            val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val ciphertext = combined.copyOfRange(SALT_LENGTH + IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
