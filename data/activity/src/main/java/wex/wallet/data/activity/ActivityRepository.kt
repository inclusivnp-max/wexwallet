@file:Suppress("MagicNumber")

package wex.wallet.data.activity

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import wex.wallet.core.network.EtherscanClient
import wex.wallet.core.network.EtherscanResult
import wex.wallet.core.network.unavailableReason

/**
 * Загружает ленту активности по нескольким сетям.
 *
 * Ключ Etherscan сюда не читается: [EtherscanClient] приходит извне, потому что
 * BuildConfig доступен только в :app.
 */
class ActivityRepository(
    private val etherscan: EtherscanClient,
    private val chains: List<ActivityChain> = ActivityChains.Mainnets,
    maxConcurrentRequests: Int = 2,
    minRequestIntervalMillis: Long = 250,
) {

    private val throttle = RequestThrottle(maxConcurrentRequests, minRequestIntervalMillis)

    suspend fun load(
        address: String,
        offsetPerChain: Int = 50,
        filterSpam: Boolean = true,
    ): ActivityFeed = coroutineScope {
        val perChain = chains.map { chain ->
            async { loadChain(address, chain, offsetPerChain, filterSpam) }
        }.awaitAll()

        ActivityFeed(
            items = ActivityMerger.sortNewestFirst(perChain.flatMap { it.items }),
            chains = perChain.map { it.status },
            hiddenSpamCount = perChain.sumOf { it.hiddenSpamCount },
        )
    }

    private data class ChainLoad(
        val status: ChainActivityStatus,
        val items: List<ActivityItem> = emptyList(),
        val hiddenSpamCount: Int = 0,
    )

    @Suppress("ReturnCount")
    private suspend fun loadChain(
        address: String,
        chain: ActivityChain,
        offset: Int,
        filterSpam: Boolean,
    ): ChainLoad {
        // Сеть вне бесплатного тарифа: не тратим запрос, сразу объясняем причину.
        if (!EtherscanClient.isFreeTierChain(chain.chainId)) {
            return ChainLoad(chain.unavailable(EtherscanResult.ChainNotCovered(chain.chainId)))
        }

        val txResult = throttle.withPermit { etherscan.transactions(address, chain.chainId, offset = offset) }
        val tokenResult = throttle.withPermit { etherscan.tokenTransfers(address, chain.chainId, offset = offset) }

        val transactions = txResult.itemsOrNull()
        val transfers = tokenResult.itemsOrNull()

        // Обе ручки отказали — данных по сети нет, и мы знаем почему.
        if (transactions == null && transfers == null) {
            return ChainLoad(chain.unavailable(txResult))
        }

        val merged = ActivityMerger.mergeChain(
            walletAddress = address,
            chain = chain,
            transactions = transactions.orEmpty(),
            tokenTransfers = transfers.orEmpty(),
            filterSpam = filterSpam,
        )
        return ChainLoad(
            status = ChainActivityStatus(chain.chainId, chain.name),
            items = merged.items,
            hiddenSpamCount = merged.hiddenSpamCount,
        )
    }

    private fun ActivityChain.unavailable(result: EtherscanResult<*>) = ChainActivityStatus(
        chainId = chainId,
        networkName = name,
        unavailableReason = result.unavailableReason() ?: "История недоступна",
    )
}

/** Успешный список, пустой список для [EtherscanResult.Empty], `null` при отказе. */
private fun <T> EtherscanResult<List<T>>.itemsOrNull(): List<T>? = when (this) {
    is EtherscanResult.Ok -> data
    EtherscanResult.Empty -> emptyList()
    else -> null
}

private fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()
