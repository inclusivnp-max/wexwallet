package wex.wallet.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme

private val KeypadRows = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6),
    listOf(7, 8, 9),
)
private const val ZeroDigit = 0

private val DotEmptySize: Dp = WexTheme.spacing.md
private val DotFilledSize: Dp = WexTheme.spacing.lg

/** Клавиша проседает заметнее обычной кнопки: PIN набирают вслепую, на ощупь. */
private const val KeyPressedScale = 0.92f

private const val ShakeHalfMs = Motion.Duration.STANDARD / 2
private const val ShakeFirstPeak = 45
private const val ShakeSecondPeak = 95
private const val ShakeMaxDp = 12f
private const val ShakeMidDp = 7f
private const val ShakeMinDp = 3f

/** Первый рывок — размашистый: его видно боковым зрением. */
private val ShakeOut = keyframes<Float> {
    durationMillis = ShakeHalfMs
    -ShakeMaxDp at ShakeFirstPeak
    ShakeMaxDp at ShakeSecondPeak
    0f at ShakeHalfMs
}

/** Второй — затухающий: движение гасится, а не обрывается. */
private val ShakeSettle = keyframes<Float> {
    durationMillis = ShakeHalfMs
    -ShakeMidDp at ShakeFirstPeak
    ShakeMinDp at ShakeSecondPeak
    0f at ShakeHalfMs
}

// Пропорции иконки удаления внутри её квадрата.
private const val IconStrokeRatio = 0.08f
private const val IconTagCut = 0.30f
private const val IconCrossLeft = 0.46f
private const val IconCrossRight = 0.78f
private const val IconCrossTop = 0.34f
private const val IconCrossBottom = 0.66f

/**
 * Клавиатура ввода PIN.
 *
 * Своя, а не системная: системная клавиатура кэширует ввод, показывает подсказки
 * и может быть подменена сторонним приложением — для кода от кошелька это
 * недопустимо. Заодно цифры не переезжают при смене раскладки.
 *
 * Об ошибке сообщает встряска, а не текст: неверный PIN случается и у хозяина
 * кошелька, и подпись «неправильный код» на весь экран заставляет читать там,
 * где достаточно почувствовать. Плюс подсмотреть через плечо нечего.
 *
 * @param pinLength сколько цифр в коде
 * @param enteredCount сколько уже введено
 * @param shake поднять при неверном коде; сбросить обратно, чтобы встряхнуть снова
 */
@Composable
fun WexPinKeypad(
    pinLength: Int,
    enteredCount: Int,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    shake: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinDots(
            total = pinLength.coerceAtLeast(0),
            filled = enteredCount.coerceIn(0, pinLength.coerceAtLeast(0)),
            shake = shake,
        )
        Spacer(Modifier.height(WexTheme.spacing.section))
        KeypadGrid(onDigit = onDigit, onBackspace = onBackspace)
    }
}

/**
 * Ряд точек заполнения.
 *
 * Высота ряда постоянна, растёт только точка: иначе клавиатура под ним
 * подпрыгивала бы на каждой цифре и палец промахивался бы по клавише.
 */
@Composable
private fun PinDots(
    total: Int,
    filled: Int,
    shake: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val shift = remember { Animatable(0f) }

    LaunchedEffect(shake) {
        if (shake) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shift.animateTo(targetValue = 0f, animationSpec = ShakeOut)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shift.animateTo(targetValue = 0f, animationSpec = ShakeSettle)
        }
    }

    Row(
        modifier = modifier
            .height(DotFilledSize)
            .offset { IntOffset(shift.value.dp.roundToPx(), 0) },
        horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            PinDot(filled = index < filled)
        }
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    val colors = WexTheme.colors
    val size by animateDpAsState(
        targetValue = if (filled) DotFilledSize else DotEmptySize,
        animationSpec = Motion.bouncy(),
        label = "pin-dot-size",
    )
    val color by animateColorAsState(
        targetValue = if (filled) colors.lime else colors.surface3,
        animationSpec = Motion.standard(Motion.Duration.QUICK),
        label = "pin-dot-color",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun KeypadGrid(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KeypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.lg)) {
                row.forEach { digit ->
                    DigitKey(digit = digit, onDigit = onDigit)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.lg)) {
            // Пустая ячейка слева от нуля: сетка не должна разъезжаться.
            Spacer(Modifier.size(WexTheme.touch.keypadKey))
            DigitKey(digit = ZeroDigit, onDigit = onDigit)
            BackspaceKey(onBackspace = onBackspace)
        }
    }
}

@Composable
private fun DigitKey(digit: Int, onDigit: (Int) -> Unit) {
    KeypadKey(onClick = { onDigit(digit) }) {
        Text(
            text = digit.toString(),
            style = WexTheme.type.title,
            color = WexTheme.colors.ink,
        )
    }
}

@Composable
private fun BackspaceKey(onBackspace: () -> Unit) {
    KeypadKey(onClick = onBackspace) {
        BackspaceIcon(tint = WexTheme.colors.ink2)
    }
}

/**
 * Клавиша.
 *
 * Отдача срабатывает до вызова обработчика: проверка PIN может занять время
 * (разбор хранилища ключей), и палец должен получить ответ раньше экрана.
 */
@Composable
private fun KeypadKey(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = WexTheme.colors
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(WexTheme.touch.keypadKey)
            .clip(CircleShape)
            .background(colors.surface2)
            .border(WexBorderWidth, colors.edgeFaint, CircleShape)
            .pressable(pressedScale = KeyPressedScale) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Иконка удаления.
 *
 * Нарисована здесь, а не взята из material-icons: модуль этот набор не
 * подключает, а тянуть его ради одного глифа — лишние сотни килобайт в APK.
 */
@Composable
private fun BackspaceIcon(tint: Color) {
    Canvas(modifier = Modifier.size(WexTheme.spacing.xxl)) {
        val width = size.width
        val height = size.height
        val stroke = width * IconStrokeRatio
        val tag = Path().apply {
            moveTo(0f, height / 2f)
            lineTo(width * IconTagCut, 0f)
            lineTo(width, 0f)
            lineTo(width, height)
            lineTo(width * IconTagCut, height)
            close()
        }
        drawPath(
            path = tag,
            color = tint,
            style = Stroke(width = stroke, join = StrokeJoin.Round),
        )
        val left = width * IconCrossLeft
        val right = width * IconCrossRight
        val top = height * IconCrossTop
        val bottom = height * IconCrossBottom
        drawLine(tint, Offset(left, top), Offset(right, bottom), stroke, StrokeCap.Round)
        drawLine(tint, Offset(right, top), Offset(left, bottom), stroke, StrokeCap.Round)
    }
}

@PreviewLightDark
@Composable
private fun WexPinKeypadPreview() {
    WexTheme {
        Box(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.xl),
        ) {
            WexPinKeypad(
                pinLength = 6,
                enteredCount = 3,
                onDigit = {},
                onBackspace = {},
            )
        }
    }
}
