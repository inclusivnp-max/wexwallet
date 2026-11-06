@file:Suppress("MagicNumber")

package wex.wallet.security.keystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object KeystoreKeys {

    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun createBiometricKey(alias: String) {
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= 30) {
            specBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)

        if (Build.VERSION.SDK_INT >= 28) {
            try {
                specBuilder.setIsStrongBoxBacked(true)
                generator.init(specBuilder.build())
                generator.generateKey()
                return
            } catch (_: Exception) {
                specBuilder.setIsStrongBoxBacked(false)
            }
        }

        generator.init(specBuilder.build())
        generator.generateKey()
    }

    fun getEncryptCipher(alias: String): Cipher {
        val keyStore = loadKeyStore()
        val key = keyStore.getKey(alias, null)
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    fun getDecryptCipher(alias: String, iv: ByteArray): Cipher {
        val keyStore = loadKeyStore()
        val key = keyStore.getKey(alias, null)
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
    }

    fun hasKey(alias: String): Boolean {
        val keyStore = loadKeyStore()
        return keyStore.containsAlias(alias)
    }

    fun deleteKey(alias: String) {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(PROVIDER).apply { load(null) }
}
