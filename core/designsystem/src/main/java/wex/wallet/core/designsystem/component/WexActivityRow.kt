@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexSpacing
import wex.wallet.core.designsystem.theme.WexTheme
import java.math.BigDecimal

/** Диаметр кружка направления в углу бейджа. */
private val MarkerSize: Dp = WexSpacing.xl

/** Поле рисунка внутри кружка. */
private val GlyphSize: Dp = WexSpacing.md

/** Толщина штриха значка. Оптика иконки, к шкале отступов отношения не имеет. */
private val GlyphStroke: Dp = 1.5.dp

/**
 * Что случилось с операцией.
 *
 * Неудача — не третье направление, а отсутствие результата, но в углу бейджа
 * место одно, и показать там нужно самое важное. Провалившийся перевод почти
 * всегда исходящий: чужая неудачная транзакция до истории владельца не доходит,
 * потому что средства к нему так и не пришли.
 */
enum class WexActivityKind {
    Outgoing,
    Incoming,
    Failed,
}

/**
 * Строка операции в истории.
 *
 * Направление вынесено на бейдж отдельным кружком, а не спрятано в цвет суммы:
 * при дальтонизме и на солнце цвет пропадает первым, а перепутанное направление
 * перевода — самая дорогая ошибка чтения истории.
 *
 * Сумма всегда со знаком по той же причине: «+» и «−» переживают и монохромный
 * скриншот, и выцветший экран.
 */
@Composable
fun WexActivityRow(
    label: String,
    counterparty: String,
    network: String,
    symbol: String,
    amount: BigDecimal,
    relativeTime: String,
    kind: WexActivityKind,
    modifier: Modifier = Modifier,
    decrypted: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WexTheme.touch.row)
            .pressable(enabled = onClick != null) { onClick?.invoke() }
            .clip(WexTheme.shapes.row)
            .background(WexTheme.colors.surface2)
            .padding(horizontal = WexTheme.spacing.lg, vertical = WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionBadge(symbol = symbol, kind = kind)
        Spacer(Modifier.width(WexTheme.spacing.md))
        ActivityTitles(
            label = label,
            counterparty = counterparty,
            network = network,
            decrypted = decrypted,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(WexTheme.spacing.sm))
        ActivityValues(
            amount = amount,
            symbol = symbol,
            relativeTime = relativeTime,
            kind = kind,
        )
    }
}

/**
 * Бейдж сети с меткой направления.
 *
 * Кружок подложен цветом строки и обведён кромкой: без выреза значок сливался бы
 * с логотипом сети, а именно его читают первым при пролистывании истории.
 */
@Composable
private fun DirectionBadge(
    symbol: String,
    kind: WexActivityKind,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        ChainBadge(symbol = symbol)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(MarkerSize)
                .clip(CircleShape)
                .background(WexTheme.colors.surface2)
                .border(WexBorderWidth, WexTheme.colors.edge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MarkerGlyph(kind = kind)
        }
    }
}

@Composable
private fun MarkerGlyph(kind: WexActivityKind) {
    val colors = WexTheme.colors
    val color = when (kind) {
        WexActivityKind.Outgoing -> colors.ink2
        WexActivityKind.Incoming -> colors.positive
        WexActivityKind.Failed -> colors.danger
    }
    Canvas(modifier = Modifier.size(GlyphSize)) {
        val stroke = Stroke(
            width = GlyphStroke.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        if (kind == WexActivityKind.Failed) {
            drawExclamation(color, stroke)
        } else {
            rotate(degrees = if (kind == WexActivityKind.Outgoing) 0f else 180f) {
                drawUpArrow(color, stroke)
            }
        }
    }
}

private fun DrawScope.drawUpArrow(color: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    val shaft = Path().apply {
        moveTo(w * 0.5f, h * 0.84f)
        lineTo(w * 0.5f, h * 0.18f)
        moveTo(w * 0.2f, h * 0.46f)
        lineTo(w * 0.5f, h * 0.16f)
        lineTo(w * 0.8f, h * 0.46f)
    }
    drawPath(path = shaft, color = color, style = stroke)
}

private fun DrawScope.drawExclamation(color: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    val bar = Path().apply {
        moveTo(w * 0.5f, h * 0.16f)
        lineTo(w * 0.5f, h * 0.58f)
    }
    drawPath(path = bar, color = color, style = stroke)
    drawCircle(color = color, radius = stroke.width * 0.6f, center = Offset(w * 0.5f, h * 0.82f))
}

@Composable
private fun ActivityTitles(
    label: String,
    counterparty: String,
    network: String,
    decrypted: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = WexTheme.type.headline,
            color = WexTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "$counterparty · $network",
            style = WexTheme.type.label,
            color = WexTheme.colors.ink3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!decrypted) {
            // Молчать нельзя: без пометки владелец решит, что перевод пустой.
            Text(
                text = "Не удалось расшифровать",
                style = WexTheme.type.label,
                color = WexTheme.colors.warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActivityValues(
    amount: BigDecimal,
    symbol: String,
    relativeTime: String,
    kind: WexActivityKind,
) {
    val colors = WexTheme.colors
    val sign = if (kind == WexActivityKind.Incoming) "+" else "−"
    val amountColor = when (kind) {
        WexActivityKind.Incoming -> colors.positive
        WexActivityKind.Outgoing -> colors.ink
        // Баланс не изменился, поэтому сумма приглушена до уровня подписи.
        WexActivityKind.Failed -> colors.ink3
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = sign + WexFormat.amount(amount.abs(), symbol),
            style = WexTheme.type.amount,
            color = amountColor,
            maxLines = 1,
        )
        Text(
            text = relativeTime,
            style = WexTheme.type.amountSecondary,
            color = colors.ink3,
            maxLines = 1,
        )
    }
}

@PreviewLightDark
@Composable
private fun WexActivityRowPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            WexActivityRow(
                label = "Отправлено",
                counterparty = WexFormat.shortAddress("0x71C7656EC7ab88b098defB751B7401B5f6d8976F"),
                network = "Ethereum",
                symbol = "ETH",
                amount = BigDecimal("0.42"),
                relativeTime = "12 минут назад",
                kind = WexActivityKind.Outgoing,
            )
            Spacer(Modifier.height(WexTheme.spacing.rowGap))
            WexActivityRow(
                label = "Получено",
                counterparty = WexFormat.shortAddress("EQCcrxSpAvOgQKk-ZBOM6QUlvQdSkyM8vqDNMnE2Vg"),
                network = "TON",
                symbol = "USDT",
                amount = BigDecimal("120"),
                relativeTime = "вчера",
                kind = WexActivityKind.Incoming,
                decrypted = false,
            )
            Spacer(Modifier.height(WexTheme.spacing.rowGap))
            WexActivityRow(
                label = "Обмен",
                counterparty = "Jupiter",
                network = "Solana",
                symbol = "SOL",
                amount = BigDecimal("3.5"),
                relativeTime = "3 дня назад",
                kind = WexActivityKind.Failed,
            )
        }
    }
}
