@file:Suppress("MagicNumber")

package wex.wallet.data.portfolio

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import wex.wallet.core.network.EtherscanClient
import wex.wallet.core.network.EtherscanResult
import wex.wallet.core.network.unavailableReason

/**
 * Результат обнаружения токенов в одной сети.
 *
 * [unavailableReason] не `null`, когда список пуст не потому, что токенов нет,
 * а потому, что источник недоступен. Эту причину обязательно показывают в UI —
 * иначе «не смогли посмотреть» выглядит как «ничего нет».
 */
data class TokenDiscoveryResult(
    val chainId: Long,
    val tokens: List<TokenBalance>,
    val unavailableReason: String? = null,
    val hiddenSpamCount: Int = 0,
) {
    val isAvailable: Boolean get() = unavailableReason == null
}

/**
 * Находит ERC-20 токены кошелька: контракты берутся из истории переводов
 * Etherscan, актуальный баланс — прямым вызовом balanceOf по RPC.
 */
class TokenDiscovery(
    private val etherscan: EtherscanClient,
    private val balanceRepository: EvmBalanceRepository,
) {

    @Suppress("ReturnCount")
    suspend fun discover(
        address: String,
        chainId: Long,
        filterSpam: Boolean = true,
    ): TokenDiscoveryResult {
        if (!etherscan.isConfigured) {
            return unavailable(chainId, EtherscanResult.NotConfigured)
        }
        if (!EtherscanClient.isFreeTierChain(chainId)) {
            return unavailable(chainId, EtherscanResult.ChainNotCovered(chainId))
        }

        val response = etherscan.tokenTransfers(address, chainId)
        val transfers = when (response) {
            is EtherscanResult.Ok -> response.data
            EtherscanResult.Empty -> emptyList()
            else -> return unavailable(chainId, response)
        }

        val contracts = transfers
            .map { it.contractAddress.lowercase() }
            .distinct()
            .take(MAX_TOKENS_PER_CHAIN)
        val metadata = transfers.associateBy { it.contractAddress.lowercase() }

        val balances = coroutineScope {
            contracts.map { contract ->
                async {
                    runCatching {
                        val meta = metadata[contract]
                        TokenBalance(
                            contractAddress = contract,
                            symbol = meta?.tokenSymbol ?: "?",
                            name = meta?.tokenName ?: "Unknown",
                            decimals = meta?.tokenDecimals ?: 18,
                            balance = balanceRepository.getTokenBalance(address, contract),
                        )
                    }.getOrNull()
                }
            }.mapNotNull { it.await() }
        }

        val kept = if (filterSpam) SpamFilter.filter(balances) else balances
        return TokenDiscoveryResult(
            chainId = chainId,
            tokens = kept,
            hiddenSpamCount = balances.size - kept.size,
        )
    }

    /** Совместимость с прежним вызовом: только список, без причины отказа. */
    suspend fun discoverTokens(
        address: String,
        chainId: Long,
        filterSpam: Boolean = true,
    ): List<TokenBalance> = discover(address, chainId, filterSpam).tokens

    private fun unavailable(chainId: Long, result: EtherscanResult<*>) = TokenDiscoveryResult(
        chainId = chainId,
        tokens = emptyList(),
        unavailableReason = result.unavailableReason(),
    )

    private companion object {
        const val MAX_TOKENS_PER_CHAIN = 30
    }
}
