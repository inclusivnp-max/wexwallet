package wex.wallet.data.activity

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

enum class ActivityDirection { INCOMING, OUTGOING, SELF }

enum class ActivityStatus { SUCCESS, FAILED }

enum class ActivityKind { NATIVE_TRANSFER, TOKEN_TRANSFER, CONTRACT_CALL }

/**
 * Строка ленты активности.
 *
 * Все поля уже пригодны для отрисовки: имя сети и ссылка на эксплорер приходят
 * готовыми строками, чтобы слой feature не тянул зависимость на :chain:evm.
 */
data class ActivityItem(
    val id: String,
    val txHash: String,
    val chainId: Long,
    val networkName: String,
    val explorerUrl: String,
    val timestampSeconds: Long,
    val blockNumber: Long,
    val kind: ActivityKind,
    val direction: ActivityDirection,
    val status: ActivityStatus,
    val symbol: String,
    val decimals: Int,
    val amount: BigInteger,
    val counterparty: String,
    val contractAddress: String? = null,
    val methodLabel: String = "",
    val methodId: String = "",
    val isMethodDecoded: Boolean = true,
) {
    val displayAmount: BigDecimal
        get() = BigDecimal(amount).divide(
            BigDecimal.TEN.pow(decimals),
            decimals.coerceAtMost(DISPLAY_SCALE),
            RoundingMode.HALF_UP,
        )

    private companion object {
        const val DISPLAY_SCALE = 8
    }
}
