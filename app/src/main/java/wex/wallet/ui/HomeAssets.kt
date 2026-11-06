@file:Suppress("MagicNumber", "MatchingDeclarationName")

package wex.wallet.ui

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import wex.wallet.chain.evm.EvmNetwork
import wex.wallet.core.network.EtherscanClient
import wex.wallet.core.network.EvmRpcRegistry
import wex.wallet.data.portfolio.EvmBalanceRepository
import wex.wallet.data.portfolio.PriceRepository
import wex.wallet.data.portfolio.TokenBalance
import wex.wallet.data.portfolio.TokenDiscovery
import wex.wallet.feature.home.AssetItem
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal data class BalanceResult(
    val assets: List<AssetItem>,
    val ethBalanceWei: BigInteger,
    val totalUsd: String,
)

/**
 * Балансы монет по всем EVM-сетям плюс цены в долларах.
 *
 * Токены сюда не попадают: они догружаются отдельно [loadTokens], потому что
 * зависят от Etherscan, который покрывает не все сети бесплатным ключом.
 */
internal suspend fun loadEvmBalances(
    assets: List<AssetItem>,
    evmAddress: String,
    registry: EvmRpcRegistry,
    priceRepository: PriceRepository,
    nowMillis: Long,
): BalanceResult = coroutineScope {
    val evmNetworks = EvmNetwork.MAINNETS
    val deferred = evmNetworks.map { network ->
        async {
            network.chainId to runCatching {
                registry.clientForChain(network.chainId).getBalance(evmAddress)
            }.getOrNull()
        }
    }
    val balances = deferred.awaitAll().toMap()
    val ethBalanceWei = balances[EvmNetwork.ETHEREUM.chainId] ?: BigInteger.ZERO

    val coinIds = evmNetworks.mapNotNull { PriceRepository.coinIdForSymbol(it.symbol) }.distinct()
    val prices = runCatching { priceRepository.getPrices(coinIds, nowMillis) }
        .getOrDefault(emptyMap())

    var totalUsd = BigDecimal.ZERO
    val updated = assets.map { asset ->
        // Строки токенов приходят уже посчитанными — не трогаем их балансом монеты.
        if (asset.contractAddress != null) return@map asset

        // Сеть не ответила: строка перестаёт грузиться, но и нулём баланс не объявляем —
        // «0 ETH» было бы утверждением о деньгах, которого мы не знаем.
        val balanceWei = balances[asset.chainId] ?: return@map asset.copy(isLoading = false)

        val amount = BigDecimal(balanceWei).divide(BigDecimal.TEN.pow(asset.decimals))
        val usd = PriceRepository.coinIdForSymbol(asset.symbol)?.let { prices[it] }?.multiply(amount)
        if (usd != null) totalUsd = totalUsd.add(usd)

        asset.copy(
            balance = formatAmount(balanceWei, asset.decimals, asset.symbol),
            isLoading = false,
            usdValue = usd?.let(::formatUsd).orEmpty(),
            amountValue = amount,
            fiatValue = usd,
        )
    }
    BalanceResult(updated, ethBalanceWei, formatUsd(totalUsd))
}

/**
 * ERC-20 токены по сетям, где бесплатный ключ Etherscan действительно работает.
 * По остальным сетям обнаружение молча пропускается — балансы монет от этого не страдают.
 */
internal suspend fun loadTokens(
    evmAddress: String,
    registry: EvmRpcRegistry,
    etherscan: EtherscanClient,
): List<AssetItem> = coroutineScope {
    if (!etherscan.isConfigured) return@coroutineScope emptyList()

    EvmNetwork.MAINNETS
        .filter { EtherscanClient.isFreeTierChain(it.chainId) }
        .map { network ->
            async {
                runCatching {
                    val repository = EvmBalanceRepository(registry.clientForChain(network.chainId))
                    TokenDiscovery(etherscan, repository)
                        .discover(evmAddress, network.chainId)
                        .tokens
                        .map { it.toAssetItem(network, evmAddress) }
                }.getOrDefault(emptyList())
            }
        }
        .awaitAll()
        .flatten()
}

private fun TokenBalance.toAssetItem(network: EvmNetwork, owner: String) = AssetItem(
    symbol = symbol,
    name = name,
    balance = formatAmount(balance, decimals, symbol),
    chainLabel = network.name,
    address = owner,
    chainId = network.chainId,
    decimals = decimals,
    isLoading = false,
    contractAddress = contractAddress,
    amountValue = BigDecimal(balance).movePointLeft(decimals),
)

internal fun formatUsd(value: BigDecimal): String =
    "$" + value.setScale(2, RoundingMode.HALF_UP).toPlainString()

/**
 * Знаки берутся у самого актива: у USDC их 6, и деление на 10^18
 * показало бы ноль вместо реального баланса.
 */
internal fun formatAmount(raw: BigInteger, decimals: Int, symbol: String): String {
    if (raw == BigInteger.ZERO) return "0 $symbol"
    val scale = decimals.coerceAtMost(8)
    val amount = BigDecimal(raw).divide(BigDecimal.TEN.pow(decimals), scale, RoundingMode.HALF_UP)
    return "${amount.stripTrailingZeros().toPlainString()} $symbol"
}

internal fun buildAssetList(addresses: Map<String, String>): List<AssetItem> {
    val evmAddress = addresses["Ethereum"] ?: ""
    val evmAssets = EvmNetwork.MAINNETS.map { network ->
        AssetItem(
            symbol = network.symbol,
            name = network.name,
            balance = "",
            chainLabel = network.name,
            address = evmAddress,
            chainId = network.chainId,
            decimals = network.decimals,
            isLoading = true,
        )
    }
    // Балансы Bitcoin, Solana и TON ещё не реализованы. Ноль здесь — не утверждение
    // о деньгах, а признак «сеть подключена, чтения баланса пока нет»: скелетон
    // обещал бы значение, которое в этой сборке не придёт никогда.
    val nonEvmAssets = addresses.filterKeys { !it.contains("Ethereum") }.map { (chain, addr) ->
        AssetItem(
            symbol = chainSymbol(chain),
            name = chain,
            balance = "",
            chainLabel = chain,
            address = addr,
            amountValue = BigDecimal.ZERO,
        )
    }
    return evmAssets + nonEvmAssets
}

internal fun chainSymbol(chain: String): String = when {
    chain.contains("Bitcoin") -> "BTC"
    chain.contains("Solana") -> "SOL"
    chain.contains("TON") -> "TON"
    else -> "?"
}
