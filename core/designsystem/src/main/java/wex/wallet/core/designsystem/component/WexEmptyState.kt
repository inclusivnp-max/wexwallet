@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.theme.WexTheme

/** Глиф внутри круга. Мелкая оптика: подбирается под круг, а не под шкалу отступов. */
private val EmptyGlyphSize: Dp = 28.dp

/**
 * Что именно пусто.
 *
 * «Операций нет» и «историю не удалось получить» выглядят на экране одинаково —
 * пустой список, — но означают противоположное. В первом случае данные верны и
 * ждать нечего. Во втором на счету может быть входящий перевод, которого
 * пользователь не видит. Свести их в одно серое «Ничего нет» значит соврать
 * человеку про его деньги, поэтому вариант выбирается явно.
 */
enum class WexEmptyStateKind {
    /** Данные получены и они пусты: операций не было, активов нет, адресная книга чиста. */
    Empty,

    /** Данные получить не удалось: узел не ответил, нет сети, сеть отключена в настройках. */
    Unavailable,
}

/**
 * Пустое состояние списка.
 *
 * [reason] обязателен и стоит рядом с [title] не случайно: заголовок называет
 * факт, а причину пользователь должен получить, не открывая поддержку. «Узел не
 * ответил» и «переводов ещё не было» ведут к разным следующим действиям, и
 * выбор за пользователем, а не за нами.
 */
@Composable
fun WexEmptyState(
    title: String,
    reason: String,
    modifier: Modifier = Modifier,
    kind: WexEmptyStateKind = WexEmptyStateKind.Empty,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WexTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyStateBadge(kind)
        Spacer(Modifier.height(WexTheme.spacing.lg))
        Text(
            text = title,
            style = WexTheme.type.headline,
            color = WexTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(WexTheme.spacing.xs))
        Text(
            text = reason,
            style = WexTheme.type.body,
            color = WexTheme.colors.ink2,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(WexTheme.spacing.beforeCta))
            WexSecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

/**
 * Круг с глифом.
 *
 * У недоступности он подкрашен предупреждением, у настоящей пустоты — нейтрален:
 * серый круг не должен намекать на проблему там, где её нет.
 */
@Composable
private fun EmptyStateBadge(kind: WexEmptyStateKind) {
    val colors = WexTheme.colors
    val glyph = if (kind == WexEmptyStateKind.Unavailable) colors.warning else colors.ink3
    val container = if (kind == WexEmptyStateKind.Unavailable) colors.warningWash else colors.surface3

    Box(
        modifier = Modifier
            .size(WexTheme.spacing.giant)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(EmptyGlyphSize)) {
            val side = size.minDimension
            when (kind) {
                WexEmptyStateKind.Empty -> drawEmptyFrame(glyph, side)
                WexEmptyStateKind.Unavailable -> drawUnavailableSign(glyph, side)
            }
        }
    }
}

/** Пустая рамка: контейнер есть, содержимого нет. */
private fun DrawScope.drawEmptyFrame(color: Color, side: Float) {
    val inset = side * 0.10f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(side - inset * 2f, side - inset * 2f),
        cornerRadius = CornerRadius(side * 0.22f, side * 0.22f),
        style = Stroke(width = side * 0.10f),
    )
}

/** Перечёркнутый круг: запрет, а не пустота. Силуэт отличается от рамки даже боковым зрением. */
private fun DrawScope.drawUnavailableSign(color: Color, side: Float) {
    val center = Offset(side / 2f, side / 2f)
    val radius = side * 0.40f
    val arm = radius * 0.7071f
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = side * 0.10f),
    )
    drawLine(
        color = color,
        start = Offset(center.x - arm, center.y + arm),
        end = Offset(center.x + arm, center.y - arm),
        strokeWidth = side * 0.10f,
        cap = StrokeCap.Round,
    )
}

@Composable
private fun EmptyStateShowcase() {
    Column(
        modifier = Modifier
            .background(WexTheme.colors.ground)
            .padding(vertical = WexTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.section),
    ) {
        WexEmptyState(
            title = "Операций пока нет",
            reason = "Первая появится здесь, как только вы отправите или получите монеты.",
            kind = WexEmptyStateKind.Empty,
        )
        WexEmptyState(
            title = "История недоступна",
            reason = "Узел Ethereum не ответил. Баланс на месте — не пришёл только список операций.",
            kind = WexEmptyStateKind.Unavailable,
            actionText = "Повторить",
            onAction = {},
        )
    }
}

@Preview(name = "Пустые состояния · тёмная", heightDp = 640)
@Composable
private fun WexEmptyStateDarkPreview() {
    WexTheme(darkTheme = true) { EmptyStateShowcase() }
}

@Preview(name = "Пустые состояния · светлая", heightDp = 640)
@Composable
private fun WexEmptyStateLightPreview() {
    WexTheme(darkTheme = false) { EmptyStateShowcase() }
}
