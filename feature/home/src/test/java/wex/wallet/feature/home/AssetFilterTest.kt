@file:Suppress("MagicNumber")

package wex.wallet.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetFilterTest {

    private val assets = listOf(
        AssetItem(
            symbol = "ETH",
            name = "Ethereum",
            balance = "1 ETH",
            chainLabel = "Ethereum",
            address = "0xabc",
            chainId = 1,
        ),
        AssetItem(
            symbol = "USDC",
            name = "USD Coin",
            balance = "5 USDC",
            chainLabel = "Base",
            address = "0xabc",
            chainId = 8453,
            decimals = 6,
            contractAddress = "0xc0ffee1234",
        ),
        AssetItem(
            symbol = "BTC",
            name = "Bitcoin (Segwit)",
            balance = "",
            chainLabel = "Bitcoin (Segwit)",
            address = "bc1q",
        ),
    )

    @Test
    fun `пустой запрос возвращает всё`() {
        assertEquals(assets, AssetFilter.apply(assets, ""))
        assertEquals(assets, AssetFilter.apply(assets, "   "))
    }

    @Test
    fun `поиск по символу`() {
        assertEquals(1, AssetFilter.apply(assets, "usdc").size)
    }

    @Test
    fun `поиск не зависит от регистра`() {
        assertEquals(AssetFilter.apply(assets, "ETH").size, AssetFilter.apply(assets, "eth").size)
    }

    @Test
    fun `поиск по имени сети`() {
        assertEquals(1, AssetFilter.apply(assets, "base").size)
    }

    @Test
    fun `поиск по адресу контракта`() {
        val found = AssetFilter.apply(assets, "0xc0ffee")
        assertEquals(1, found.size)
        assertEquals("USDC", found[0].symbol)
    }

    @Test
    fun `несовпадающий запрос даёт пусто`() {
        assertTrue(AssetFilter.apply(assets, "zzz").isEmpty())
    }
}
