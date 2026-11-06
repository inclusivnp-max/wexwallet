@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TWO_PI = 6.2831855f

/**
 * Анимированный градиентный фон с декоративными элементами и откликом на касание.
 *
 * Фаза берётся от часов и растёт непрерывно, а не крутится в диапазоне 0..1:
 * зацикленное значение давало бы видимый рывок на границе цикла у всего, что
 * движется непериодично.
 *
 * Касание наблюдается на этапе [PointerEventPass.Initial] и никогда не
 * поглощается — контент поверх фона продолжает получать все события.
 */
@Composable
fun AnimatedShaderBackground(
    modifier: Modifier = Modifier,
    settings: ShaderBackgroundSettings = ShaderBackgroundSettings(),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val cycleMillis = max(6_000L, 22_000L - settings.speedClamped * 150L)

    // Опорное время берётся на первом кадре, поэтому фаза всегда стартует с нуля.
    val frame by produceState(FrameState.Zero, cycleMillis) {
        var startMillis = 0L
        while (true) {
            withFrameMillis { now ->
                if (startMillis == 0L) startMillis = now
                value = FrameState(now, (now - startMillis).toFloat() / cycleMillis)
            }
        }
    }
    val frameMillis = frame.millis
    val phase = frame.phase

    val touch = remember { ShaderTouchState() }
    val palette = rememberPalette(settings, phase)
    val particles = remember(settings.effect, settings.effectDensity) {
        ShaderParticles.build(settings.effect, settings.effectDensity)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .observeTouch(touch, settings.touchEnabled, frameMillis),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .let { if (settings.blurRadius > 0) it.blur(settings.blurRadius.dp) else it },
        ) {
            touch.advance(frameMillis)

            drawRect(color = palette.base)
            drawWash(palette, settings)
            repeat(BlobLayout.OriginX.size) { index ->
                drawBlob(index, phase, palette, settings, touch)
            }
            if (particles.isNotEmpty()) {
                drawEffects(particles, phase, palette, settings, touch)
            }
            if (!touch.isIdle) {
                drawTouchLayer(touch, palette.glow, settings, frameMillis)
            }
        }
        content()
    }
}

/** Момент кадра и накопленная фаза анимации. */
private data class FrameState(val millis: Long, val phase: Float) {
    companion object { val Zero = FrameState(0L, 0f) }
}

/** Цвета фона: либо из темы, либо из готовой палитры, со сдвигом оттенка. */
internal class ShaderPaletteColors(
    val base: Color,
    val accents: List<Color>,
    val glow: Color,
) {
    fun accent(index: Int): Color = accents[index.coerceIn(0, accents.lastIndex)]
}

@Composable
private fun rememberPalette(settings: ShaderBackgroundSettings, phase: Float): ShaderPaletteColors {
    val scheme = MaterialTheme.colorScheme
    // Оттенок пересчитывается ступенями: непрерывный пересчёт градиентов
    // на каждом кадре не даёт видимой разницы, но заметно греет процессор.
    val animatedHue = if (settings.colorAnimation) (phase % 1f) * 360f else 0f
    val hueBucket = ((settings.hueShift + animatedHue) / 3f).toInt()

    return remember(settings.palette, hueBucket, scheme) {
        val source = if (settings.palette <= 0) {
            listOf(scheme.surface, scheme.primary, scheme.secondary, scheme.tertiary)
        } else {
            ShaderPalettes.All[(settings.palette - 1).coerceIn(0, ShaderPalettes.Count - 1)]
        }
        val hue = hueBucket * 3f
        val shifted = source.map { it.shiftHue(hue) }
        val blend = Color(
            red = (shifted[1].red + shifted[3].red) / 2f,
            green = (shifted[1].green + shifted[3].green) / 2f,
            blue = (shifted[1].blue + shifted[3].blue) / 2f,
        )
        ShaderPaletteColors(
            base = shifted[0],
            accents = shifted.drop(1),
            glow = blend.lighten(0.35f),
        )
    }
}

private fun Modifier.observeTouch(
    touch: ShaderTouchState,
    enabled: Boolean,
    frameMillis: Long,
): Modifier {
    if (!enabled) return this
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                // Initial-проход: фон узнаёт о касании раньше контента и ничего не поглощает.
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val point = event.changes.firstOrNull() ?: continue
                when (event.type) {
                    PointerEventType.Press -> touch.press(point.position, frameMillis)
                    PointerEventType.Move -> touch.move(point.position, frameMillis)
                    PointerEventType.Release -> touch.release()
                    else -> Unit
                }
            }
        }
    }
}

/** Диагональная подложка. Статична: линейный сдвиг рвался бы на границе цикла. */
private fun DrawScope.drawWash(palette: ShaderPaletteColors, settings: ShaderBackgroundSettings) {
    val vivid = 0.5f + settings.intensity / 100f
    val tint = palette.accents[2]
    val strength = (settings.alpha / 100f) * 0.35f * vivid

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(tint.copy(alpha = 0f), tint.copy(alpha = strength.coerceIn(0f, 1f))),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
    )
}

private fun DrawScope.drawBlob(
    index: Int,
    phase: Float,
    palette: ShaderPaletteColors,
    settings: ShaderBackgroundSettings,
    touch: ShaderTouchState,
) {
    val vivid = 0.5f + settings.intensity / 100f
    val base = settings.alpha / 100f
    val color = palette.accents[index]
    val radius = max(size.width, size.height) * BlobLayout.RadiusScale[index]

    val angle = (phase + BlobLayout.Phase[index]) * TWO_PI
    // Вертикальная составляющая идёт с другой частотой, иначе пятна ходили бы по кругу.
    val center = Offset(
        x = size.width * BlobLayout.OriginX[index] + cos(angle) * size.width * 0.18f,
        y = size.height * BlobLayout.OriginY[index] + sin(angle * 0.8f) * size.height * 0.12f,
    )
    val minSide = min(size.width, size.height)
    val shifted = center + touch.push(center, minSide * 0.85f, minSide * 0.18f, settings.touchFactor)

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = (base * 0.78f * vivid).coerceIn(0f, 1f)),
                0.45f to color.copy(alpha = (base * 0.22f * vivid).coerceIn(0f, 1f)),
                1f to Color.Transparent,
            ),
            center = shifted,
            radius = radius,
        ),
        radius = radius,
        center = shifted,
    )
}

private fun DrawScope.drawTouchLayer(
    touch: ShaderTouchState,
    glow: Color,
    settings: ShaderBackgroundSettings,
    nowMillis: Long,
) {
    val minSide = min(size.width, size.height)
    val factor = settings.touchFactor

    for (point in touch.trail) {
        val age = (nowMillis - point.bornMillis).toFloat()
        val life = 1f - age / ShaderTouchState.TRAIL_LIFE_MILLIS
        if (life <= 0f) continue
        val radius = minSide * (0.05f + 0.055f * life)
        val alpha = 0.55f * life * life * (0.35f + 0.65f * touch.strength) * factor
        drawGlow(point.position, radius, glow, alpha)
    }

    if (touch.strength > 0.02f) {
        val radius = minSide * (0.15f + 0.06f * touch.strength)
        drawGlow(touch.position, radius, glow, 0.65f * touch.strength * factor)
    }

    for (pulse in touch.pulses) {
        val age = (nowMillis - pulse.bornMillis).toFloat()
        val progress = age / ShaderTouchState.PULSE_LIFE_MILLIS
        if (progress >= 1f) continue
        val eased = 1f - (1f - progress) * (1f - progress)
        val alpha = (0.7f * (1f - progress) * (1f - progress) * factor).coerceIn(0f, 1f)
        drawCircle(
            color = glow.copy(alpha = alpha),
            radius = minSide * (0.03f + 0.36f * eased),
            center = pulse.position,
            style = Stroke(width = max(2f, minSide * 0.012f * (1f - progress))),
        )
    }
}

private fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0.01f || radius <= 1f) return
    val clamped = alpha.coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = clamped),
                0.5f to color.copy(alpha = clamped * 0.38f),
                1f to Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

internal fun DrawScope.effectCenter(
    particle: ShaderParticle,
    phase: Float,
    verticalFrequency: Float,
): Offset = Offset(
    x = particle.baseX * size.width +
        sin((phase * particle.speedMul + particle.phase) * TWO_PI) * particle.ampX * size.width,
    y = particle.baseY * size.height +
        cos((phase * particle.speedMul * verticalFrequency + particle.phase) * TWO_PI) *
        particle.ampY * size.height,
)

internal fun Size.minSide(): Float = min(width, height)
