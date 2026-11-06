@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.background

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.random.Random

/** Один декоративный элемент. Позиция задаётся долями от размера холста. */
internal data class ShaderParticle(
    val baseX: Float,
    val baseY: Float,
    val radius: Float,
    val ampX: Float,
    val ampY: Float,
    val phase: Float,
    val speedMul: Float,
    val spin: Float,
    val sides: Int,
    val colorIndex: Int,
    val alphaMul: Float,
    val twinkle: Boolean,
)

internal object ShaderParticles {

    /**
     * Раскладка детерминирована по зерну: при перерисовке и смене размера элементы
     * остаются на своих местах, иначе фон «вскипал» бы на каждом кадре.
     */
    fun build(effect: ShaderEffect, density: Int): List<ShaderParticle> {
        if (effect == ShaderEffect.NONE) return emptyList()

        val random = Random(effect.ordinal * 977 + 1234)
        val ratio = density.coerceIn(0, 100) / 100f

        fun count(low: Int, high: Int): Int {
            val exact = (low + (high - low) * ratio).roundToInt()
            return if (ratio <= 0f) 0 else exact.coerceAtLeast(1)
        }

        fun rf(low: Float, high: Float) = low + random.nextFloat() * (high - low)

        return when (effect) {
            ShaderEffect.BOKEH -> List(count(4, 22)) {
                ShaderParticle(
                    baseX = random.nextFloat(), baseY = random.nextFloat(),
                    radius = rf(0.06f, 0.20f), ampX = rf(0.03f, 0.10f), ampY = rf(0.03f, 0.09f),
                    phase = random.nextFloat(), speedMul = rf(0.4f, 1.0f), spin = 0f, sides = 0,
                    colorIndex = random.nextInt(3), alphaMul = rf(0.35f, 0.75f), twinkle = false,
                )
            }

            ShaderEffect.PARTICLES -> List(count(12, 46)) {
                ShaderParticle(
                    baseX = random.nextFloat(), baseY = random.nextFloat(),
                    radius = rf(0.012f, 0.030f), ampX = rf(0.02f, 0.08f), ampY = rf(0.02f, 0.08f),
                    phase = random.nextFloat(), speedMul = rf(0.5f, 1.4f), spin = 0f, sides = 0,
                    colorIndex = random.nextInt(3), alphaMul = rf(0.75f, 1.0f), twinkle = true,
                )
            }

            ShaderEffect.BUBBLES -> List(count(10, 38)) {
                ShaderParticle(
                    baseX = random.nextFloat(), baseY = random.nextFloat(),
                    radius = rf(0.022f, 0.070f), ampX = rf(0.01f, 0.05f), ampY = 0f,
                    phase = random.nextFloat(), speedMul = rf(0.5f, 1.2f), spin = 0f, sides = 0,
                    colorIndex = random.nextInt(3), alphaMul = rf(0.6f, 0.95f), twinkle = false,
                )
            }

            ShaderEffect.SHAPES -> List(count(5, 20)) {
                ShaderParticle(
                    baseX = random.nextFloat(), baseY = random.nextFloat(),
                    radius = rf(0.045f, 0.110f), ampX = rf(0.03f, 0.10f), ampY = rf(0.03f, 0.09f),
                    phase = random.nextFloat(), speedMul = rf(0.4f, 1.0f), spin = rf(-1.2f, 1.2f),
                    sides = 3 + random.nextInt(4), colorIndex = random.nextInt(3),
                    alphaMul = rf(0.6f, 1.0f), twinkle = false,
                )
            }

            ShaderEffect.NONE -> emptyList()
        }
    }
}

/** Сдвиг оттенка по кругу. Нужен, чтобы палитра могла медленно перетекать. */
internal fun Color.shiftHue(degrees: Float): Color {
    if (degrees == 0f) return this
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[0] = ((hsv[0] + degrees) % 360f + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Подмешивает белый, чтобы метка читалась на фоне того же оттенка. */
internal fun Color.lighten(factor: Float): Color {
    val k = factor.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * k,
        green = green + (1f - green) * k,
        blue = blue + (1f - blue) * k,
        alpha = alpha,
    )
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt(),
)
