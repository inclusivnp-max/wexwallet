@file:Suppress("MagicNumber")

package wex.wallet.feature.activity

import wex.wallet.data.activity.ActivityDirection
import wex.wallet.data.activity.ActivityItem
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Форматирование строк ленты. Чистые функции — покрыты тестами. */
object ActivityFormat {

    /**
     * Тикер сети по её идентификатору.
     *
     * Нативный перевод и вызов контракта приходят из данных без символа: он
     * принадлежит сети, а не строке. Незнакомая сеть остаётся без тикера —
     * придуманный тикер рядом с суммой хуже его отсутствия.
     */
    private val NativeSymbols: Map<Long, String> = mapOf(
        1L to "ETH",
        10L to "ETH",
        56L to "BNB",
        137L to "POL",
        8453L to "ETH",
        42161L to "ETH",
        84532L to "ETH",
    )

    /** Символ для бейджа и суммы: свой у токена, сетевой у нативного перевода. */
    fun displaySymbol(item: ActivityItem): String =
        item.symbol.ifBlank { NativeSymbols[item.chainId].orEmpty() }

    fun shortenAddress(address: String): String {
        if (address.length <= 12) return address
        return "${address.take(6)}...${address.takeLast(4)}"
    }

    fun shortenHash(hash: String): String {
        if (hash.length <= 14) return hash
        return "${hash.take(8)}...${hash.takeLast(4)}"
    }

    /** Сумма со знаком: исходящая — со минусом, входящая — с плюсом. */
    fun signedAmount(item: ActivityItem): String {
        val amount = item.displayAmount.stripTrailingZeros().toPlainString()
        val symbol = item.symbol.ifBlank { "" }
        val body = if (symbol.isBlank()) amount else "$amount $symbol"
        return when (item.direction) {
            ActivityDirection.OUTGOING -> "-$body"
            ActivityDirection.INCOMING -> "+$body"
            ActivityDirection.SELF -> body
        }
    }

    /** «5 мин назад», «3 ч назад», «12 дн назад». */
    fun relativeTime(timestampSeconds: Long, nowSeconds: Long): String {
        val diff = (nowSeconds - timestampSeconds).coerceAtLeast(0)
        val minutes = TimeUnit.SECONDS.toMinutes(diff)
        val hours = TimeUnit.SECONDS.toHours(diff)
        val days = TimeUnit.SECONDS.toDays(diff)
        return when {
            diff < 60 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            hours < 24 -> "$hours ч назад"
            days < 30 -> "$days дн назад"
            else -> String.format(Locale.US, "%d мес назад", days / 30)
        }
    }

    fun directionLabel(item: ActivityItem): String = when (item.direction) {
        ActivityDirection.OUTGOING -> "Отправлено"
        ActivityDirection.INCOMING -> "Получено"
        ActivityDirection.SELF -> "Себе"
    }
}
