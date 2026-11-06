package wex.wallet.feature.home

import java.math.BigDecimal

/**
 * Актив в списке кошелька.
 *
 * Полей под сумму два комплекта, и это осознанно. [amountValue] и [fiatValue] —
 * то, что показывает строка списка: она форматирует числа сама, одинаково со всеми
 * остальными экранами. [balance] и [usdValue] остались от прежней вёрстки, где
 * строку собирал вызывающий код, и продолжают заполняться им.
 *
 * Числовые поля со значением по умолчанию `null`, поэтому источник данных
 * переезжает на них по одному месту за раз, ничего не ломая. Пока значение
 * не пришло числом, оно разбирается обратно из строки — см. [amountOrNull].
 */
data class AssetItem(
    val symbol: String,
    val name: String,
    val balance: String,
    val chainLabel: String,
    val address: String,
    val chainId: Long = 0,
    val decimals: Int = 18,
    val isLoading: Boolean = false,
    val contractAddress: String? = null,
    val usdValue: String = "",
    val amountValue: BigDecimal? = null,
    val fiatValue: BigDecimal? = null,
)

/**
 * Баланс числом: сначала готовое значение, потом разбор строки.
 *
 * Разбор — временный мост на время переезда, а не способ хранения. Число,
 * собранное в строку и разобранное назад, уже потеряло точность сверх нужной,
 * поэтому источникам данных правильнее заполнять [AssetItem.amountValue].
 */
internal fun AssetItem.amountOrNull(): BigDecimal? =
    amountValue ?: parseDecimal(balance.trim().removeSuffix(symbol))

/** Стоимость в фиате числом. Курса может не быть вовсе — тогда `null`. */
internal fun AssetItem.fiatOrNull(): BigDecimal? =
    fiatValue ?: parseDecimal(usdValue)

/** Неразрывный пробел из разрядов: `Char.isWhitespace` его не ловит. */
private const val NoBreakSpace = '\u00A0'

/**
 * Разбор суммы, собранной для показа. Всё, что не разобралось, — `null`:
 * подставить ноль значило бы показать «средств нет» там, где они есть.
 */
internal fun parseDecimal(formatted: String): BigDecimal? {
    val digits = formatted
        .filterNot { it.isWhitespace() || it == NoBreakSpace || it == '$' }
        .replace(',', '.')
    return digits.toBigDecimalOrNull()
}
