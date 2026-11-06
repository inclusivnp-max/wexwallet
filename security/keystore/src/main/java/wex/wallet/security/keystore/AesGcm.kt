package wex.wallet.security.keystore

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object AesGcm {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128

    fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(key: ByteArray, ivAndCiphertext: ByteArray): ByteArray {
        require(ivAndCiphertext.size > IV_LENGTH) { "Data too short" }
        val iv = ivAndCiphertext.copyOfRange(0, IV_LENGTH)
        val ciphertext = ivAndCiphertext.copyOfRange(IV_LENGTH, ivAndCiphertext.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
