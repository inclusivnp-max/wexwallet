package wex.wallet.security.keystore

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.CharBuffer
import java.security.SecureRandom

class PinHasher {

    private val argon2 = Argon2Kt()

    fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val pinBytes = charArrayToBytes(pin)
        try {
            val result = argon2.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = pinBytes,
                salt = salt,
                tCostInIterations = T_COST,
                mCostInKibibyte = M_COST,
                hashLengthInBytes = KEY_LENGTH,
            )
            return result.rawHashAsByteArray()
        } finally {
            pinBytes.fill(0)
        }
    }

    fun verify(pin: CharArray, salt: ByteArray, expected: ByteArray): Boolean {
        val derived = derive(pin, salt)
        try {
            return derived.contentEquals(expected)
        } finally {
            derived.fill(0)
        }
    }

    companion object {
        const val T_COST = 3
        const val M_COST = 65536
        const val KEY_LENGTH = 32
        const val SALT_LENGTH = 16

        fun generateSalt(): ByteArray =
            ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }

        internal fun charArrayToBytes(chars: CharArray): ByteArray {
            val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(chars))
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return bytes
        }
    }
}
