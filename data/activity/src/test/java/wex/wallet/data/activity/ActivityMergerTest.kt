@file:Suppress("MagicNumber", "LongParameterList")

package wex.wallet.data.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import wex.wallet.core.network.NormalTransaction
import wex.wallet.core.network.TokenTransfer
import java.math.BigInteger

class ActivityMergerTest {

    // Кошелёк в checksum-регистре, Etherscan отдаёт lowercase — расхождение намеренное.
    private val wallet = "0xAbC1230000000000000000000000000000000001"
    private val walletLower = wallet.lowercase()
    private val other = "0x9999990000000000000000000000000000000002"

    private val chain = ActivityChain(1, "Ethereum", "https://etherscan.io")

    private fun tx(
        hash: String = "0xaaa",
        from: String = walletLower,
        to: String = other,
        value: BigInteger = BigInteger.ZERO,
        isError: Boolean = false,
        input: String = "0x",
        timestamp: Long = 1000,
    ) = NormalTransaction(
        txHash = hash,
        from = from,
        to = to,
        value = value,
        gasUsed = BigInteger.valueOf(21000),
        gasPrice = BigInteger.valueOf(1),
        timestamp = timestamp,
        blockNumber = 1,
        isError = isError,
        methodId = if (input.length >= 10) input.take(10) else "",
        input = input,
    )

    private fun transfer(
        hash: String = "0xaaa",
        from: String = walletLower,
        to: String = other,
        symbol: String = "USDC",
        decimals: Int = 6,
        value: BigInteger = BigInteger.valueOf(1_000_000),
        timestamp: Long = 1000,
        contract: String = "0xC0FFEE0000000000000000000000000000000003",
    ) = TokenTransfer(
        contractAddress = contract,
        tokenName = "USD Coin",
        tokenSymbol = symbol,
        tokenDecimals = decimals,
        from = from,
        to = to,
        value = value,
        txHash = hash,
        timestamp = timestamp,
        blockNumber = 1,
    )

    @Test
    fun `направление определяется несмотря на разный регистр адресов`() {
        assertEquals(
            ActivityDirection.OUTGOING,
            ActivityMerger.directionFor(wallet, walletLower, other),
        )
        assertEquals(
            ActivityDirection.INCOMING,
            ActivityMerger.directionFor(wallet, other, walletLower),
        )
    }

    @Test
    fun `перевод самому себе помечается как SELF`() {
        assertEquals(
            ActivityDirection.SELF,
            ActivityMerger.directionFor(wallet, walletLower, wallet),
        )
    }

    @Test
    fun `обычная транзакция без значения и без токенов это вызов контракта`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(input = "0x095ea7b3")),
            tokenTransfers = emptyList(),
        )
        assertEquals(1, result.items.size)
        assertEquals(ActivityKind.CONTRACT_CALL, result.items[0].kind)
    }

    @Test
    fun `обычная транзакция со значением это перевод монеты`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(value = BigInteger.valueOf(5))),
            tokenTransfers = emptyList(),
        )
        assertEquals(ActivityKind.NATIVE_TRANSFER, result.items[0].kind)
    }

    @Test
    fun `перевод токена не дублируется строкой обычной транзакции`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(hash = "0xaaa")),
            tokenTransfers = listOf(transfer(hash = "0xaaa")),
        )
        assertEquals(1, result.items.size)
        assertEquals(ActivityKind.TOKEN_TRANSFER, result.items[0].kind)
    }

    @Test
    fun `обмен даёт две токен-строки в одном хеше`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(hash = "0xswap")),
            tokenTransfers = listOf(
                transfer(hash = "0xswap", from = walletLower, to = other, symbol = "USDC"),
                transfer(hash = "0xswap", from = other, to = walletLower, symbol = "DAI"),
            ),
        )
        assertEquals(2, result.items.size)
        assertEquals(
            setOf(ActivityDirection.OUTGOING, ActivityDirection.INCOMING),
            result.items.map { it.direction }.toSet(),
        )
    }

    @Test
    fun `обмен монеты на токен даёт и нативную и токен-строку`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(hash = "0xswap", value = BigInteger.valueOf(1_000))),
            tokenTransfers = listOf(transfer(hash = "0xswap", from = other, to = walletLower)),
        )
        assertEquals(2, result.items.size)
        assertTrue(result.items.any { it.kind == ActivityKind.NATIVE_TRANSFER })
        assertTrue(result.items.any { it.kind == ActivityKind.TOKEN_TRANSFER })
    }

    @Test
    fun `провал транзакции переносится на токен-строку`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(hash = "0xaaa", isError = true)),
            tokenTransfers = listOf(transfer(hash = "0xaaa")),
        )
        assertEquals(ActivityStatus.FAILED, result.items[0].status)
    }

    @Test
    fun `идентификаторы строк уникальны`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(hash = "0xswap")),
            tokenTransfers = listOf(
                transfer(hash = "0xswap", symbol = "USDC"),
                transfer(hash = "0xswap", symbol = "DAI"),
            ),
        )
        assertEquals(result.items.size, result.items.map { it.id }.distinct().size)
    }

    @Test
    fun `свежие транзакции идут первыми`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(
                tx(hash = "0xold", value = BigInteger.ONE, timestamp = 100),
                tx(hash = "0xnew", value = BigInteger.ONE, timestamp = 900),
            ),
            tokenTransfers = emptyList(),
        )
        assertEquals("0xnew", result.items.first().txHash)
    }

    @Test
    fun `сортировка детерминирована при одинаковом времени`() {
        val items = listOf(
            tx(hash = "0xb", value = BigInteger.ONE, timestamp = 5),
            tx(hash = "0xa", value = BigInteger.ONE, timestamp = 5),
        )
        val first = ActivityMerger.mergeChain(wallet, chain, items, emptyList()).items
        val second = ActivityMerger.mergeChain(wallet, chain, items.reversed(), emptyList()).items
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun `спам-токены отфильтровываются и считаются`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = emptyList(),
            tokenTransfers = listOf(
                transfer(hash = "0x1", symbol = "USDC"),
                transfer(hash = "0x2", symbol = "SCAM").copy(tokenName = "Visit www.scam.io"),
            ),
        )
        assertEquals(1, result.items.size)
        assertEquals(1, result.hiddenSpamCount)
    }

    @Test
    fun `спам можно не фильтровать`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = emptyList(),
            tokenTransfers = listOf(transfer(hash = "0x2").copy(tokenName = "Claim at https://x.io")),
            filterSpam = false,
        )
        assertEquals(1, result.items.size)
        assertEquals(0, result.hiddenSpamCount)
    }

    @Test
    fun `метод расшифровывается из calldata`() {
        val approveInput = "0x095ea7b3" + "0".repeat(64) + "0".repeat(64)
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(input = approveInput)),
            tokenTransfers = emptyList(),
        )
        assertEquals("Разрешение на трату", result.items[0].methodLabel)
        assertTrue(result.items[0].isMethodDecoded)
    }

    @Test
    fun `неизвестный метод помечается честно`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(input = "0xdeadbeef" + "0".repeat(64))),
            tokenTransfers = emptyList(),
        )
        assertTrue(!result.items[0].isMethodDecoded)
        assertTrue(result.items[0].methodLabel.contains("Неизвестный"))
    }

    @Test
    fun `ссылка на эксплорер собирается`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(hash = "0xabc", value = BigInteger.ONE)),
            tokenTransfers = emptyList(),
        )
        assertEquals("https://etherscan.io/tx/0xabc", result.items[0].explorerUrl)
    }

    @Test
    fun `сумма токена делится по своим знакам`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = emptyList(),
            // 1.5 USDC при 6 знаках
            tokenTransfers = listOf(transfer(value = BigInteger.valueOf(1_500_000), decimals = 6)),
        )
        assertEquals("1.500000", result.items[0].displayAmount.toPlainString())
    }

    @Test
    fun `контрагент для исходящей это получатель`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(from = walletLower, to = other, value = BigInteger.ONE)),
            tokenTransfers = emptyList(),
        )
        assertEquals(other.lowercase(), result.items[0].counterparty)
    }

    @Test
    fun `контрагент для входящей это отправитель`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(from = other, to = walletLower, value = BigInteger.ONE)),
            tokenTransfers = emptyList(),
        )
        assertEquals(other.lowercase(), result.items[0].counterparty)
    }

    @Test
    fun `пустой вход даёт пустую ленту`() {
        val result = ActivityMerger.mergeChain(wallet, chain, emptyList(), emptyList())
        assertTrue(result.items.isEmpty())
        assertEquals(0, result.hiddenSpamCount)
    }

    @Test
    fun `адрес контракта сохраняется в нижнем регистре`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = emptyList(),
            tokenTransfers = listOf(transfer(contract = "0xC0FFEE0000000000000000000000000000000003")),
        )
        assertNotNull(result.items[0].contractAddress)
        assertEquals("0xc0ffee0000000000000000000000000000000003", result.items[0].contractAddress)
    }

    @Test
    fun `у нативной строки нет адреса контракта`() {
        val result = ActivityMerger.mergeChain(
            walletAddress = wallet,
            chain = chain,
            transactions = listOf(tx(value = BigInteger.ONE)),
            tokenTransfers = emptyList(),
        )
        assertNull(result.items[0].contractAddress)
    }
}
