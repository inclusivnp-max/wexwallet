@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.theme.WexTheme

/** Ширина по умолчанию — примерно строка суммы; вызывающий обычно задаёт свою. */
private val DefaultSkeletonWidth: Dp = 80.dp

/** Период пробега блика. Медленнее выглядит зависанием, быстрее — мельтешит. */
private const val ShimmerPeriodMillis = 1_100

/** Ширина полосы блика в пикселях: заметно шире любой строки-заглушки. */
private const val ShimmerSpanPx = 300f

/**
 * Заглушка на месте ещё не загруженных данных.
 *
 * Скелетон честнее спиннера: он показывает форму того, что появится, поэтому
 * список активов не прыгает при подстановке балансов — а прыжок в момент
 * касания строки уводит палец не на тот актив.
 *
 * Подложка `surface2` с бликом `surface3`: заглушка должна читаться как
 * пустой слой интерфейса, а не как выключенный текст.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    width: Dp = DefaultSkeletonWidth,
    height: Dp = WexTheme.spacing.lg,
    cornerRadius: Dp = WexTheme.radius.nested(WexTheme.radius.chip, WexTheme.spacing.xs),
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShimmerPeriodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    val base = WexTheme.colors.surface2
    val highlight = WexTheme.colors.surface3
    val sweep = shimmer * 3f - 1f

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(sweep * ShimmerSpanPx, 0f),
                    end = Offset((sweep + 1f) * ShimmerSpanPx, 0f),
                ),
            ),
    )
}

@PreviewLightDark
@Composable
private fun SkeletonPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm),
        ) {
            Skeleton(width = WexTheme.spacing.giant * 2)
            Skeleton(width = WexTheme.spacing.giant, height = WexTheme.spacing.md)
        }
    }
}
