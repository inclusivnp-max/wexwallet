@file:Suppress("MagicNumber", "LongParameterList")

package wex.wallet.data.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class ActivitySearchTest {

    private fun item(
        symbol: String = "USDC",
        network: String = "Ethereum",
        counterparty: String = "0xabc0000000000000000000000000000000000001",
        txHash: String = "0xfeed",
        method: String = "Перевод токена",
        amount: BigInteger = BigInteger.valueOf(2_500_000),
        decimals: Int = 6,
        id: String = symbol + txHash,
    ) = ActivityItem(
        id = id,
        txHash = txHash,
        chainId = 1,
        networkName = network,
        explorerUrl = "",
        timestampSeconds = 1,
        blockNumber = 1,
        kind = ActivityKind.TOKEN_TRANSFER,
        direction = ActivityDirection.OUTGOING,
        status = ActivityStatus.SUCCESS,
        symbol = symbol,
        decimals = decimals,
        amount = amount,
        counterparty = counterparty,
        methodLabel = method,
    )

    private val feed = listOf(
        item(symbol = "USDC", network = "Ethereum", id = "a"),
        item(symbol = "DAI", network = "Polygon", txHash = "0xbeef", id = "b"),
        item(symbol = "WETH", network = "Arbitrum", txHash = "0xcafe", id = "c"),
    )

    @Test
    fun `пустой запрос возвращает всё`() {
        assertEquals(feed, ActivitySearch.apply(feed, ""))
    }

    @Test
    fun `запрос из пробелов возвращает всё`() {
        assertEquals(feed, ActivitySearch.apply(feed, "   "))
    }

    @Test
    fun `поиск по символу токена`() {
        val found = ActivitySearch.apply(feed, "dai")
        assertEquals(1, found.size)
        assertEquals("DAI", found[0].symbol)
    }

    @Test
    fun `поиск не зависит от регистра`() {
        assertEquals(1, ActivitySearch.apply(feed, "USDC").size)
        assertEquals(1, ActivitySearch.apply(feed, "usdc").size)
    }

    @Test
    fun `поиск по имени сети`() {
        val found = ActivitySearch.apply(feed, "polygon")
        assertEquals(1, found.size)
        assertEquals("Polygon", found[0].networkName)
    }

    @Test
    fun `поиск по хешу транзакции`() {
        assertEquals(1, ActivitySearch.apply(feed, "0xcafe").size)
    }

    @Test
    fun `поиск по адресу контрагента`() {
        assertEquals(3, ActivitySearch.apply(feed, "0xabc").size)
    }

    @Test
    fun `поиск по метке метода`() {
        assertEquals(3, ActivitySearch.apply(feed, "перевод").size)
    }

    @Test
    fun `поиск по сумме`() {
        assertTrue(ActivitySearch.apply(feed, "2.5").isNotEmpty())
    }

    @Test
    fun `несовпадающий запрос даёт пусто`() {
        assertTrue(ActivitySearch.apply(feed, "несуществующее").isEmpty())
    }

    @Test
    fun `запрос обрезается по краям`() {
        assertEquals(1, ActivitySearch.apply(feed, "  dai  ").size)
    }
}
