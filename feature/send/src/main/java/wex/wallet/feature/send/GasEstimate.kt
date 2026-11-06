package wex.wallet.feature.send

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class GasEstimate(
    val gasLimit: BigInteger,
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger,
) {
    val totalMaxFeeWei: BigInteger get() = gasLimit * maxFeePerGas

    @Suppress("MagicNumber")
    val totalMaxFeeEth: BigDecimal
        get() = BigDecimal(totalMaxFeeWei).divide(
            BigDecimal.TEN.pow(18),
            8,
            RoundingMode.HALF_UP,
        )
}
