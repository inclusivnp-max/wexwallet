package wex.wallet.chain.bitcoin

import fr.acinq.bitcoin.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class BitcoinAddressDeriverTest {

    /**
     * Ключ и адрес — официальный вектор BIP84 для m/84'/0'/0'/0/0.
     * Сверяется полное значение: проверка одного префикса прошла бы и при неверной деривации.
     */
    @Test
    fun `segwit address matches BIP84 vector`() {
        val pubKeyHex = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c"
        val deriver = BitcoinSegwitAddressDeriver()
        val address = deriver.deriveAddress(pubKeyHex.hexToByteArray())
        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", address)
    }

    @Test
    fun `segwit address starts with tb1q for testnet`() {
        val pubKeyHex = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c"
        val deriver = BitcoinSegwitAddressDeriver(chain = Chain.Testnet3)
        val address = deriver.deriveAddress(pubKeyHex.hexToByteArray())
        assertTrue("Address should start with tb1q: $address", address.startsWith("tb1q"))
    }

    @Test
    fun `taproot address starts with bc1p for mainnet`() {
        val pubKeyHex = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c"
        val deriver = BitcoinTaprootAddressDeriver()
        val address = deriver.deriveAddress(pubKeyHex.hexToByteArray())
        assertTrue("Address should start with bc1p: $address", address.startsWith("bc1p"))
    }

    @Test
    fun `taproot address starts with tb1p for testnet`() {
        val pubKeyHex = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c"
        val deriver = BitcoinTaprootAddressDeriver(chain = Chain.Testnet3)
        val address = deriver.deriveAddress(pubKeyHex.hexToByteArray())
        assertTrue("Address should start with tb1p: $address", address.startsWith("tb1p"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `segwit rejects non-33-byte key`() {
        BitcoinSegwitAddressDeriver().deriveAddress(ByteArray(32))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `taproot rejects non-33-byte key`() {
        BitcoinTaprootAddressDeriver().deriveAddress(ByteArray(65))
    }

    @Test
    fun `segwit chain id and curve`() {
        val deriver = BitcoinSegwitAddressDeriver()
        assertEquals(wex.wallet.chain.api.ChainId.BITCOIN, deriver.chainId)
        assertEquals(wex.wallet.chain.api.KeyCurve.SECP256K1, deriver.curve)
    }

    @Test
    fun `taproot chain id and curve`() {
        val deriver = BitcoinTaprootAddressDeriver()
        assertEquals(wex.wallet.chain.api.ChainId.BITCOIN, deriver.chainId)
        assertEquals(wex.wallet.chain.api.KeyCurve.SECP256K1, deriver.curve)
    }

    @Test
    fun `different keys produce different addresses`() {
        val key1 = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c"
            .hexToByteArray()
        val key2 = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2"
            .hexToByteArray()
        val deriver = BitcoinSegwitAddressDeriver()
        assertTrue(deriver.deriveAddress(key1) != deriver.deriveAddress(key2))
    }
}
