package wex.wallet.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme

private val SegmentHeight: Dp = WexTheme.spacing.xs
private val SegmentRadius: Dp = WexTheme.spacing.xs / 2

/** Текущий шаг заливается вполсилы: начат, но ещё не засчитан. */
private const val CurrentStepAlpha = 0.5f

/**
 * Индикатор шагов.
 *
 * Нужен там, где процесс нельзя бросить посередине без последствий: запись
 * seed-фразы, её проверка, установка PIN. Пользователь должен видеть, сколько
 * осталось, иначе на третьем экране из пяти он решает, что застрял, и выходит —
 * а кошелёк остаётся наполовину настроенным.
 *
 * Полоски вместо цифр «шаг 2 из 5»: длина показывает остаток мгновенно,
 * без чтения, и не требует перевода.
 *
 * @param totalSteps сколько шагов в сценарии
 * @param currentStep индекс текущего шага с нуля; значение, равное [totalSteps],
 *   означает, что пройдены все
 */
@Composable
fun WexStepper(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    val safeTotal = totalSteps.coerceAtLeast(0)
    val safeCurrent = currentStep.coerceIn(0, safeTotal)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.xs),
    ) {
        repeat(safeTotal) { index ->
            StepSegment(
                passed = index < safeCurrent,
                current = index == safeCurrent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Один сегмент.
 *
 * Заливка растёт по ширине, а не проявляется прозрачностью: рост читается как
 * движение вперёд, а вспышка — как ошибка или обновление данных.
 */
@Composable
private fun StepSegment(
    passed: Boolean,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = WexTheme.colors
    val fillFraction by animateFloatAsState(
        targetValue = if (passed || current) 1f else 0f,
        animationSpec = Motion.standard(),
        label = "stepper-fill",
    )
    val fillColor by animateColorAsState(
        targetValue = if (passed) colors.lime else colors.lime.copy(alpha = CurrentStepAlpha),
        animationSpec = Motion.standard(),
        label = "stepper-color",
    )

    Box(
        modifier = modifier
            .height(SegmentHeight)
            .clip(RoundedCornerShape(SegmentRadius))
            .background(colors.surface3),
    ) {
        if (fillFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .background(fillColor),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WexStepperPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.lg),
        ) {
            WexStepper(totalSteps = 4, currentStep = 0)
            WexStepper(totalSteps = 4, currentStep = 2)
            WexStepper(totalSteps = 4, currentStep = 4)
        }
    }
}
