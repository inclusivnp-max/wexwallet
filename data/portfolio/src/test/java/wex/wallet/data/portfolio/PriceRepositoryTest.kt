@file:Suppress("MagicNumber")

package wex.wallet.data.portfolio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import wex.wallet.core.network.CoinGeckoClient

class PriceRepositoryTest {

    @Test
    fun `coinIdForSymbol maps known symbols`() {
        assertEquals(CoinGeckoClient.ETHEREUM, PriceRepository.coinIdForSymbol("ETH"))
        assertEquals(CoinGeckoClient.BITCOIN, PriceRepository.coinIdForSymbol("BTC"))
        assertEquals(CoinGeckoClient.SOLANA, PriceRepository.coinIdForSymbol("SOL"))
        assertEquals(CoinGeckoClient.THE_OPEN_NETWORK, PriceRepository.coinIdForSymbol("TON"))
        assertEquals(CoinGeckoClient.BINANCECOIN, PriceRepository.coinIdForSymbol("BNB"))
    }

    @Test
    fun `coinIdForSymbol is case insensitive`() {
        assertEquals(CoinGeckoClient.ETHEREUM, PriceRepository.coinIdForSymbol("eth"))
    }

    @Test
    fun `POL and MATIC map to the same coin`() {
        assertEquals(
            PriceRepository.coinIdForSymbol("MATIC"),
            PriceRepository.coinIdForSymbol("POL"),
        )
    }

    @Test
    fun `unknown symbol has no coin id`() {
        assertNull(PriceRepository.coinIdForSymbol("NOTACOIN"))
    }
}
