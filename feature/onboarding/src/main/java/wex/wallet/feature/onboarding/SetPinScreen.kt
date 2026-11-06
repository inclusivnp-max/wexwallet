@file:Suppress("MagicNumber")

package wex.wallet.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.WexPinKeypad
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme

private const val PinLength = 6

/** Флаг встряски держится ровно столько, сколько длится сама анимация в клавиатуре. */
private const val ShakeHoldMs = Motion.Duration.STANDARD

private const val MismatchText = "Коды не совпали. Введите ещё раз"

@Composable
fun SetPinScreen(
    onPinSet: (pin: CharArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(PinEntry()) }
    val shake = rememberShakeFlag(state.failedAttempt)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = WexTheme.spacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PinHeader(isConfirming = state.isConfirming, error = state.error)
        Spacer(Modifier.height(WexTheme.spacing.section))
        // Точки, отдача и встряска живут внутри клавиатуры: экран держит только состояние.
        WexPinKeypad(
            pinLength = PinLength,
            enteredCount = state.visible.length,
            onDigit = { digit -> state = state.withDigit(digit.digitToChar(), onPinSet) },
            onBackspace = { state = state.withoutLastDigit() },
            shake = shake,
        )
    }
}

private data class PinEntry(
    val pin: String = "",
    val confirmPin: String = "",
    val isConfirming: Boolean = false,
    val error: String? = null,
    /** Счётчик, а не флаг: две подряд одинаковые ошибки тоже должны тряхнуть точки. */
    val failedAttempt: Int = 0,
) {
    val visible: String get() = if (isConfirming) confirmPin else pin
}

private fun PinEntry.withDigit(digit: Char, onPinSet: (CharArray) -> Unit): PinEntry {
    if (!isConfirming) {
        val next = if (pin.length < PinLength) pin + digit else pin
        return copy(pin = next, isConfirming = next.length == PinLength, error = null)
    }
    val next = if (confirmPin.length < PinLength) confirmPin + digit else confirmPin
    return if (next.length < PinLength) {
        copy(confirmPin = next, error = null)
    } else {
        confirmed(next, onPinSet)
    }
}

private fun PinEntry.confirmed(entered: String, onPinSet: (CharArray) -> Unit): PinEntry {
    if (entered == pin) {
        onPinSet(pin.toCharArray())
        return copy(confirmPin = entered, error = null)
    }
    return copy(confirmPin = "", error = MismatchText, failedAttempt = failedAttempt + 1)
}

private fun PinEntry.withoutLastDigit(): PinEntry = if (isConfirming) {
    copy(confirmPin = confirmPin.dropLast(1), error = null)
} else {
    copy(pin = pin.dropLast(1), error = null)
}

/**
 * Флаг поднимается на время анимации и опускается обратно: клавиатура тряхнёт
 * точки только на смене значения, а вторая подряд ошибка тоже должна её тряхнуть.
 */
@Composable
private fun rememberShakeFlag(failedAttempt: Int): Boolean {
    var shake by remember { mutableStateOf(false) }
    LaunchedEffect(failedAttempt) {
        if (failedAttempt > 0) {
            shake = true
            delay(ShakeHoldMs.toLong())
            shake = false
        }
    }
    return shake
}

@Composable
private fun PinHeader(isConfirming: Boolean, error: String?, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isConfirming) "Повторите код" else "Придумайте код",
                style = WexTheme.type.title,
                color = WexTheme.colors.ink,
            )
            Spacer(Modifier.height(WexTheme.spacing.sm))
            Text(
                text = if (isConfirming) {
                    "Введите те же шесть цифр ещё раз"
                } else {
                    "Шесть цифр, чтобы открывать кошелёк"
                },
                style = WexTheme.type.body,
                color = WexTheme.colors.ink2,
                textAlign = TextAlign.Center,
            )
            PinError(error = error)
        }
    }
}

@Composable
private fun PinError(error: String?) {
    if (error == null) return
    Spacer(Modifier.height(WexTheme.spacing.sm))
    Text(
        text = error,
        style = WexTheme.type.label,
        color = WexTheme.colors.danger,
        textAlign = TextAlign.Center,
    )
}
