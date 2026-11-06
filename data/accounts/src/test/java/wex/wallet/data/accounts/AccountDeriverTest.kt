package wex.wallet.data.accounts

import org.junit.Assert.assertEquals
import org.junit.Test
import wex.wallet.chain.api.AddressDeriver
import wex.wallet.chain.api.ChainId
import wex.wallet.chain.api.KeyCurve
import wex.wallet.security.mnemonic.Mnemonic

class AccountDeriverTest {

    private val abandonWords =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .split(" ")

    @Test
    fun `derive EVM address from abandon mnemonic`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val deriver = object : AddressDeriver {
            override val chainId = ChainId.EVM
            override val curve = KeyCurve.SECP256K1
            override val derivationPath = "m/44'/60'/0'/0/0"
            override fun deriveAddress(publicKey: ByteArray): String {
                assertEquals(33, publicKey.size)
                return "mock-evm"
            }
        }
        val address = AccountDeriver.deriveAddress(seed, deriver)
        assertEquals("mock-evm", address)
        seed.close()
    }

    @Test
    fun `derive Solana address from abandon mnemonic`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val deriver = object : AddressDeriver {
            override val chainId = ChainId.SOLANA
            override val curve = KeyCurve.ED25519
            override val derivationPath = "m/44'/501'/0'/0'"
            override fun deriveAddress(publicKey: ByteArray): String {
                assertEquals(32, publicKey.size)
                return "mock-solana"
            }
        }
        val address = AccountDeriver.deriveAddress(seed, deriver)
        assertEquals("mock-solana", address)
        seed.close()
    }

    @Test
    fun `deriveAllAddresses returns map for all chains`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val evmDeriver = object : AddressDeriver {
            override val chainId = ChainId.EVM
            override val curve = KeyCurve.SECP256K1
            override val derivationPath = "m/44'/60'/0'/0/0"
            override fun deriveAddress(publicKey: ByteArray) = "evm-addr"
        }
        val solDeriver = object : AddressDeriver {
            override val chainId = ChainId.SOLANA
            override val curve = KeyCurve.ED25519
            override val derivationPath = "m/44'/501'/0'/0'"
            override fun deriveAddress(publicKey: ByteArray) = "sol-addr"
        }
        val result = AccountDeriver.deriveAllAddresses(
            seed,
            listOf(evmDeriver, solDeriver),
        )
        assertEquals(2, result.size)
        assertEquals("evm-addr", result[evmDeriver])
        assertEquals("sol-addr", result[solDeriver])
        seed.close()
    }

    @Test
    fun `secp256k1 derivation produces 33-byte compressed pubkey`() {
        val seed = Mnemonic.toSeed(abandonWords)
        var capturedSize = 0
        val deriver = object : AddressDeriver {
            override val chainId = ChainId.BITCOIN
            override val curve = KeyCurve.SECP256K1
            override val derivationPath = "m/84'/0'/0'/0/0"
            override fun deriveAddress(publicKey: ByteArray): String {
                capturedSize = publicKey.size
                return "btc"
            }
        }
        AccountDeriver.deriveAddress(seed, deriver)
        assertEquals(33, capturedSize)
        seed.close()
    }

    @Test
    fun `ed25519 derivation produces 32-byte pubkey`() {
        val seed = Mnemonic.toSeed(abandonWords)
        var capturedSize = 0
        val deriver = object : AddressDeriver {
            override val chainId = ChainId.TON
            override val curve = KeyCurve.ED25519
            override val derivationPath = "m/44'/607'/0'"
            override fun deriveAddress(publicKey: ByteArray): String {
                capturedSize = publicKey.size
                return "ton"
            }
        }
        AccountDeriver.deriveAddress(seed, deriver)
        assertEquals(32, capturedSize)
        seed.close()
    }
}
