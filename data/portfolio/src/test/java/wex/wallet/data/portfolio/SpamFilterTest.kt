@file:Suppress("MagicNumber")

package wex.wallet.data.portfolio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class SpamFilterTest {

    private fun token(
        symbol: String = "USDC",
        name: String = "USD Coin",
        balance: BigInteger = BigInteger.valueOf(1_000_000),
    ) = TokenBalance(
        contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
        symbol = symbol,
        name = name,
        decimals = 6,
        balance = balance,
    )

    @Test
    fun `legit token is not spam`() {
        assertFalse(SpamFilter.isSpam(token()))
    }

    @Test
    fun `token with url in name is spam`() {
        assertTrue(SpamFilter.isSpam(token(name = "Claim at https://scam.xyz")))
    }

    @Test
    fun `token with airdrop in symbol is spam`() {
        assertTrue(SpamFilter.isSpam(token(symbol = "AIRDROP")))
    }

    @Test
    fun `token with telegram link is spam`() {
        assertTrue(SpamFilter.isSpam(token(name = "Join t.me/freecrypto")))
    }

    @Test
    fun `zero balance token is spam`() {
        assertTrue(SpamFilter.isSpam(token(balance = BigInteger.ZERO)))
    }

    @Test
    fun `overly long symbol is spam`() {
        assertTrue(SpamFilter.isSpam(token(symbol = "VERYLONGTOKENSYMBOL")))
    }

    @Test
    fun `filter removes only spam tokens`() {
        val tokens = listOf(
            token(symbol = "USDC", name = "USD Coin"),
            token(symbol = "SCAM", name = "Visit www.scam.io"),
            token(symbol = "DAI", name = "Dai Stablecoin"),
        )
        val filtered = SpamFilter.filter(tokens)
        assertEquals(2, filtered.size)
        assertTrue(filtered.none { it.symbol == "SCAM" })
    }
}
