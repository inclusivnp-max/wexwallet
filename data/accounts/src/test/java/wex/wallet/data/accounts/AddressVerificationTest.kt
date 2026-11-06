package wex.wallet.data.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import wex.wallet.chain.bitcoin.BitcoinSegwitAddressDeriver
import wex.wallet.chain.bitcoin.BitcoinTaprootAddressDeriver
import wex.wallet.chain.evm.EvmAddressDeriver
import wex.wallet.chain.solana.SolanaAddressDeriver
import wex.wallet.security.mnemonic.Mnemonic

class AddressVerificationTest {

    private val abandonWords =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .split(" ")

    @Test
    fun `EVM address matches Python reference for abandon mnemonic`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val address = AccountDeriver.deriveAddress(seed, EvmAddressDeriver())
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)
        seed.close()
    }

    @Test
    fun `Bitcoin segwit address matches BIP84 reference for abandon mnemonic`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val address = AccountDeriver.deriveAddress(seed, BitcoinSegwitAddressDeriver())
        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", address)
        seed.close()
    }

    /**
     * Официальный вектор BIP86, раздел «Test vectors», m/86'/0'/0'/0/0.
     * Проверять надо именно значение: taproot-адрес требует твика внутреннего ключа,
     * и без него получится синтаксически валидный bc1p-адрес, с которого нельзя потратить.
     */
    @Test
    fun `Bitcoin taproot address matches BIP86 reference for abandon mnemonic`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val address = AccountDeriver.deriveAddress(seed, BitcoinTaprootAddressDeriver())
        assertEquals("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr", address)
        seed.close()
    }

    @Test
    fun `Solana address for abandon mnemonic`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val address = AccountDeriver.deriveAddress(seed, SolanaAddressDeriver())
        assertTrue(address.isNotEmpty())
        assertTrue(address.length in 32..44)
        seed.close()
    }

    @Test
    fun `all four chains produce distinct addresses`() {
        val seed = Mnemonic.toSeed(abandonWords)
        val derivers = listOf(
            EvmAddressDeriver(),
            BitcoinSegwitAddressDeriver(),
            BitcoinTaprootAddressDeriver(),
            SolanaAddressDeriver(),
        )
        val addresses = derivers.map { AccountDeriver.deriveAddress(seed, it) }
        assertEquals(4, addresses.toSet().size)
        seed.close()
    }
}
