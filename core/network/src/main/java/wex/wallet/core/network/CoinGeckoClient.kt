@file:Suppress("MagicNumber")

package wex.wallet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.Closeable

class CoinGeckoClient(private val apiKey: String = "") : Closeable {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@CoinGeckoClient.json) }
    }

    suspend fun getPrices(
        coinIds: List<String>,
        vsCurrencies: List<String> = listOf("usd"),
    ): Map<String, Map<String, Double>> {
        if (coinIds.isEmpty()) return emptyMap()

        val response: JsonObject = httpClient.get("https://api.coingecko.com/api/v3/simple/price") {
            parameter("ids", coinIds.joinToString(","))
            parameter("vs_currencies", vsCurrencies.joinToString(","))
            if (apiKey.isNotBlank()) {
                header("x-cg-demo-api-key", apiKey)
            }
        }.body()

        return response.mapValues { (_, value) ->
            value.jsonObject.mapValues { (_, price) ->
                price.jsonPrimitive.double
            }
        }
    }

    suspend fun getPrice(coinId: String, vsCurrency: String = "usd"): Double? {
        val prices = getPrices(listOf(coinId), listOf(vsCurrency))
        return prices[coinId]?.get(vsCurrency)
    }

    override fun close() {
        httpClient.close()
    }

    companion object {
        const val ETHEREUM = "ethereum"
        const val BITCOIN = "bitcoin"
        const val SOLANA = "solana"
        const val THE_OPEN_NETWORK = "the-open-network"
        const val MATIC_NETWORK = "matic-network"
        const val BINANCECOIN = "binancecoin"
    }
}
