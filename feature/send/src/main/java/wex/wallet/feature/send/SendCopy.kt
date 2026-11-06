@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import wex.wallet.core.designsystem.component.WexNoticeTone
import wex.wallet.core.designsystem.component.WexPillTone

/**
 * Подписи экрана отправки и разбор технических сообщений.
 *
 * Вынесено из разметки: проверка адреса и ответы узла говорят по-английски и
 * покрыты тестами, поэтому их тексты не переводятся на месте, а раскрываются
 * здесь — на языке интерфейса.
 */

internal fun doneTitle(txStatus: TxStatus): String = when (txStatus) {
    TxStatus.PENDING -> "Отправлено"
    TxStatus.CONFIRMED -> "Подтверждено"
    TxStatus.FAILED -> "Транзакция не прошла"
}

internal fun statusLabel(txStatus: TxStatus): String = when (txStatus) {
    TxStatus.PENDING -> "Ждём подтверждения"
    TxStatus.CONFIRMED -> "Подтверждена"
    TxStatus.FAILED -> "Не прошла"
}

internal fun statusTone(txStatus: TxStatus): WexPillTone = when (txStatus) {
    TxStatus.PENDING -> WexPillTone.Warning
    TxStatus.CONFIRMED -> WexPillTone.Ok
    TxStatus.FAILED -> WexPillTone.Danger
}

/** Уровень проверки задаёт тон плашки: опасность нельзя показать спокойным цветом. */
internal fun ScamWarning.Level.toTone(): WexNoticeTone = when (this) {
    ScamWarning.Level.DANGER -> WexNoticeTone.Danger
    ScamWarning.Level.WARNING -> WexNoticeTone.Warning
    ScamWarning.Level.INFO -> WexNoticeTone.Info
}

/** Что случилось и почему это важно: заголовок называет, пояснение объясняет. */
internal data class ScamNotice(val title: String, val message: String)

internal fun ScamWarning.toNotice(): ScamNotice = when {
    message.contains("poisoning", ignoreCase = true) -> ScamNotice(
        title = "Адрес похож на недавний",
        message = "Совпадают края, расходится середина — так выглядит подмена адреса в буфере. " +
            "Сверьте адрес целиком.",
    )
    message.contains("Unlimited", ignoreCase = true) -> ScamNotice(
        title = "Запрошен безлимитный доступ к токену",
        message = "Разрешение позволяет снять любую сумму в любой момент. " +
            "Разрешайте только то, что тратите сейчас.",
    )
    message.contains("burn", ignoreCase = true) -> ScamNotice(
        title = "Это нулевой адрес",
        message = "Перевод на 0x000…000 сжигает средства: вернуть их не сможет никто.",
    )
    message.contains("First transfer", ignoreCase = true) -> ScamNotice(
        title = "Первый перевод на этот адрес",
        message = "Раньше вы сюда не отправляли. Сверьте адрес до подтверждения.",
    )
    else -> ScamNotice(title = "Проверьте получателя", message = message)
}

@Suppress("ReturnCount")
internal fun isValidEvmAddress(address: String): Boolean {
    if (!address.startsWith("0x") && !address.startsWith("0X")) return false
    val hex = address.removePrefix("0x").removePrefix("0X")
    return hex.length == 40 && hex.all {
        it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
    }
}

/** Ответ узла — строка для разработчика. Пользователю нужна причина и следующий шаг. */
internal fun humanizeRpcError(message: String): String = when {
    message.contains("insufficient funds", ignoreCase = true) ->
        "Не хватает средств на перевод вместе с комиссией."
    message.contains("nonce too low", ignoreCase = true) ->
        "Конфликт номера транзакции. Попробуйте ещё раз."
    message.contains("gas required exceeds allowance", ignoreCase = true) ->
        "Лимит газа больше, чем позволяет баланс."
    message.contains("execution reverted", ignoreCase = true) ->
        "Сеть отклонит такую транзакцию. Проверьте получателя и сумму."
    else -> message
}
