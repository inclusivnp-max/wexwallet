@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TWO_PI = 6.2831855f

/** Общие для всех режимов множители размера и свечения. */
private class EffectScale(settings: ShaderBackgroundSettings) {
    val size: Float = 0.4f + (settings.effectSize.coerceIn(0, 100) / 100f) * 1.2f
    val glow: Float = settings.effectGlow.coerceIn(0, 100) / 50f
    val vivid: Float = 0.6f + settings.intensity / 100f * 0.6f

    /**
     * У декоративных элементов своя нижняя граница непрозрачности: общий фон
     * держится приглушённым, и без этого метки просто не были бы видны.
     */
    val alpha: Float = 0.55f + 0.45f * (settings.alpha / 80f).coerceIn(0f, 1f)
}

internal fun DrawScope.drawEffects(
    particles: List<ShaderParticle>,
    phase: Float,
    palette: ShaderPaletteColors,
    settings: ShaderBackgroundSettings,
    touch: ShaderTouchState,
) {
    val scale = EffectScale(settings)
    val minSide = min(size.width, size.height)

    for (particle in particles) {
        val raw = palette.accent(particle.colorIndex)
        when (settings.effect) {
            ShaderEffect.BUBBLES -> drawBubble(particle, phase, raw, scale, minSide, settings, touch)
            ShaderEffect.SHAPES -> drawShape(particle, phase, raw, scale, minSide, settings, touch)
            ShaderEffect.PARTICLES -> drawSpark(particle, phase, raw, scale, minSide, settings, touch)
            ShaderEffect.BOKEH -> drawBokeh(particle, phase, raw, scale, minSide, settings, touch)
            ShaderEffect.NONE -> Unit
        }
    }
}

/** Пузырьки всплывают снизу вверх; край цикла спрятан затуханием у обеих границ. */
private fun DrawScope.drawBubble(
    particle: ShaderParticle,
    phase: Float,
    raw: Color,
    scale: EffectScale,
    minSide: Float,
    settings: ShaderBackgroundSettings,
    touch: ShaderTouchState,
) {
    val color = raw.lighten(0.30f)
    val travel = ((particle.phase + phase * particle.speedMul) % 1f + 1f) % 1f
    val center = Offset(
        x = particle.baseX * size.width +
            sin((phase * particle.speedMul * 2f + particle.phase) * TWO_PI) * particle.ampX * size.width,
        y = (1f - travel) * size.height,
    ).pushed(touch, minSide, settings)

    val edgeFade = min(travel / 0.12f, min((1f - travel) / 0.12f, 1f)).coerceIn(0f, 1f)
    val alpha = (scale.alpha * particle.alphaMul * scale.vivid * edgeFade).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return

    val radius = particle.radius * minSide * scale.size
    drawCircle(color.copy(alpha = alpha * 0.14f * scale.glow), radius, center)
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = max(2f, radius * 0.12f)),
    )
    // Блик смещён к верхнему левому краю — так пузырь читается объёмным.
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.6f),
        radius = max(1.5f, radius * 0.16f),
        center = center + Offset(-radius * 0.32f, -radius * 0.32f),
    )
}

private fun DrawScope.drawShape(
    particle: ShaderParticle,
    phase: Float,
    raw: Color,
    scale: EffectScale,
    minSide: Float,
    settings: ShaderBackgroundSettings,
    touch: ShaderTouchState,
) {
    val color = raw.lighten(0.28f)
    val center = effectCenter(particle, phase, 0.85f).pushed(touch, minSide, settings)
    val alpha = (scale.alpha * particle.alphaMul * scale.vivid).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return

    val radius = particle.radius * minSide * scale.size
    val path = polygonPath(center, radius, particle.sides, (phase * particle.spin + particle.phase) * 360f)
    drawPath(path, color.copy(alpha = alpha * 0.14f * scale.glow))
    drawPath(path, color.copy(alpha = alpha), style = Stroke(width = max(2.5f, radius * 0.10f)))
}

private fun DrawScope.drawSpark(
    particle: ShaderParticle,
    phase: Float,
    raw: Color,
    scale: EffectScale,
    minSide: Float,
    settings: ShaderBackgroundSettings,
    touch: ShaderTouchState,
) {
    val color = raw.lighten(0.45f)
    val center = effectCenter(particle, phase, 1.1f).pushed(touch, minSide, settings)
    val twinkle = if (particle.twinkle) {
        0.7f + 0.3f * sin((phase * (2f + particle.speedMul) + particle.phase) * TWO_PI)
    } else {
        1f
    }
    val alpha = (scale.alpha * particle.alphaMul * scale.vivid * twinkle).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return

    val radius = max(1.5f, particle.radius * minSide * scale.size)
    // Ореол позади ядра, иначе точки тонут в градиенте.
    drawCircle(color.copy(alpha = alpha * 0.28f * scale.glow), radius * (1f + 1.4f * scale.glow), center)
    drawCircle(color.copy(alpha = alpha), radius, center)
}

/** Мягкое пятно, собранное из вложенных заливок вместо размытия. */
private fun DrawScope.drawBokeh(
    particle: ShaderParticle,
    phase: Float,
    raw: Color,
    scale: EffectScale,
    minSide: Float,
    settings: ShaderBackgroundSettings,
    touch: ShaderTouchState,
) {
    val center = effectCenter(particle, phase, 0.9f).pushed(touch, minSide, settings)
    val alpha = ((settings.alpha / 100f) * particle.alphaMul * scale.vivid).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return

    val radius = particle.radius * minSide * scale.size
    drawCircle(raw.copy(alpha = alpha * 0.30f * scale.glow), radius, center)
    drawCircle(raw.copy(alpha = alpha * 0.45f * scale.glow), radius * 0.66f, center)
    drawCircle(raw.copy(alpha = alpha * 0.75f), radius * 0.34f, center)
}

private fun Offset.pushed(
    touch: ShaderTouchState,
    minSide: Float,
    settings: ShaderBackgroundSettings,
): Offset = this + touch.push(this, minSide * 0.5f, minSide * 0.10f, settings.touchFactor)

private fun polygonPath(center: Offset, radius: Float, sides: Int, rotationDegrees: Float): Path {
    val path = Path()
    if (sides < 3) return path

    val start = Math.toRadians(rotationDegrees.toDouble() - 90.0)
    val step = PI * 2.0 / sides
    for (index in 0 until sides) {
        val angle = start + step * index
        val x = center.x + (cos(angle) * radius).toFloat()
        val y = center.y + (sin(angle) * radius).toFloat()
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
