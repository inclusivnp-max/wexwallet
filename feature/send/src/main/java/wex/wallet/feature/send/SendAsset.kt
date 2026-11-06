package wex.wallet.feature.send

import java.math.BigInteger

data class SendAsset(
    val symbol: String,
    val name: String,
    val decimals: Int = 18,
    val contractAddress: String? = null,
    val balance: BigInteger = BigInteger.ZERO,
    val chainId: Long = 1,
) {
    val isNative: Boolean get() = contractAddress == null
}
