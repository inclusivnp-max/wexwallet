@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.background

import androidx.compose.ui.graphics.Color

/** Декоративные элементы поверх градиента. */
enum class ShaderEffect { NONE, BOKEH, PARTICLES, BUBBLES, SHAPES }

/**
 * Настройки фона.
 *
 * Все значения — проценты либо доли, чтобы фон одинаково смотрелся на разных
 * экранах и его можно было вынести в настройки приложения без пересчётов.
 */
data class ShaderBackgroundSettings(
    /** 0 — палитра из темы, иначе номер готовой палитры. */
    val palette: Int = 0,
    /** Насыщенность фона. Держится низкой: фон не должен спорить с контентом. */
    val alphaPercent: Int = 34,
    val hueShift: Int = 0,
    val intensity: Int = 70,
    val colorAnimation: Boolean = false,
    val speed: Int = 45,
    val blurRadius: Int = 0,
    val effect: ShaderEffect = ShaderEffect.NONE,
    val effectDensity: Int = 60,
    val effectSize: Int = 50,
    val effectGlow: Int = 50,
    val touchEnabled: Boolean = true,
    val touchIntensity: Int = 60,
) {
    val alpha: Int get() = alphaPercent.coerceIn(0, 80)
    val speedClamped: Int get() = speed.coerceIn(10, 100)

    /** 60 соответствует исходной силе отклика, остальное масштабирует её. */
    val touchFactor: Float get() = touchIntensity.coerceIn(0, 100) / 60f
}

/** Готовые палитры: базовый цвет и три акцента. */
internal object ShaderPalettes {

    val All: List<List<Color>> = listOf(
        listOf(Color(0xFF0A1210), Color(0xFF92DD3D), Color(0xFF23FFF1), Color(0xFFA05CFF)),
        listOf(Color(0xFF0A1424), Color(0xFF3A84FF), Color(0xFF23E0FF), Color(0xFF8F62FF)),
        listOf(Color(0xFF140A1E), Color(0xFFFF5FCE), Color(0xFFFF3D6E), Color(0xFF23FFF1)),
        listOf(Color(0xFF1E0F0A), Color(0xFFFF8A3D), Color(0xFFFF5FA2), Color(0xFFC05CFF)),
        listOf(Color(0xFF041826), Color(0xFF1FA2A6), Color(0xFF35F0D0), Color(0xFF4DA8FF)),
    )

    val Count: Int get() = All.size
}

/** Геометрия трёх больших пятен. Разные фазы не дают им двигаться синхронно. */
internal object BlobLayout {
    val OriginX = floatArrayOf(0.20f, 0.82f, 0.45f)
    val OriginY = floatArrayOf(0.18f, 0.35f, 0.88f)
    val RadiusScale = floatArrayOf(0.72f, 0.62f, 0.82f)
    val Phase = floatArrayOf(0f, 0.33f, 0.66f)
}
