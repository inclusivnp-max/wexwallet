package wex.wallet.security.mnemonic

import org.junit.Assert.assertEquals
import org.junit.Test
import wex.wallet.core.common.SensitiveBytes

@OptIn(ExperimentalStdlibApi::class)
class Slip0010Test {

    @Test
    fun `SLIP-0010 vector 1 - master key`() {
        val seed = SensitiveBytes.wrap(V1_SEED.hexToByteArray())

        val key = HdKeyDeriver.deriveSlip0010Ed25519(seed, "m")
        assertEquals(
            "2b4be7f19ee27bbf30c667b642d5f4aa69fd169872f8fc3059c08ebae2eb19e7",
            key.privateKey.copyBytes().toHexString(),
        )
        assertEquals(
            "90046a93de5380a72b5e45010748567d5ea02bbf6522f979e05c0d8d8ca9fffb",
            key.chainCode.toHexString(),
        )
        assertEquals(
            "a4b2856bfec510abab89753fac1ac0e1112364e7d250545963f135f2a33188ed",
            key.publicKey.toHexString(),
        )
        key.close()
    }

    @Test
    fun `SLIP-0010 vector 1 - derive m_0h`() {
        val seed = SensitiveBytes.wrap(V1_SEED.hexToByteArray())

        val key = HdKeyDeriver.deriveSlip0010Ed25519(seed, "m/0'")
        assertEquals(
            "68e0fe46dfb67e368c75379acec591dad19df3cde26e63b93a8e704f1dade7a3",
            key.privateKey.copyBytes().toHexString(),
        )
        assertEquals(
            "8b59aa11380b624e81507a27fedda59fea6d0b779a778918a2fd3590e16e9c69",
            key.chainCode.toHexString(),
        )
        assertEquals(
            "8c8a13df77a28f3445213a0f432fde644acaa215fc72dcdf300d5efaa85d350c",
            key.publicKey.toHexString(),
        )
        key.close()
    }

    @Test
    fun `SLIP-0010 vector 1 - derive m_0h_1h`() {
        val seed = SensitiveBytes.wrap(V1_SEED.hexToByteArray())

        val key = HdKeyDeriver.deriveSlip0010Ed25519(seed, "m/0'/1'")
        assertEquals(
            "b1d0bad404bf35da785a64ca1ac54b2617211d2777696fbffaf208f746ae84f2",
            key.privateKey.copyBytes().toHexString(),
        )
        assertEquals(
            "a320425f77d1b5c2505a6b1b27382b37368ee640e3557c315416801243552f14",
            key.chainCode.toHexString(),
        )
        assertEquals(
            "1932a5270f335bed617d5b935c80aedb1a35bd9fc1e31acafd5372c30f5c1187",
            key.publicKey.toHexString(),
        )
        key.close()
    }

    @Test
    fun `SLIP-0010 vector 2 - longer seed master key`() {
        val seed = SensitiveBytes.wrap(V2_SEED.hexToByteArray())

        val key = HdKeyDeriver.deriveSlip0010Ed25519(seed, "m")
        assertEquals(
            "171cb88b1b3c1db25add599712e36245d75bc65a1a5c9e18d76f9f2b1eab4012",
            key.privateKey.copyBytes().toHexString(),
        )
        assertEquals(
            "ef70a74db9c3a5af931b5fe73ed8e1a53464133654fd55e7a66f8570b8e33c3b",
            key.chainCode.toHexString(),
        )
        key.close()
    }

    @Test
    fun `Solana derivation path produces valid key`() {
        val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .split(" ")
        val seed = Mnemonic.toSeed(words)

        val key = HdKeyDeriver.deriveSlip0010Ed25519(seed, DerivationPaths.SOLANA)
        assertEquals(32, key.privateKey.size)
        assertEquals(32, key.publicKey.size)
        key.close()
    }

    @Test
    fun `TON derivation path produces valid key`() {
        val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .split(" ")
        val seed = Mnemonic.toSeed(words)

        val key = HdKeyDeriver.deriveSlip0010Ed25519(seed, DerivationPaths.TON)
        assertEquals(32, key.privateKey.size)
        assertEquals(32, key.publicKey.size)
        key.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-hardened derivation rejected for ed25519`() {
        val seed = SensitiveBytes.wrap(V1_SEED.hexToByteArray())
        HdKeyDeriver.deriveSlip0010Ed25519(seed, "m/0")
    }

    companion object {
        private const val V1_SEED = "000102030405060708090a0b0c0d0e0f"
        private const val V2_SEED =
            "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a2" +
                "9f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542"
    }
}
