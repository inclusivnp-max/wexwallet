@file:Suppress("MagicNumber")

package wex.wallet.data.portfolio

import wex.wallet.core.network.CoinGeckoClient
import java.math.BigDecimal

/**
 * Цены с кэшем: CoinGecko на бесплатном тарифе жёстко лимитирует запросы,
 * поэтому повторный запрос в течение TTL берётся из памяти.
 */
class PriceRepository(
    private val coinGecko: CoinGeckoClient,
    private val ttlMillis: Long = 60_000,
) {

    private val cache = mutableMapOf<String, CachedPrice>()

    suspend fun getPrices(coinIds: List<String>, nowMillis: Long): Map<String, BigDecimal> {
        val fresh = coinIds.filter { id ->
            val cached = cache[id]
            cached == null || nowMillis - cached.timestamp > ttlMillis
        }

        if (fresh.isNotEmpty()) {
            runCatching { coinGecko.getPrices(fresh) }.getOrNull()?.forEach { (id, prices) ->
                prices["usd"]?.let { usd ->
                    cache[id] = CachedPrice(BigDecimal.valueOf(usd), nowMillis)
                }
            }
        }

        return coinIds.mapNotNull { id ->
            cache[id]?.let { id to it.usd }
        }.toMap()
    }

    fun cachedPrice(coinId: String): BigDecimal? = cache[coinId]?.usd

    private data class CachedPrice(val usd: BigDecimal, val timestamp: Long)

    companion object {
        fun coinIdForSymbol(symbol: String): String? = when (symbol.uppercase()) {
            "ETH" -> CoinGeckoClient.ETHEREUM
            "BTC" -> CoinGeckoClient.BITCOIN
            "SOL" -> CoinGeckoClient.SOLANA
            "TON" -> CoinGeckoClient.THE_OPEN_NETWORK
            "POL", "MATIC" -> CoinGeckoClient.MATIC_NETWORK
            "BNB" -> CoinGeckoClient.BINANCECOIN
            else -> null
        }
    }
}
