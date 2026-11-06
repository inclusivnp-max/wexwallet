@file:Suppress("MagicNumber")

package wex.wallet.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тела ответов взяты из реальных вызовов Etherscan API V2, а не выдуманы.
 */
class EtherscanResultTest {

    private fun body(raw: String): JsonObject =
        Json.parseToJsonElement(raw) as JsonObject

    @Test
    fun `успешный ответ не считается отказом`() {
        val response = body("""{"status":"1","message":"OK","result":[]}""")
        assertNull(classifyEtherscanFailure(response, 1))
    }

    @Test
    fun `сеть вне бесплатного тарифа распознаётся`() {
        val response = body(
            """{"status":"0","message":"NOTOK","result":"Free API access is not supported """ +
                """for this chain. Please upgrade your api plan for full chain coverage."}""",
        )
        val result = classifyEtherscanFailure(response, 8453)
        assertEquals(EtherscanResult.ChainNotCovered(8453), result)
    }

    @Test
    fun `отсутствие транзакций это не ошибка`() {
        val response = body("""{"status":"0","message":"No transactions found","result":[]}""")
        assertEquals(EtherscanResult.Empty, classifyEtherscanFailure(response, 1))
    }

    @Test
    fun `лимит запросов распознаётся`() {
        val response = body(
            """{"status":"0","message":"NOTOK","result":"Max calls per sec rate limit reached"}""",
        )
        assertEquals(EtherscanResult.RateLimited, classifyEtherscanFailure(response, 1))
    }

    @Test
    fun `неверный ключ распознаётся`() {
        val response = body("""{"status":"0","message":"NOTOK","result":"Invalid API Key"}""")
        val result = classifyEtherscanFailure(response, 1)
        assertTrue(result is EtherscanResult.Failure)
    }

    @Test
    fun `неизвестная ошибка сохраняет текст ответа`() {
        val response = body("""{"status":"0","message":"NOTOK","result":"Something odd happened"}""")
        val result = classifyEtherscanFailure(response, 1)
        assertEquals(EtherscanResult.Failure("Something odd happened"), result)
    }

    @Test
    fun `сеть вне тарифа объясняется человеку`() {
        val reason = EtherscanResult.ChainNotCovered(8453).unavailableReason()
        assertNotNull(reason)
        assertTrue(reason!!.contains("платный тариф"))
    }

    @Test
    fun `успешный результат не имеет причины недоступности`() {
        assertNull(EtherscanResult.Ok(listOf(1, 2, 3)).unavailableReason())
        assertNull(EtherscanResult.Empty.unavailableReason())
    }

    @Test
    fun `orEmpty возвращает данные только для Ok`() {
        val ok: EtherscanResult<List<Int>> = EtherscanResult.Ok(listOf(1, 2))
        val empty: EtherscanResult<List<Int>> = EtherscanResult.Empty
        val notConfigured: EtherscanResult<List<Int>> = EtherscanResult.NotConfigured
        val notCovered: EtherscanResult<List<Int>> = EtherscanResult.ChainNotCovered(10)

        assertEquals(listOf(1, 2), ok.orEmpty())
        assertEquals(emptyList<Int>(), empty.orEmpty())
        assertEquals(emptyList<Int>(), notConfigured.orEmpty())
        assertEquals(emptyList<Int>(), notCovered.orEmpty())
    }

    @Test
    fun `бесплатный тариф покрывает проверенные сети`() {
        assertTrue(EtherscanClient.isFreeTierChain(1))
        assertTrue(EtherscanClient.isFreeTierChain(42161))
        assertTrue(EtherscanClient.isFreeTierChain(137))
        assertTrue(EtherscanClient.isFreeTierChain(84532))
    }

    @Test
    fun `бесплатный тариф не покрывает Base Optimism и BSC`() {
        assertTrue(!EtherscanClient.isFreeTierChain(8453))
        assertTrue(!EtherscanClient.isFreeTierChain(10))
        assertTrue(!EtherscanClient.isFreeTierChain(56))
    }
}
