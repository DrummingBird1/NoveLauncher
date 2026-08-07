package com.ailauncher.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableBackupCryptoTest {

    private val crypto = PortableBackupCrypto()

    @Test fun `round-trips plaintext through the same password`() {
        val plain = """{"appearance":{},"pages":{}}"""
        val envelope = crypto.encrypt(plain, "correct horse battery staple")
        assertEquals(plain, crypto.decryptOrNull(envelope, "correct horse battery staple"))
    }

    @Test fun `wrong password fails to decrypt`() {
        val envelope = crypto.encrypt("secret settings blob", "right-password")
        assertNull(crypto.decryptOrNull(envelope, "wrong-password"))
    }

    @Test fun `envelope is tagged with the NVLBK1 prefix`() {
        val envelope = crypto.encrypt("data", "pw")
        assertTrue(envelope.startsWith(PortableBackupCrypto.PREFIX))
        assertTrue(PortableBackupCrypto.isEncrypted(envelope))
    }

    @Test fun `plain JSON is not mistaken for an encrypted envelope`() {
        assertFalse(PortableBackupCrypto.isEncrypted("""{"appearance":{}}"""))
    }

    @Test fun `two encryptions of the same plaintext produce different envelopes`() {
        // Random salt + IV per call — ciphertext must not be deterministic.
        val a = crypto.encrypt("same plaintext", "same-password")
        val b = crypto.encrypt("same plaintext", "same-password")
        assertTrue(a != b)
        assertEquals("same plaintext", crypto.decryptOrNull(a, "same-password"))
        assertEquals("same plaintext", crypto.decryptOrNull(b, "same-password"))
    }

    @Test fun `garbage input is not mistaken for a decryptable envelope`() {
        assertNull(crypto.decryptOrNull("not an envelope at all", "any-password"))
    }
}
