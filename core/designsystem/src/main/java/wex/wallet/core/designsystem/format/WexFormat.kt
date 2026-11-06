@file:Suppress("MagicNumber", "TooManyFunctions")

package wex.wallet.core.designsystem.format

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Форматирование денег и адресов.
 *
 * Правила вынесены из разметки, потому что одна и та же сумма показывается
 * на трёх экранах, и расхождение в округлении между ними читается как ошибка
 * приложения, а не как разная детализация.
 */
object WexFormat {

    /**
     * Разделитель разрядов.
     *
     * Русская типографика требует узкий неразрывный пробел U+202F, но ни Manrope,
     * ни IBM Plex Mono его не содержат: символ ушёл бы в подменный шрифт и сломал
     * бы выравнивание табличных цифр. Поэтому обычный неразрывный U+00A0.
     */
    const val GROUP_SEPARATOR = ' '

    const val DECIMAL_SEPARATOR = ','

    /** Порог, ниже которого сумма показывается как «меньше чем», а не нулём. */
    private val TINY_THRESHOLD: BigDecimal = BigDecimal("0.000001")

    /**
     * Знаки после запятой в списке. В подтверждении сумма показывается полностью:
     * подписывают точное значение, а не округлённое.
     */
    fun listDecimals(symbol: String): Int = when (symbol.uppercase()) {
        "BTC" -> 8
        "ETH", "SOL", "TON", "BNB", "POL", "MATIC" -> 6
        "USDC", "USDT", "DAI" -> 2
        else -> 6
    }

    /** Сумма в фиате: всегда два знака. Минус ставится перед символом валюты. */
    fun fiat(value: BigDecimal, currency: String = "$"): String {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val body = currency + group(rounded.abs().toPlainString())
        return if (rounded.signum() < 0) "-$body" else body
    }

    /**
     * Сумма в монете. Незначащие нули убираются, но ноль остаётся нулём,
     * а не превращается в пустоту.
     */
    @Suppress("ReturnCount")
    fun amount(value: BigDecimal, symbol: String, decimals: Int = listDecimals(symbol)): String {
        if (value.signum() == 0) return "0 $symbol"

        val abs = value.abs()
        if (abs < TINY_THRESHOLD) {
            // Показать ноль там, где деньги есть, нельзя: человек решит, что средств нет.
            return "< ${plain(TINY_THRESHOLD)} $symbol"
        }

        val rounded = abs.setScale(decimals, RoundingMode.DOWN).stripTrailingZeros()
        val body = group(plain(rounded))
        return if (value.signum() < 0) "-$body $symbol" else "$body $symbol"
    }

    /** Сумма из минимальных единиц: wei, сатоши, лампорты. */
    fun amountFromRaw(raw: BigInteger, tokenDecimals: Int, symbol: String): String {
        val value = BigDecimal(raw).movePointLeft(tokenDecimals)
        return amount(value, symbol)
    }

    /** Изменение за период. Знак обязателен и не заменяется цветом. */
    fun signedFiat(value: BigDecimal, currency: String = "$"): String {
        val sign = if (value.signum() < 0) "−" else "+"
        val body = value.abs().setScale(2, RoundingMode.HALF_UP)
        return sign + currency + group(body.toPlainString())
    }

    /**
     * Сокращение адреса.
     *
     * Середина скрывается, начало и хвост сохраняются: подмена адреса в буфере
     * подбирает совпадающие края, а расходится как раз середина. Сокращение
     * с конца сделало бы подлог невидимым.
     */
    fun shortAddress(address: String): String {
        val rule = AddressRule.forAddress(address)
        if (address.length <= rule.head + rule.tail + 3) return address
        return address.take(rule.head) + "…" + address.takeLast(rule.tail)
    }

    /** Хеш транзакции. Показывается полностью только на экране деталей. */
    fun shortHash(hash: String): String {
        if (hash.length <= 18) return hash
        return hash.take(10) + "…" + hash.takeLast(6)
    }

    /**
     * Полный адрес, разбитый на группы по четыре знака.
     *
     * Человек сверяет адрес по группам, а не посимвольно: сплошная строка
     * из сорока знаков не проверяется глазом вообще.
     */
    fun groupedAddress(address: String, groupSize: Int = 4): String =
        address.chunked(groupSize).joinToString(" ")

    /** Комиссия всегда в двух единицах: «0,0004 ETH» само по себе ничего не говорит. */
    fun fee(coinAmount: BigDecimal, symbol: String, fiatValue: BigDecimal?): String {
        val coin = amount(coinAmount, symbol)
        return if (fiatValue == null) coin else "$coin · ≈ ${fiat(fiatValue)}"
    }

    private fun plain(value: BigDecimal): String =
        value.stripTrailingZeros().toPlainString().replace('.', DECIMAL_SEPARATOR)

    /** Расставляет разделитель разрядов в целой части. */
    private fun group(plainNumber: String): String {
        val normalized = plainNumber.replace('.', DECIMAL_SEPARATOR)
        val decimalAt = normalized.indexOf(DECIMAL_SEPARATOR)
        val intPart = if (decimalAt >= 0) normalized.substring(0, decimalAt) else normalized
        val rest = if (decimalAt >= 0) normalized.substring(decimalAt) else ""

        val negative = intPart.startsWith("-")
        val digits = if (negative) intPart.drop(1) else intPart

        val grouped = digits.reversed()
            .chunked(3)
            .joinToString(GROUP_SEPARATOR.toString())
            .reversed()

        return (if (negative) "-" else "") + grouped + rest
    }
}

/** Сколько знаков сохранять по краям адреса. Зависит от формата сети. */
internal data class AddressRule(val head: Int, val tail: Int) {
    companion object {
        /** EVM: два знака префикса плюс четыре значащих. */
        private val EVM = AddressRule(head = 6, tail = 4)

        /** bech32 длиннее, и его начало несёт версию — сохраняем больше. */
        private val BECH32 = AddressRule(head = 8, tail = 6)

        /** Base58 у Solana и TON: края одинаково значимы. */
        private val BASE58 = AddressRule(head = 4, tail = 4)

        fun forAddress(address: String): AddressRule = when {
            address.startsWith("0x", ignoreCase = true) -> EVM
            address.startsWith("bc1") || address.startsWith("tb1") -> BECH32
            else -> BASE58
        }
    }
}
