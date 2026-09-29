package com.drs.ai.features.settings

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN hashing with PBKDF2-HmacSHA256 (150k iterations, 16-byte random salt).
 * The plain PIN never touches storage.
 */
object PinCrypto {

    private const val ITERATIONS = 150_000
    private const val KEY_LEN = 256

    fun newSalt(): String {
        val salt = ByteArray(16)
        java.security.SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hash(pin: String, saltHex: String): String {
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LEN)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return f.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
    }

    fun verify(pin: String, saltHex: String, expectedHex: String): Boolean {
        val computed = hash(pin, saltHex)
        if (computed.length != expectedHex.length) return false
        var diff = 0
        for (i in computed.indices) diff = diff or (computed[i].code xor expectedHex[i].code)
        return diff == 0
    }
}
