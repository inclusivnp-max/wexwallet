package wex.wallet.chain.solana

import org.junit.Assert.assertEquals
import org.junit.Test

class SolanaAddressDeriverTest {

    private val deriver = SolanaAddressDeriver()

    @Test
    fun `derive address from Ed25519 pubkey`() {
        val pubkey = ByteArray(32) { it.toByte() }
        val address = deriver.deriveAddress(pubkey)
        assert(address.isNotEmpty())
        assert(address.all { it in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject wrong key size`() {
        deriver.deriveAddress(ByteArray(33))
    }

    @Test
    fun `chain id and curve are correct`() {
        assertEquals(wex.wallet.chain.api.ChainId.SOLANA, deriver.chainId)
        assertEquals(wex.wallet.chain.api.KeyCurve.ED25519, deriver.curve)
    }
}
