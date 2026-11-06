package wex.wallet.data.portfolio

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class EvmBalance(
    val address: String,
    val nativeBalanceWei: BigInteger,
    val tokens: List<TokenBalance> = emptyList(),
) {
    val nativeBalanceEth: BigDecimal
        get() = BigDecimal(nativeBalanceWei).divide(WEI_PER_ETH, DISPLAY_SCALE, RoundingMode.HALF_UP)
}

data class TokenBalance(
    val contractAddress: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val balance: BigInteger,
) {
    val displayBalance: BigDecimal
        get() = if (decimals > 0) {
            BigDecimal(balance).divide(
                BigDecimal.TEN.pow(decimals),
                decimals.coerceAtMost(DISPLAY_SCALE),
                RoundingMode.HALF_UP,
            )
        } else {
            BigDecimal(balance)
        }
}

@Suppress("MagicNumber")
private val WEI_PER_ETH = BigDecimal.TEN.pow(18)
private const val DISPLAY_SCALE = 8
