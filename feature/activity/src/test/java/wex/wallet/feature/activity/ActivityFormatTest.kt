@file:Suppress("MagicNumber")

package wex.wallet.feature.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import wex.wallet.data.activity.ActivityDirection
import wex.wallet.data.activity.ActivityItem
import wex.wallet.data.activity.ActivityKind
import wex.wallet.data.activity.ActivityStatus
import java.math.BigInteger

class ActivityFormatTest {

    private fun item(
        direction: ActivityDirection = ActivityDirection.OUTGOING,
        amount: BigInteger = BigInteger.valueOf(1_500_000),
        decimals: Int = 6,
        symbol: String = "USDC",
    ) = ActivityItem(
        id = "1", txHash = "0xabc", chainId = 1, networkName = "Ethereum",
        explorerUrl = "", timestampSeconds = 0, blockNumber = 0,
        kind = ActivityKind.TOKEN_TRANSFER, direction = direction,
        status = ActivityStatus.SUCCESS, symbol = symbol, decimals = decimals,
        amount = amount, counterparty = "0xdef",
    )

    @Test
    fun `исходящая сумма со знаком минус`() {
        assertTrue(ActivityFormat.signedAmount(item()).startsWith("-"))
    }

    @Test
    fun `входящая сумма со знаком плюс`() {
        assertTrue(ActivityFormat.signedAmount(item(ActivityDirection.INCOMING)).startsWith("+"))
    }

    @Test
    fun `перевод себе без знака`() {
        val text = ActivityFormat.signedAmount(item(ActivityDirection.SELF))
        assertTrue(!text.startsWith("-") && !text.startsWith("+"))
    }

    @Test
    fun `сумма учитывает знаки токена`() {
        assertEquals("-1.5 USDC", ActivityFormat.signedAmount(item()))
    }

    @Test
    fun `адрес сокращается`() {
        assertEquals("0x1234...cdef", ActivityFormat.shortenAddress("0x1234567890abcdef"))
    }

    @Test
    fun `короткий адрес не трогается`() {
        assertEquals("0xabc", ActivityFormat.shortenAddress("0xabc"))
    }

    @Test
    fun `относительное время в минутах`() {
        assertEquals("5 мин назад", ActivityFormat.relativeTime(0, 300))
    }

    @Test
    fun `относительное время в часах`() {
        assertEquals("2 ч назад", ActivityFormat.relativeTime(0, 7_200))
    }

    @Test
    fun `относительное время в днях`() {
        assertEquals("3 дн назад", ActivityFormat.relativeTime(0, 259_200))
    }

    @Test
    fun `совсем свежая транзакция`() {
        assertEquals("только что", ActivityFormat.relativeTime(0, 30))
    }

    @Test
    fun `время из будущего не даёт отрицательных значений`() {
        assertEquals("только что", ActivityFormat.relativeTime(1_000, 0))
    }
}
