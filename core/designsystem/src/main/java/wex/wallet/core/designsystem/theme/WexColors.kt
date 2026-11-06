@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Полный набор цветовых токенов.
 *
 * Экраны берут цвет отсюда, а не из `MaterialTheme.colorScheme`: у Material нет
 * понятий «кромка слоя», «подложка предупреждения» и «цвет сети», а без них
 * светлая тема начинает расходиться с тёмной уже на третьем экране.
 */
@Immutable
@Suppress("LongParameterList")
class WexColors(
    // Подложки
    val ground: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,

    // Кромки. Глубина строится ими, а не тенями.
    val edgeFaint: Color,
    val edge: Color,
    val edgeStrong: Color,
    /** Внутренний блик по верхней кромке — имитация освещённого края. */
    val lit: Color,

    // Текст
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val ink4: Color,

    // Акцент
    val lime100: Color,
    val lime300: Color,
    val lime: Color,
    val limeHi: Color,
    val limeLo: Color,
    val lime700: Color,
    val onLime: Color,
    val limeWash: Color,

    // Смысловые
    val danger: Color,
    val dangerWash: Color,
    val warning: Color,
    val warningWash: Color,

    // Сети. Только опознавательный знак, никогда не акцент действия.
    val cyan: Color,
    val cyanWash: Color,
    val violet: Color,
    val violetWash: Color,
    val bitcoin: Color,

    val isDark: Boolean,
) {
    /**
     * Цвет текста поверх заливки [limeWash].
     *
     * На светлой теме сам [lime] по такой подложке даёт около 2:1 и не читается,
     * поэтому для текста берётся затемнённая ступень.
     */
    val onLimeWash: Color get() = if (isDark) lime else lime700

    /** Цвет положительного изменения. Совпадает с акцентом намеренно. */
    val positive: Color get() = if (isDark) lime else lime700

    /** Цвет отрицательного изменения. */
    val negative: Color get() = danger
}

internal val WexDarkColors = WexColors(
    ground = Color(0xFF0A0C0B),
    surface1 = Color(0xFF111413),
    surface2 = Color(0xFF171B19),
    surface3 = Color(0xFF1E2321),
    surface4 = Color(0xFF262C29),

    edgeFaint = Color(0xFFFFFFFF).copy(alpha = 0.055f),
    edge = Color(0xFFFFFFFF).copy(alpha = 0.10f),
    edgeStrong = Color(0xFFFFFFFF).copy(alpha = 0.17f),
    lit = Color(0xFFFFFFFF).copy(alpha = 0.13f),

    ink = Color(0xFFECF1EE),
    ink2 = Color(0xFF9AA6A0),
    ink3 = Color(0xFF626D68),
    ink4 = Color(0xFF3C4643),

    lime100 = Color(0xFFEDFBD0),
    lime300 = Color(0xFFCDF285),
    lime = Color(0xFFA8E831),
    limeHi = Color(0xFFBCF056),
    limeLo = Color(0xFF8CC71E),
    lime700 = Color(0xFF5E8F10),
    onLime = Color(0xFF0B1200),
    limeWash = Color(0xFFA8E831).copy(alpha = 0.13f),

    danger = Color(0xFFFF6B6B),
    dangerWash = Color(0xFFFF6B6B).copy(alpha = 0.13f),
    warning = Color(0xFFF0B429),
    warningWash = Color(0xFFF0B429).copy(alpha = 0.13f),

    cyan = Color(0xFF3DD9D0),
    cyanWash = Color(0xFF3DD9D0).copy(alpha = 0.13f),
    violet = Color(0xFFA175F5),
    violetWash = Color(0xFFA175F5).copy(alpha = 0.14f),
    bitcoin = Color(0xFFF7931A),

    isDark = true,
)

internal val WexLightColors = WexColors(
    ground = Color(0xFFFBFCFB),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF4F7F5),
    surface3 = Color(0xFFECF1ED),
    surface4 = Color(0xFFE1E8E3),

    edgeFaint = Color(0xFF0A1E14).copy(alpha = 0.06f),
    edge = Color(0xFF0A1E14).copy(alpha = 0.11f),
    edgeStrong = Color(0xFF0A1E14).copy(alpha = 0.20f),
    lit = Color(0xFFFFFFFF).copy(alpha = 0.90f),

    ink = Color(0xFF0D120F),
    ink2 = Color(0xFF4C5854),
    ink3 = Color(0xFF75817C),
    ink4 = Color(0xFFA7B1AC),

    lime100 = Color(0xFFF2FBDF),
    lime300 = Color(0xFFB7DE6A),
    lime = Color(0xFF7BB814),
    limeHi = Color(0xFF8FCC24),
    limeLo = Color(0xFF669C08),
    lime700 = Color(0xFF47700A),
    // Белый по этому лайму даёт 2,4:1 и проваливает AA — текст только тёмный.
    onLime = Color(0xFF0D120F),
    limeWash = Color(0xFF7BB814).copy(alpha = 0.12f),

    danger = Color(0xFFD33B3B),
    dangerWash = Color(0xFFD33B3B).copy(alpha = 0.10f),
    warning = Color(0xFFB37A06),
    warningWash = Color(0xFFB37A06).copy(alpha = 0.12f),

    cyan = Color(0xFF0E9A92),
    cyanWash = Color(0xFF0E9A92).copy(alpha = 0.11f),
    violet = Color(0xFF7B4FD6),
    violetWash = Color(0xFF7B4FD6).copy(alpha = 0.11f),
    bitcoin = Color(0xFFF7931A),

    isDark = false,
)
