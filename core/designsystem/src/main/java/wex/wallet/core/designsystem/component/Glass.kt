@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme

/** Доля высоты, на которой блик верхней кромки сходит на нет. */
private const val LitFalloff = 0.16f

/**
 * Прозрачность подложки по умолчанию.
 *
 * Ниже 0.6 подвижный фон начинает мешать читать суммы, выше 0.75 стекло
 * превращается в обычную непрозрачную карточку.
 */
private const val DefaultTintAlpha = 0.66f

/** Размер плитки: на 4 меньше цели касания — плитка декоративна и не должна спорить с кнопкой. */
private val IconTileSize: Dp = 44.dp

/**
 * Матовая панель поверх подвижного фона.
 *
 * Ощущение стекла даёт не одна прозрачность, а три вещи вместе: подложка
 * `surface1`, сквозь которую просвечивает фон; кромка, гаснущая от `edge`
 * к `edgeFaint` — так край читается как скошенный, а не как обводка; и блик
 * `lit` по верху, изображающий освещённую грань. Убрать любую — и панель
 * превращается в плоский полупрозрачный прямоугольник.
 *
 * Фон размывается глобально, поэтому дублировать и размывать его под каждой
 * панелью не нужно: за стеклом уже матовое пятно.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = WexTheme.shapes.card,
    contentPadding: PaddingValues = PaddingValues(WexTheme.spacing.cardPadding),
    tint: Color = WexTheme.colors.surface1,
    tintAlpha: Float = DefaultTintAlpha,
    elevation: Dp = WexTheme.spacing.md,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = WexTheme.colors

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(tint.copy(alpha = tintAlpha))
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to colors.lit,
                        LitFalloff to Color.Transparent,
                    ),
                ),
            )
            .border(
                width = WexBorderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(colors.edge, colors.edgeFaint),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Плитка под иконку.
 *
 * На подвижном фоне одиночная иконка теряется, поэтому её сажают на подложку
 * `limeWash`: приглушённый акцент даёт вес и постоянное место в строке, но не
 * спорит с главной кнопкой экрана. Содержимое красится в `onLimeWash` —
 * сам акцент по своей же заливке в светлой теме даёт около 2:1 и не читается.
 */
@Composable
fun IconTile(
    modifier: Modifier = Modifier,
    size: Dp = IconTileSize,
    shape: Shape = RoundedCornerShape(WexTheme.radius.nested(WexTheme.radius.row, WexTheme.spacing.xs)),
    container: Color = WexTheme.colors.limeWash,
    content: @Composable BoxScope.() -> Unit,
) {
    val contentColor = WexTheme.colors.onLimeWash

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        val scope = this
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            scope.content()
        }
    }
}

@PreviewLightDark
@Composable
private fun GlassCardPreview() {
    WexTheme {
        Box(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.md)) {
                    IconTile {
                        Text(text = "Ξ", style = WexTheme.type.headline)
                    }
                    Text(
                        text = "Комиссия сети",
                        style = WexTheme.type.label,
                        color = WexTheme.colors.ink2,
                    )
                    Text(
                        text = "0,00042 ETH",
                        style = WexTheme.type.amount,
                        color = WexTheme.colors.ink,
                    )
                }
            }
        }
    }
}
