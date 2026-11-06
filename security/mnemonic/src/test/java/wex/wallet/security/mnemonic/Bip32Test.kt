package wex.wallet.security.mnemonic

import org.junit.Assert.assertEquals
import org.junit.Test
import wex.wallet.core.common.SensitiveBytes

@OptIn(ExperimentalStdlibApi::class)
class Bip32Test {

    @Test
    fun `BIP32 vector 1 - master key from seed`() {
        val seed = SensitiveBytes.wrap("000102030405060708090a0b0c0d0e0f".hexToByteArray())

        val key = HdKeyDeriver.deriveBip32(seed, "m")
        assertEquals(
            "e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35",
            key.privateKey.copyBytes().toHexString(),
        )
        assertEquals(
            "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508",
            key.chainCode.toHexString(),
        )
        key.close()
    }

    @Test
    fun `BIP32 vector 1 - derive m_0h`() {
        val seed = SensitiveBytes.wrap("000102030405060708090a0b0c0d0e0f".hexToByteArray())

        val key = HdKeyDeriver.deriveBip32(seed, "m/0'")
        assertEquals(
            "edb2e14f9ee77d26dd93b4ecede8d16ed408ce149b6cd80b0715a2d911a0afea",
            key.privateKey.copyBytes().toHexString(),
        )
        key.close()
    }

    @Test
    fun `BIP32 vector 2 - long derivation path`() {
        val seedHex =
            "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a2" +
                "9f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542"
        val seed = SensitiveBytes.wrap(seedHex.hexToByteArray())

        val key = HdKeyDeriver.deriveBip32(seed, "m/0/2147483647'/1")
        assertEquals(
            "704addf544a06e5ee4bea37098463c23613da32020d604506da8c0518e1da4b7",
            key.privateKey.copyBytes().toHexString(),
        )
        key.close()
    }

    @Test
    fun `EVM derivation path m_44h_60h_0h_0_0`() {
        val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .split(" ")
        val seed = Mnemonic.toSeed(words)

        val key = HdKeyDeriver.deriveBip32(seed, DerivationPaths.EVM)
        assertEquals(32, key.privateKey.size)
        assertEquals(33, key.publicKey.size)
        key.close()
    }

    @Test
    fun `Bitcoin segwit derivation path`() {
        val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .split(" ")
        val seed = Mnemonic.toSeed(words)

        val key = HdKeyDeriver.deriveBip32(seed, DerivationPaths.BITCOIN_SEGWIT)
        assertEquals(32, key.privateKey.size)
        assertEquals(33, key.publicKey.size)
        key.close()
    }
}
