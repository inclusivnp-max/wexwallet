@file:Suppress("MagicNumber")

package wex.wallet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.Closeable

/** Непотраченный выход. [value] в сатоши. */
data class Utxo(
    val txid: String,
    val vout: Int,
    val value: Long,
    val confirmed: Boolean,
    val blockHeight: Long = 0,
)

/**
 * Рекомендованные комиссии в сатоши за виртуальный байт.
 *
 * Именно sat/vB, а не sat/byte: для segwit это разные величины, и подстановка
 * одной вместо другой завышает комиссию примерно вчетверо.
 */
data class FeeRates(
    val fastest: Int,
    val halfHour: Int,
    val hour: Int,
    val economy: Int,
    val minimum: Int,
) {
    companion object {
        /** Разумный запасной вариант, когда сеть недоступна. */
        val Fallback = FeeRates(fastest = 10, halfHour = 6, hour = 3, economy = 2, minimum = 1)
    }
}

/**
 * Клиент Esplora (blockstream.info / mempool.space).
 *
 * [baseUrl] задаёт и сеть: у signet на mempool.space отдельный префикс пути,
 * поэтому сеть не хранится отдельным флагом — её несёт сам адрес.
 */
class EsploraClient(private val baseUrl: String) : Closeable {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@EsploraClient.json) }
    }

    private val root = baseUrl.trimEnd('/')

    suspend fun utxos(address: String): List<Utxo> {
        val response: JsonArray = httpClient.get("$root/address/$address/utxo").body()
        return response.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val status = obj["status"]?.jsonObject
                Utxo(
                    txid = obj.str("txid"),
                    vout = obj.num("vout").toInt(),
                    value = obj.num("value"),
                    confirmed = status?.get("confirmed")?.jsonPrimitive?.content == "true",
                    blockHeight = status?.num("block_height") ?: 0,
                )
            }.getOrNull()
        }
    }

    /** Баланс адреса в сатоши — сумма непотраченных выходов. */
    suspend fun balance(address: String): Long = utxos(address).sumOf { it.value }

    suspend fun feeRates(): FeeRates {
        val response: JsonObject = httpClient.get("$root/v1/fees/recommended").body()
        return FeeRates(
            fastest = response.num("fastestFee").toInt(),
            halfHour = response.num("halfHourFee").toInt(),
            hour = response.num("hourFee").toInt(),
            economy = response.num("economyFee").toInt(),
            minimum = response.num("minimumFee").toInt(),
        )
    }

    suspend fun blockHeight(): Long =
        httpClient.get("$root/blocks/tip/height").bodyAsText().trim().toLongOrNull() ?: 0

    /** Сырая транзакция в hex. Возвращает txid либо кидает [EsploraException]. */
    suspend fun broadcast(rawTxHex: String): String {
        val response: HttpResponse = httpClient.post("$root/tx") {
            contentType(ContentType.Text.Plain)
            setBody(rawTxHex)
        }
        val body = response.bodyAsText().trim()
        if (response.status != HttpStatusCode.OK) {
            throw EsploraException(body.ifBlank { "Broadcast failed: ${response.status}" })
        }
        return body
    }

    /** `null`, если транзакция ещё не видна узлу. */
    suspend fun isConfirmed(txid: String): Boolean? = runCatching {
        val response: JsonObject = httpClient.get("$root/tx/$txid/status").body()
        response["confirmed"]?.jsonPrimitive?.content == "true"
    }.getOrNull()

    override fun close() {
        httpClient.close()
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() } ?: ""

    private fun JsonObject.num(key: String): Long =
        this[key]?.let { runCatching { it.jsonPrimitive.content.toLong() }.getOrNull() } ?: 0

    companion object {
        const val MAINNET = "https://mempool.space/api"
        const val SIGNET = "https://mempool.space/signet/api"
        const val TESTNET = "https://mempool.space/testnet/api"
        const val BLOCKSTREAM_MAINNET = "https://blockstream.info/api"
    }
}

class EsploraException(override val message: String) : Exception(message)
