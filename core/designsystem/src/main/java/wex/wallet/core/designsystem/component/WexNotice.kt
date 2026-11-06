@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme

/** Глиф внутри плитки. Мелкая оптика: подбирается под плитку, а не под шкалу отступов. */
private val NoticeGlyphSize: Dp = 20.dp

/**
 * Тон блок-уведомления.
 *
 * У каждого тона своя фигура глифа: перечёркнутый цветом треугольник — потеря,
 * одиночный восклицательный знак — задержка, буква «i» — справка, галочка — успех.
 * Пользователь на солнце и пользователь с дейтераномалией видят одну и ту же
 * разницу, даже когда красный и оранжевый сливаются.
 */
enum class WexNoticeTone {
    /** Можно потерять деньги: чужая сеть, подпись неизвестного контракта, отзыв доступа. */
    Danger,

    /** Деньги на месте, но что-то пойдёт не так: низкая комиссия, перегруженная сеть. */
    Warning,

    /** Справка о состоянии: тестовая сеть, курс от стороннего источника. */
    Info,

    /** Подтверждение необратимого шага: фраза сохранена, ключ импортирован. */
    Success,
}

/**
 * Блок-уведомление над содержимым экрана.
 *
 * Тон [WexNoticeTone.Danger] намеренно ломает ровный ритм остальных: у него
 * кромка в полную силу цвета, насыщенная плитка вместо полупрозрачной и более
 * крупный заголовок. Такое уведомление появляется там, где следующее касание
 * может стоить пользователю всего баланса, и оно обязано перехватить взгляд
 * раньше суммы, кнопки и заголовка экрана. Остальные тона намеренно тише:
 * если кричит всё, не кричит ничего.
 *
 * Действие ставится справа, когда это короткое «Изменить», и снизу через
 * [actionBelow], когда подпись длиннее двух слов — в узкой колонке рядом с
 * текстом она переносится по одному слову и перестаёт читаться кнопкой.
 */
@Composable
fun WexNotice(
    title: String,
    modifier: Modifier = Modifier,
    tone: WexNoticeTone = WexNoticeTone.Info,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    actionBelow: Boolean = false,
) {
    val accent = noticeAccent(tone)
    val urgent = tone == WexNoticeTone.Danger
    val action: (@Composable (Modifier) -> Unit)? =
        if (actionText != null && onAction != null) {
            { slot -> NoticeAction(text = actionText, accent = accent, onClick = onAction, modifier = slot) }
        } else {
            null
        }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(WexTheme.shapes.card)
            .background(noticeContainer(tone))
            .border(WexBorderWidth, if (urgent) accent else WexTheme.colors.edgeFaint, WexTheme.shapes.card)
            .padding(WexTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            NoticeIcon(tone = tone, accent = accent, urgent = urgent)
            Spacer(Modifier.width(WexTheme.spacing.md))
            NoticeText(
                title = title,
                message = message,
                urgent = urgent,
                modifier = Modifier.weight(1f),
            )
            if (action != null && !actionBelow) {
                Spacer(Modifier.width(WexTheme.spacing.md))
                action(Modifier.align(Alignment.CenterVertically))
            }
        }
        if (action != null && actionBelow) {
            action(Modifier.fillMaxWidth())
        }
    }
}

/**
 * Плитка с глифом. У опасности она залита цветом, у остальных тонов — только подкрашена.
 *
 * Приглушение сделано смешиванием с поверхностью, а не понижением альфы: [IconTile]
 * задаёт непрозрачность подложки сам и перезаписал бы её, а тогда глиф оказался бы
 * на плитке собственного цвета и исчез.
 */
@Composable
private fun NoticeIcon(tone: WexNoticeTone, accent: Color, urgent: Boolean) {
    IconTile(
        size = WexTheme.touch.buttonSmall,
        shape = WexTheme.shapes.chip,
        container = if (urgent) accent else lerp(WexTheme.colors.surface2, accent, 0.22f),
    ) {
        NoticeGlyph(
            tone = tone,
            color = if (urgent) WexTheme.colors.ground else accent,
        )
    }
}

@Composable
private fun NoticeText(
    title: String,
    message: String?,
    urgent: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = if (urgent) WexTheme.type.headline else WexTheme.type.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = WexTheme.colors.ink,
        )
        if (message != null) {
            Spacer(Modifier.height(WexTheme.spacing.xs))
            Text(
                text = message,
                style = WexTheme.type.body,
                color = WexTheme.colors.ink2,
            )
        }
    }
}

/**
 * Действие уведомления.
 *
 * Собрано на Box: Material-кнопка перехватила бы касание, и внешнее проседание
 * из [pressable] до неё не дошло бы.
 */
@Composable
private fun NoticeAction(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(WexTheme.touch.buttonSmall)
            .pressable(onClick = onClick)
            .clip(WexTheme.shapes.button)
            .background(accent.copy(alpha = 0.16f))
            .border(WexBorderWidth, accent.copy(alpha = 0.45f), WexTheme.shapes.button)
            .padding(horizontal = WexTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = WexTheme.type.label,
            color = accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun NoticeGlyph(tone: WexNoticeTone, color: Color) {
    Canvas(modifier = Modifier.size(NoticeGlyphSize)) {
        val side = size.minDimension
        when (tone) {
            WexNoticeTone.Danger -> drawDangerGlyph(color, side)
            WexNoticeTone.Warning -> drawWarningGlyph(color, side)
            WexNoticeTone.Info -> drawInfoGlyph(color, side)
            WexNoticeTone.Success -> drawSuccessGlyph(color, side)
        }
    }
}

private fun DrawScope.drawDangerGlyph(color: Color, side: Float) {
    val outline = Path().apply {
        moveTo(side * 0.50f, side * 0.06f)
        lineTo(side * 0.97f, side * 0.90f)
        lineTo(side * 0.03f, side * 0.90f)
        close()
    }
    drawPath(
        path = outline,
        color = color,
        style = Stroke(width = side * 0.12f, join = StrokeJoin.Round),
    )
    drawLine(
        color = color,
        start = Offset(side * 0.50f, side * 0.38f),
        end = Offset(side * 0.50f, side * 0.62f),
        strokeWidth = side * 0.11f,
        cap = StrokeCap.Round,
    )
    drawCircle(color = color, radius = side * 0.055f, center = Offset(side * 0.50f, side * 0.77f))
}

private fun DrawScope.drawWarningGlyph(color: Color, side: Float) {
    drawLine(
        color = color,
        start = Offset(side * 0.50f, side * 0.12f),
        end = Offset(side * 0.50f, side * 0.62f),
        strokeWidth = side * 0.16f,
        cap = StrokeCap.Round,
    )
    drawCircle(color = color, radius = side * 0.09f, center = Offset(side * 0.50f, side * 0.86f))
}

private fun DrawScope.drawInfoGlyph(color: Color, side: Float) {
    drawCircle(color = color, radius = side * 0.09f, center = Offset(side * 0.50f, side * 0.14f))
    drawLine(
        color = color,
        start = Offset(side * 0.50f, side * 0.38f),
        end = Offset(side * 0.50f, side * 0.88f),
        strokeWidth = side * 0.16f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSuccessGlyph(color: Color, side: Float) {
    val check = Path().apply {
        moveTo(side * 0.16f, side * 0.52f)
        lineTo(side * 0.40f, side * 0.76f)
        lineTo(side * 0.84f, side * 0.24f)
    }
    drawPath(
        path = check,
        color = color,
        style = Stroke(width = side * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@Composable
private fun noticeAccent(tone: WexNoticeTone): Color = when (tone) {
    WexNoticeTone.Danger -> WexTheme.colors.danger
    WexNoticeTone.Warning -> WexTheme.colors.warning
    WexNoticeTone.Info -> WexTheme.colors.cyan
    WexNoticeTone.Success -> WexTheme.colors.onLimeWash
}

@Composable
private fun noticeContainer(tone: WexNoticeTone): Color = when (tone) {
    WexNoticeTone.Danger -> WexTheme.colors.dangerWash
    WexNoticeTone.Warning -> WexTheme.colors.warningWash
    WexNoticeTone.Info -> WexTheme.colors.cyanWash
    WexNoticeTone.Success -> WexTheme.colors.limeWash
}

@Composable
private fun NoticeShowcase() {
    Column(
        modifier = Modifier
            .background(WexTheme.colors.ground)
            .padding(WexTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.cardGap),
    ) {
        WexNotice(
            title = "Адрес не из этой сети",
            tone = WexNoticeTone.Danger,
            message = "Вы отправляете USDT в сети TRON на адрес Ethereum. Монеты уйдут безвозвратно.",
            actionText = "Изменить",
            onAction = {},
        )
        WexNotice(
            title = "Комиссия ниже рыночной",
            tone = WexNoticeTone.Warning,
            message = "0,00012 BTC · при текущей загрузке подтверждения можно ждать больше часа.",
            actionText = "Поднять до 0,00021 BTC",
            onAction = {},
            actionBelow = true,
        )
        WexNotice(
            title = "Sepolia — тестовая сеть",
            tone = WexNoticeTone.Info,
            message = "Монеты в ней ничего не стоят и в основную сеть не переводятся.",
        )
        WexNotice(
            title = "Резервная фраза сохранена",
            tone = WexNoticeTone.Success,
            message = "Проверка пройдена: 12 слов из 12.",
        )
    }
}

@Preview(name = "Уведомления · тёмная")
@Composable
private fun WexNoticeDarkPreview() {
    WexTheme(darkTheme = true) { NoticeShowcase() }
}

@Preview(name = "Уведомления · светлая")
@Composable
private fun WexNoticeLightPreview() {
    WexTheme(darkTheme = false) { NoticeShowcase() }
}
