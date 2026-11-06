@file:Suppress("MagicNumber")

package wex.wallet.data.activity

import wex.wallet.chain.evm.CalldataDecoder
import wex.wallet.core.network.NormalTransaction
import wex.wallet.core.network.TokenTransfer
import java.math.BigInteger

data class ChainMergeResult(
    val items: List<ActivityItem>,
    val hiddenSpamCount: Int = 0,
)

/**
 * Склейка txlist и tokentx в ленту одной сети. Чистая функция — без сети и корутин,
 * поэтому полностью покрывается юнит-тестами.
 *
 * Правила:
 *  - адреса сравниваются только в нижнем регистре: Etherscan отдаёт lowercase,
 *    а наш деривер — EIP-55 checksum, иначе направление определится неверно;
 *  - каждый TokenTransfer даёт свою строку (обмен = одна исходящая + одна входящая);
 *  - обычная транзакция с тем же хешем строку не дублирует, но передаёт
 *    токен-строкам свой статус и метод;
 *  - обычная транзакция с ненулевым value даёт дополнительную нативную строку,
 *    даже если по этому хешу есть переводы токенов (обмен монеты на токен);
 *  - обычная транзакция без value и без токен-переводов — вызов контракта.
 */
object ActivityMerger {

    fun mergeChain(
        walletAddress: String,
        chain: ActivityChain,
        transactions: List<NormalTransaction>,
        tokenTransfers: List<TokenTransfer>,
        filterSpam: Boolean = true,
    ): ChainMergeResult {
        val wallet = walletAddress.lowercase()
        val byHash = transactions.associateBy { it.txHash.lowercase() }

        val kept = if (filterSpam) tokenTransfers.filterNot(ActivitySpamRules::isSpam) else tokenTransfers
        val hiddenSpamCount = tokenTransfers.size - kept.size

        val tokenItems = kept.mapIndexed { index, transfer ->
            transfer.toItem(wallet, chain, byHash[transfer.txHash.lowercase()], index)
        }

        val hashesWithTokens = kept.map { it.txHash.lowercase() }.toSet()
        val nativeItems = transactions.mapNotNull { tx ->
            tx.toItemOrNull(wallet, chain, hasTokenRows = tx.txHash.lowercase() in hashesWithTokens)
        }

        return ChainMergeResult(
            items = sortNewestFirst(tokenItems + nativeItems),
            hiddenSpamCount = hiddenSpamCount,
        )
    }

    /** Свежие сверху. При равном времени порядок детерминирован по идентификатору. */
    fun sortNewestFirst(items: List<ActivityItem>): List<ActivityItem> =
        items.sortedWith(compareByDescending<ActivityItem> { it.timestampSeconds }.thenBy { it.id })

    fun directionFor(walletAddress: String, from: String, to: String): ActivityDirection {
        val wallet = walletAddress.lowercase()
        val isFrom = from.lowercase() == wallet
        val isTo = to.lowercase() == wallet
        return when {
            isFrom && isTo -> ActivityDirection.SELF
            isFrom -> ActivityDirection.OUTGOING
            else -> ActivityDirection.INCOMING
        }
    }

    private fun TokenTransfer.toItem(
        wallet: String,
        chain: ActivityChain,
        parent: NormalTransaction?,
        index: Int,
    ): ActivityItem {
        val decoded = CalldataDecoder.decode(parent?.input)
        val direction = directionFor(wallet, from, to)
        return ActivityItem(
            id = "${chain.chainId}:${txHash.lowercase()}:t$index",
            txHash = txHash,
            chainId = chain.chainId,
            networkName = chain.name,
            explorerUrl = chain.explorerTxUrl(txHash),
            timestampSeconds = timestamp,
            blockNumber = blockNumber,
            kind = ActivityKind.TOKEN_TRANSFER,
            direction = direction,
            status = if (parent?.isError == true) ActivityStatus.FAILED else ActivityStatus.SUCCESS,
            symbol = tokenSymbol,
            decimals = tokenDecimals,
            amount = value,
            counterparty = if (direction == ActivityDirection.OUTGOING) to.lowercase() else from.lowercase(),
            contractAddress = contractAddress.lowercase(),
            methodLabel = decoded.humanLabel,
            methodId = decoded.methodId,
            isMethodDecoded = decoded.isKnown || decoded.methodId.isEmpty(),
        )
    }

    /** `null`, когда строка была бы дублем уже показанного токен-перевода. */
    private fun NormalTransaction.toItemOrNull(
        wallet: String,
        chain: ActivityChain,
        hasTokenRows: Boolean,
    ): ActivityItem? {
        val hasValue = value > BigInteger.ZERO
        if (hasTokenRows && !hasValue) return null

        val decoded = CalldataDecoder.decode(input)
        val direction = directionFor(wallet, from, to)
        return ActivityItem(
            id = "${chain.chainId}:${txHash.lowercase()}:n",
            txHash = txHash,
            chainId = chain.chainId,
            networkName = chain.name,
            explorerUrl = chain.explorerTxUrl(txHash),
            timestampSeconds = timestamp,
            blockNumber = blockNumber,
            kind = if (hasValue) ActivityKind.NATIVE_TRANSFER else ActivityKind.CONTRACT_CALL,
            direction = direction,
            status = if (isError) ActivityStatus.FAILED else ActivityStatus.SUCCESS,
            symbol = "",
            decimals = 18,
            amount = value,
            counterparty = if (direction == ActivityDirection.OUTGOING) to.lowercase() else from.lowercase(),
            methodLabel = decoded.humanLabel,
            methodId = decoded.methodId,
            isMethodDecoded = decoded.isKnown || decoded.methodId.isEmpty(),
        )
    }
}
