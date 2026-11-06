@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalWexColors = staticCompositionLocalOf { WexDarkColors }
private val LocalWexTypography = staticCompositionLocalOf { WexTypographyDefaults }

/**
 * Точка доступа к токенам.
 *
 * Экраны обращаются к `WexTheme.colors` и `WexTheme.type`, а не к
 * `MaterialTheme.colorScheme`: у Material нет кромок слоёв, подложек
 * предупреждений и цветов сетей, и подмена их ближайшими по смыслу
 * ролями расходится между темами.
 */
object WexTheme {
    val colors: WexColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWexColors.current

    val type: WexTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalWexTypography.current

    val spacing = WexSpacing
    val radius = WexRadius
    val shapes = WexShapes
    val touch = WexTouch
}

/**
 * Схема Material собирается из наших токенов.
 *
 * Нужна ради компонентов Material 3, которые читают цвет сами: поля ввода,
 * индикаторы, разделители. Без сопоставления они рисовались бы палитрой
 * по умолчанию и выпадали бы из темы.
 */
private fun WexColors.toMaterialScheme() = if (isDark) {
    darkColorScheme(
        primary = lime,
        onPrimary = onLime,
        primaryContainer = limeWash,
        onPrimaryContainer = onLimeWash,
        secondary = cyan,
        onSecondary = ground,
        tertiary = violet,
        onTertiary = ground,
        background = ground,
        onBackground = ink,
        surface = surface1,
        onSurface = ink,
        surfaceVariant = surface2,
        onSurfaceVariant = ink2,
        outline = edgeStrong,
        outlineVariant = edge,
        error = danger,
        onError = ground,
        errorContainer = dangerWash,
        onErrorContainer = danger,
    )
} else {
    lightColorScheme(
        primary = lime,
        onPrimary = onLime,
        primaryContainer = limeWash,
        onPrimaryContainer = onLimeWash,
        secondary = cyan,
        onSecondary = surface1,
        tertiary = violet,
        onTertiary = surface1,
        background = ground,
        onBackground = ink,
        surface = surface1,
        onSurface = ink,
        surfaceVariant = surface2,
        onSurfaceVariant = ink2,
        outline = edgeStrong,
        outlineVariant = edge,
        error = danger,
        onError = surface1,
        errorContainer = dangerWash,
        onErrorContainer = danger,
    )
}

private fun WexTypography.toMaterialTypography() = Typography(
    displaySmall = display,
    headlineMedium = title,
    headlineSmall = title,
    titleLarge = headline,
    titleMedium = headline,
    titleSmall = label,
    bodyLarge = bodyLarge,
    bodyMedium = body,
    bodySmall = label,
    labelLarge = label,
    labelMedium = label,
    labelSmall = caption,
)

@Composable
fun WexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) WexDarkColors else WexLightColors
    val typography = WexTypographyDefaults

    CompositionLocalProvider(
        LocalWexColors provides colors,
        LocalWexTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = typography.toMaterialTypography(),
            shapes = androidx.compose.material3.Shapes(
                extraSmall = WexShapes.chip,
                small = WexShapes.button,
                medium = WexShapes.row,
                large = WexShapes.card,
                extraLarge = WexShapes.sheet,
            ),
            content = content,
        )
    }
}
