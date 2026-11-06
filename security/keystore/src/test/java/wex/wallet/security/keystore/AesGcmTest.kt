package wex.wallet.security.keystore

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.security.SecureRandom

class AesGcmTest {

    @Test
    fun `encrypt-decrypt round trip`() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val plaintext = "hello world".toByteArray()

        val encrypted = AesGcm.encrypt(key, plaintext)
        val decrypted = AesGcm.decrypt(key, encrypted)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `ciphertext differs from plaintext`() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val plaintext = ByteArray(64).also { SecureRandom().nextBytes(it) }

        val encrypted = AesGcm.encrypt(key, plaintext)
        assertFalse(encrypted.contentEquals(plaintext))
    }

    @Test
    fun `different encryptions produce different ciphertexts`() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val plaintext = "same data".toByteArray()

        val enc1 = AesGcm.encrypt(key, plaintext)
        val enc2 = AesGcm.encrypt(key, plaintext)

        assertFalse(enc1.contentEquals(enc2))
    }

    @Test(expected = Exception::class)
    fun `wrong key fails decryption`() {
        val key1 = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key2 = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val plaintext = "secret".toByteArray()

        val encrypted = AesGcm.encrypt(key1, plaintext)
        AesGcm.decrypt(key2, encrypted)
    }

    @Test(expected = Exception::class)
    fun `tampered ciphertext fails`() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encrypted = AesGcm.encrypt(key, "data".toByteArray())
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0xFF).toByte()
        AesGcm.decrypt(key, encrypted)
    }

    @Test
    fun `empty plaintext round trip`() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encrypted = AesGcm.encrypt(key, ByteArray(0))
        val decrypted = AesGcm.decrypt(key, encrypted)
        assertArrayEquals(ByteArray(0), decrypted)
    }
}
