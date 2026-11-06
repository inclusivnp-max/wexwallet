@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme

/** Размер знака на приветственном экране. */
private val DefaultMarkSize: Dp = 96.dp

/** Толщина кольца в долях размера: тоньше — рассыпается на маленьких экранах, толще — давит. */
private const val RingWidthRatio = 0.055f

/** Высота буквы в долях размера. */
private const val LetterRatio = 0.38f

/** Оборот дуги. Шесть секунд — на грани заметности: знак живой, но взгляд не удерживает. */
private const val SweepPeriodMillis = 6_000

/** Длина дуги: примерно треть окружности. */
private const val SweepAngle = 110f
private const val FullTurn = 360f

/**
 * Фирменный знак: буква в кольце с медленно ползущей дугой.
 *
 * Знак висит на экранах, где пользователь ждёт — создание кошелька, разбор
 * резервной фразы. Кольцо неподвижно, а дуга идёт по нему от лайма к фиолету:
 * этого хватает, чтобы экран не выглядел зависшим, и мало, чтобы отвлекать
 * от текста, который в этот момент важно прочитать.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = DefaultMarkSize,
    letter: String = "W",
) {
    val colors = WexTheme.colors
    val transition = rememberInfiniteTransition(label = "brand")
    val sweepStart by transition.animateFloat(
        initialValue = 0f,
        targetValue = FullTurn,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SweepPeriodMillis, easing = Motion.Curve.Emphasized),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = size.toPx() * RingWidthRatio)
            drawRing(color = colors.edge, stroke = stroke)
            drawSweep(
                brush = Brush.sweepGradient(listOf(colors.lime, colors.violet, colors.lime)),
                stroke = stroke,
                startAngle = sweepStart,
            )
        }
        Text(
            text = letter,
            style = WexTheme.type.display,
            fontSize = (size.value * LetterRatio).sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
        )
    }
}

private fun DrawScope.drawRing(color: Color, stroke: Stroke) {
    drawCircle(
        color = color,
        radius = size.minDimension / 2f - stroke.width / 2f,
        style = stroke,
    )
}

private fun DrawScope.drawSweep(brush: Brush, stroke: Stroke, startAngle: Float) {
    val inset = stroke.width / 2f

    drawArc(
        brush = brush,
        startAngle = startAngle,
        sweepAngle = SweepAngle,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        style = stroke,
    )
}

@PreviewLightDark
@Composable
private fun BrandMarkPreview() {
    WexTheme {
        Box(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            BrandMark()
        }
    }
}
