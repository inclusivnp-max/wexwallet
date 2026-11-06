@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class GasPrecheckResult(
    val hasEnoughGas: Boolean,
    val shortfall: BigInteger = BigInteger.ZERO,
    val message: String = "",
) {
    companion object {
        private val WEI_PER_ETH = BigDecimal.TEN.pow(18)

        fun sufficient(): GasPrecheckResult = GasPrecheckResult(hasEnoughGas = true)

        fun insufficient(
            shortfall: BigInteger,
            symbol: String,
        ): GasPrecheckResult {
            val shortfallEth = BigDecimal(shortfall)
                .divide(WEI_PER_ETH, 8, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
            return GasPrecheckResult(
                hasEnoughGas = false,
                shortfall = shortfall,
                message = "Need $shortfallEth more $symbol for gas",
            )
        }
    }
}

object GasPrecheck {
    fun check(
        balanceWei: BigInteger,
        sendAmountWei: BigInteger,
        estimatedGasWei: BigInteger,
        symbol: String = "ETH",
    ): GasPrecheckResult {
        val totalNeeded = sendAmountWei + estimatedGasWei
        return if (balanceWei >= totalNeeded) {
            GasPrecheckResult.sufficient()
        } else {
            GasPrecheckResult.insufficient(totalNeeded - balanceWei, symbol)
        }
    }

    fun maxSendableWei(
        balanceWei: BigInteger,
        estimatedGasWei: BigInteger,
    ): BigInteger {
        val max = balanceWei - estimatedGasWei
        return if (max > BigInteger.ZERO) max else BigInteger.ZERO
    }
}
