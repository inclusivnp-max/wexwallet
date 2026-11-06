package wex.wallet.security.keystore

import wex.wallet.core.common.SensitiveBytes
import java.security.SecureRandom
import javax.crypto.Cipher

class EnvelopeEncryption(
    private val pinHasher: PinHasher = PinHasher(),
) {

    fun encrypt(seed: ByteArray, pin: CharArray): EncryptedWallet {
        val dek = ByteArray(DEK_LENGTH)
        SecureRandom().nextBytes(dek)
        try {
            val encryptedSeed = AesGcm.encrypt(dek, seed)
            val salt = PinHasher.generateSalt()
            val kek = pinHasher.derive(pin, salt)
            try {
                val encryptedDek = AesGcm.encrypt(kek, dek)
                return EncryptedWallet(
                    encryptedSeed = encryptedSeed,
                    encryptedDekByPin = encryptedDek,
                    argon2Salt = salt,
                    encryptedDekByBio = null,
                    keystoreAlias = null,
                )
            } finally {
                kek.fill(0)
            }
        } finally {
            dek.fill(0)
        }
    }

    fun decryptWithPin(wallet: EncryptedWallet, pin: CharArray): SensitiveBytes {
        val kek = pinHasher.derive(pin, wallet.argon2Salt)
        try {
            val dek = AesGcm.decrypt(kek, wallet.encryptedDekByPin)
            try {
                val seed = AesGcm.decrypt(dek, wallet.encryptedSeed)
                return SensitiveBytes.wrap(seed)
            } finally {
                dek.fill(0)
            }
        } finally {
            kek.fill(0)
        }
    }

    fun enableBiometric(wallet: EncryptedWallet, pin: CharArray, alias: String): EncryptedWallet {
        val kek = pinHasher.derive(pin, wallet.argon2Salt)
        try {
            val dek = AesGcm.decrypt(kek, wallet.encryptedDekByPin)
            try {
                KeystoreKeys.createBiometricKey(alias)
                val cipher = KeystoreKeys.getEncryptCipher(alias)
                val encryptedDekBio = cipher.iv + cipher.doFinal(dek)
                return wallet.withBiometric(encryptedDekBio, alias)
            } finally {
                dek.fill(0)
            }
        } finally {
            kek.fill(0)
        }
    }

    fun decryptWithBiometric(wallet: EncryptedWallet, authenticatedCipher: Cipher): SensitiveBytes {
        val encDekBio = requireNotNull(wallet.encryptedDekByBio) {
            "Biometric not enabled for this wallet"
        }
        val ciphertext = encDekBio.copyOfRange(IV_LENGTH, encDekBio.size)
        val dek = authenticatedCipher.doFinal(ciphertext)
        try {
            val seed = AesGcm.decrypt(dek, wallet.encryptedSeed)
            return SensitiveBytes.wrap(seed)
        } finally {
            dek.fill(0)
        }
    }

    fun changePin(wallet: EncryptedWallet, oldPin: CharArray, newPin: CharArray): EncryptedWallet {
        val oldKek = pinHasher.derive(oldPin, wallet.argon2Salt)
        try {
            val dek = AesGcm.decrypt(oldKek, wallet.encryptedDekByPin)
            try {
                val newSalt = PinHasher.generateSalt()
                val newKek = pinHasher.derive(newPin, newSalt)
                try {
                    val newEncryptedDek = AesGcm.encrypt(newKek, dek)
                    return wallet.withNewPin(newEncryptedDek, newSalt)
                } finally {
                    newKek.fill(0)
                }
            } finally {
                dek.fill(0)
            }
        } finally {
            oldKek.fill(0)
        }
    }

    fun disableBiometric(wallet: EncryptedWallet): EncryptedWallet {
        wallet.keystoreAlias?.let { KeystoreKeys.deleteKey(it) }
        return EncryptedWallet(
            encryptedSeed = wallet.encryptedSeed,
            encryptedDekByPin = wallet.encryptedDekByPin,
            argon2Salt = wallet.argon2Salt,
            encryptedDekByBio = null,
            keystoreAlias = null,
        )
    }

    companion object {
        private const val DEK_LENGTH = 32
        private const val IV_LENGTH = 12
    }
}
