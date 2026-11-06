package wex.wallet.data.activity

import wex.wallet.chain.evm.EvmNetwork
import wex.wallet.core.network.EtherscanClient

/**
 * Сеть в терминах слоя данных. Существует, чтобы [EvmNetwork] не протекал
 * в feature-слой: правило зависимостей — feature → data → chain → core.
 */
data class ActivityChain(
    val chainId: Long,
    val name: String,
    val blockExplorerUrl: String,
) {
    fun explorerTxUrl(txHash: String): String =
        if (blockExplorerUrl.isBlank()) "" else "$blockExplorerUrl/tx/$txHash"
}

object ActivityChains {

    val Mainnets: List<ActivityChain> = EvmNetwork.MAINNETS.map { it.toActivityChain() }

    /** Сети, по которым бесплатный ключ Etherscan реально отдаёт данные. */
    val FreeTier: List<ActivityChain> =
        Mainnets.filter { EtherscanClient.isFreeTierChain(it.chainId) }

    fun byChainId(chainId: Long): ActivityChain? = Mainnets.find { it.chainId == chainId }

    private fun EvmNetwork.toActivityChain() = ActivityChain(
        chainId = chainId,
        name = name,
        blockExplorerUrl = blockExplorerUrl,
    )
}
