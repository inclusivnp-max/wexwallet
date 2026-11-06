@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme

/** Размер знака на экране результата операции. */
private val DefaultCheckSize: Dp = 72.dp

/** Толщина линии знака. Это не кромка слоя, а рисунок, поэтому своя величина. */
private val DefaultCheckStroke: Dp = 6.dp

/** Кольцо начинается сверху, как секундная стрелка: движение читается как отсчёт. */
private const val StartAngle = -90f
private const val FullTurn = 360f

// Точки галочки в долях стороны знака.
private const val CheckStartX = 0.28f
private const val CheckStartY = 0.52f
private const val CheckBendX = 0.44f
private const val CheckBendY = 0.68f
private const val CheckEndX = 0.73f
private const val CheckEndY = 0.36f

/**
 * Галочка, которая прорисовывается.
 *
 * Отправка средств необратима, и её завершение должно ощущаться событием,
 * а не сменой надписи: рисующаяся линия занимает ровно столько времени, сколько
 * нужно, чтобы человек успел заметить, что перевод действительно ушёл.
 *
 * Цвет по умолчанию — `positive`: тот же, которым в списке операций помечен
 * приход. Успех во всём кошельке выглядит одинаково.
 */
@Composable
fun SuccessCheck(
    modifier: Modifier = Modifier,
    size: Dp = DefaultCheckSize,
    color: Color = WexTheme.colors.positive,
    strokeWidth: Dp = DefaultCheckStroke,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val ringProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = Motion.standard(Motion.Duration.SLOW),
        label = "ring",
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = Motion.standard(Motion.Duration.SLOW),
        label = "check",
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        drawRing(color = color, stroke = stroke, progress = ringProgress)
        drawCheck(color = color, stroke = stroke, progress = checkProgress)
    }
}

private fun DrawScope.drawRing(color: Color, stroke: Stroke, progress: Float) {
    val inset = stroke.width / 2f
    val side = size.minDimension

    drawArc(
        color = color,
        startAngle = StartAngle,
        sweepAngle = FullTurn * progress,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(side - inset * 2, side - inset * 2),
        style = stroke,
    )
}

private fun DrawScope.drawCheck(color: Color, stroke: Stroke, progress: Float) {
    if (progress <= 0f) return

    val side = size.minDimension
    val path = Path().apply {
        moveTo(side * CheckStartX, side * CheckStartY)
        lineTo(side * CheckBendX, side * CheckBendY)
        lineTo(side * CheckEndX, side * CheckEndY)
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val drawn = Path()
    measure.getSegment(0f, measure.length * progress, drawn, true)

    drawPath(path = drawn, color = color, style = stroke)
}

@PreviewLightDark
@Composable
private fun SuccessCheckPreview() {
    WexTheme {
        Box(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            SuccessCheck()
        }
    }
}
