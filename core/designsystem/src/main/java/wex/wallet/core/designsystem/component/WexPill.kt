@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme

/** Высота капсулы. Ниже строки списка, чтобы статус не спорил с суммой за внимание. */
private val PillHeight: Dp = 28.dp

/** Точка-индикатор. Мелкая оптика: размер подобран под кегль подписи, не под шкалу отступов. */
private val PillDotSize: Dp = 8.dp

/**
 * Смысл статуса в капсуле.
 *
 * Тон задаёт не только цвет, но и форму точки. Красный и зелёный — самая частая
 * пара, которую путает дейтераномалия, а экран кошелька читают на улице, где
 * от насыщенности остаётся половина. Поэтому у каждого тона своя фигура:
 * подтверждение — круг, ожидание — треугольник, отказ — ромб, справка — кольцо,
 * неактивное — черта.
 */
enum class WexPillTone {
    /** Транзакция подтверждена, адрес проверен, сеть отвечает. */
    Ok,

    /** Ждём подтверждений, комиссия ниже рыночной, сеть перегружена. */
    Warning,

    /** Отклонено сетью, недостаточно средств, адрес не той сети. */
    Danger,

    /** Нейтральное уточнение: тестовая сеть, тип адреса, номер нонса. */
    Info,

    /** Состояние без события: черновик, архив, скрытый актив. */
    Muted,
}

/**
 * Статусная капсула для строки актива или операции.
 *
 * Подпись обязательна, а точка — нет: цвет и фигура работают как быстрый признак
 * при скольжении взглядом по списку, но окончательный ответ «что с моими деньгами»
 * даёт только слово. Капсула без текста здесь невозможна намеренно.
 */
@Composable
fun WexPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: WexPillTone = WexPillTone.Muted,
    showDot: Boolean = true,
) {
    val accent = pillAccent(tone)
    Row(
        modifier = modifier
            .height(PillHeight)
            .clip(WexTheme.shapes.pill)
            .background(pillContainer(tone))
            .border(WexBorderWidth, accent.copy(alpha = 0.30f), WexTheme.shapes.pill)
            .padding(horizontal = WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.xs),
    ) {
        if (showDot) {
            PillDot(tone = tone, color = accent)
        }
        Text(
            text = text,
            style = WexTheme.type.label,
            color = accent,
            maxLines = 1,
        )
    }
}

/** Фигура точки — второй канал после цвета. Меняется вместе с тоном, иначе смысла нет. */
@Composable
private fun PillDot(tone: WexPillTone, color: Color) {
    Canvas(modifier = Modifier.size(PillDotSize)) {
        val side = size.minDimension
        when (tone) {
            WexPillTone.Ok -> drawCircle(color = color)
            WexPillTone.Info -> drawCircle(color = color, style = Stroke(width = side * 0.24f))
            WexPillTone.Warning -> drawPath(path = trianglePath(side), color = color)
            WexPillTone.Danger -> drawPath(path = diamondPath(side), color = color)
            WexPillTone.Muted -> drawMutedBar(color = color, side = side)
        }
    }
}

private fun DrawScope.drawMutedBar(color: Color, side: Float) = drawRect(
    color = color,
    topLeft = Offset(0f, side * 0.40f),
    size = Size(side, side * 0.20f),
)

private fun trianglePath(side: Float) = Path().apply {
    moveTo(side / 2f, 0f)
    lineTo(side, side)
    lineTo(0f, side)
    close()
}

private fun diamondPath(side: Float) = Path().apply {
    moveTo(side / 2f, 0f)
    lineTo(side, side / 2f)
    lineTo(side / 2f, side)
    lineTo(0f, side / 2f)
    close()
}

@Composable
private fun pillAccent(tone: WexPillTone): Color = when (tone) {
    WexPillTone.Ok -> WexTheme.colors.onLimeWash
    WexPillTone.Warning -> WexTheme.colors.warning
    WexPillTone.Danger -> WexTheme.colors.danger
    WexPillTone.Info -> WexTheme.colors.cyan
    WexPillTone.Muted -> WexTheme.colors.ink2
}

@Composable
private fun pillContainer(tone: WexPillTone): Color = when (tone) {
    WexPillTone.Ok -> WexTheme.colors.limeWash
    WexPillTone.Warning -> WexTheme.colors.warningWash
    WexPillTone.Danger -> WexTheme.colors.dangerWash
    WexPillTone.Info -> WexTheme.colors.cyanWash
    WexPillTone.Muted -> WexTheme.colors.surface3
}

@Composable
private fun PillShowcase() {
    Column(
        modifier = Modifier
            .background(WexTheme.colors.ground)
            .padding(WexTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm),
    ) {
        WexPill(text = "Подтверждено", tone = WexPillTone.Ok)
        WexPill(text = "2 из 6 подтверждений", tone = WexPillTone.Warning)
        WexPill(text = "Отклонено сетью", tone = WexPillTone.Danger)
        WexPill(text = "Sepolia · тестовая сеть", tone = WexPillTone.Info)
        WexPill(text = "Скрытый актив", tone = WexPillTone.Muted, showDot = false)
    }
}

@Preview(name = "Капсулы · тёмная")
@Composable
private fun WexPillDarkPreview() {
    WexTheme(darkTheme = true) { PillShowcase() }
}

@Preview(name = "Капсулы · светлая")
@Composable
private fun WexPillLightPreview() {
    WexTheme(darkTheme = false) { PillShowcase() }
}
