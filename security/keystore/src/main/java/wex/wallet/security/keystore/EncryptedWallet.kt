package wex.wallet.security.keystore

class EncryptedWallet(
    val encryptedSeed: ByteArray,
    val encryptedDekByPin: ByteArray,
    val argon2Salt: ByteArray,
    val encryptedDekByBio: ByteArray?,
    val keystoreAlias: String?,
) {
    fun withBiometric(encryptedDek: ByteArray, alias: String): EncryptedWallet =
        EncryptedWallet(encryptedSeed, encryptedDekByPin, argon2Salt, encryptedDek, alias)

    fun withNewPin(newEncryptedDek: ByteArray, newSalt: ByteArray): EncryptedWallet =
        EncryptedWallet(encryptedSeed, newEncryptedDek, newSalt, encryptedDekByBio, keystoreAlias)
}
