@file:Suppress("MagicNumber")

package wex.wallet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.Closeable
import java.math.BigInteger

data class TokenTransfer(
    val contractAddress: String,
    val tokenName: String,
    val tokenSymbol: String,
    val tokenDecimals: Int,
    val from: String,
    val to: String,
    val value: BigInteger,
    val txHash: String,
    val timestamp: Long,
    val blockNumber: Long,
)

data class NormalTransaction(
    val txHash: String,
    val from: String,
    val to: String,
    val value: BigInteger,
    val gasUsed: BigInteger,
    val gasPrice: BigInteger,
    val timestamp: Long,
    val blockNumber: Long,
    val isError: Boolean,
    val methodId: String,
    val functionName: String = "",
    val input: String = "",
)

class EtherscanClient(private val apiKey: String) : Closeable {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@EtherscanClient.json) }
    }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun tokenTransfers(
        address: String,
        chainId: Long,
        page: Int = 1,
        offset: Int = 100,
    ): EtherscanResult<List<TokenTransfer>> =
        request("tokentx", address, chainId, page, offset) { obj ->
            TokenTransfer(
                contractAddress = obj.str("contractAddress"),
                tokenName = obj.str("tokenName"),
                tokenSymbol = obj.str("tokenSymbol"),
                tokenDecimals = obj.str("tokenDecimal").toIntOrNull() ?: 18,
                from = obj.str("from"),
                to = obj.str("to"),
                value = BigInteger(obj.str("value")),
                txHash = obj.str("hash"),
                timestamp = obj.str("timeStamp").toLongOrNull() ?: 0,
                blockNumber = obj.str("blockNumber").toLongOrNull() ?: 0,
            )
        }

    suspend fun transactions(
        address: String,
        chainId: Long,
        page: Int = 1,
        offset: Int = 50,
    ): EtherscanResult<List<NormalTransaction>> =
        request("txlist", address, chainId, page, offset) { obj ->
            NormalTransaction(
                txHash = obj.str("hash"),
                from = obj.str("from"),
                to = obj.str("to"),
                value = BigInteger(obj.str("value")),
                gasUsed = BigInteger(obj.str("gasUsed").ifBlank { "0" }),
                gasPrice = BigInteger(obj.str("gasPrice").ifBlank { "0" }),
                timestamp = obj.str("timeStamp").toLongOrNull() ?: 0,
                blockNumber = obj.str("blockNumber").toLongOrNull() ?: 0,
                isError = obj.str("isError") == "1",
                methodId = obj.str("methodId"),
                functionName = obj.str("functionName"),
                input = obj.str("input"),
            )
        }

    suspend fun getTokenTransfers(
        address: String,
        chainId: Long,
        page: Int = 1,
        offset: Int = 100,
    ): List<TokenTransfer> = tokenTransfers(address, chainId, page, offset).orEmpty()

    suspend fun getTransactions(
        address: String,
        chainId: Long,
        page: Int = 1,
        offset: Int = 50,
    ): List<NormalTransaction> = transactions(address, chainId, page, offset).orEmpty()

    @Suppress("ReturnCount")
    private suspend fun <T> request(
        action: String,
        address: String,
        chainId: Long,
        page: Int,
        offset: Int,
        parse: (JsonObject) -> T,
    ): EtherscanResult<List<T>> {
        if (!isConfigured) return EtherscanResult.NotConfigured

        val response = runCatching {
            httpClient.get(BASE_URL) {
                parameter("chainid", chainId)
                parameter("module", "account")
                parameter("action", action)
                parameter("address", address)
                parameter("page", page)
                parameter("offset", offset)
                parameter("sort", "desc")
                parameter("apikey", apiKey)
            }.body<JsonObject>()
        }.getOrElse { return EtherscanResult.Failure(it.message ?: "Network error") }

        classifyEtherscanFailure(response, chainId)?.let { return it }

        val items = response["result"]?.jsonArray
            ?.mapNotNull { runCatching { parse(it.jsonObject) }.getOrNull() }
            ?: emptyList()

        return if (items.isEmpty()) EtherscanResult.Empty else EtherscanResult.Ok(items)
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() } ?: ""

    override fun close() {
        httpClient.close()
    }

    companion object {
        private const val BASE_URL = "https://api.etherscan.io/v2/api"

        /**
         * Сети, подтверждённо работающие на бесплатном тарифе (проверено запросами).
         * Используется, чтобы не тратить запрос впустую и сразу объяснить причину.
         */
        val FREE_TIER_CHAINS = setOf(1L, 42161L, 137L, 84532L, 11155111L)

        fun isFreeTierChain(chainId: Long): Boolean = chainId in FREE_TIER_CHAINS
    }
}

/**
 * `null`, если ответ успешный. Иначе — конкретная причина отказа.
 *
 * Вынесено на верхний уровень, чтобы классификацию можно было проверить тестами
 * без поднятия HTTP-клиента.
 */
internal fun classifyEtherscanFailure(
    response: JsonObject,
    chainId: Long,
): EtherscanResult<Nothing>? {
    if (response["status"]?.jsonPrimitive?.content == "1") return null

    val detail = response["result"]?.let {
        runCatching { it.jsonPrimitive.content }.getOrNull()
    }.orEmpty()
    val message = response["message"]?.jsonPrimitive?.content.orEmpty()
    val haystack = "$detail $message".lowercase()

    return when {
        haystack.contains("no transactions found") ||
            haystack.contains("no records found") -> EtherscanResult.Empty

        haystack.contains("free api access is not supported") ||
            haystack.contains("upgrade your api plan") -> EtherscanResult.ChainNotCovered(chainId)

        haystack.contains("rate limit") ||
            haystack.contains("max calls per sec") -> EtherscanResult.RateLimited

        haystack.contains("invalid api key") ->
            EtherscanResult.Failure("Неверный ключ Etherscan")

        else -> EtherscanResult.Failure(detail.ifBlank { message.ifBlank { "Etherscan error" } })
    }
}
