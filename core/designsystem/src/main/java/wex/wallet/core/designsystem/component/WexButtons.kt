@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme

/**
 * Главное действие экрана: подписать, отправить, продолжить.
 *
 * Собрано на Box, а не на Material-кнопке: та перехватывает касание, и внешнее
 * проседание [pressable] до неё не доходит, а в кошельке отклик на нажатие —
 * единственное подтверждение, что палец попал по кнопке необратимого действия.
 *
 * Выключенное состояние уходит на `surface3` и `ink4`: кнопка остаётся на месте
 * и держит вёрстку, но перестаёт выглядеть нажимаемой.
 */
@Composable
fun WexPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = WexTheme.colors
    val container by animateColorAsState(
        targetValue = if (enabled) colors.lime else colors.surface3,
        animationSpec = Motion.standard(Motion.Duration.QUICK),
        label = "primary-container",
    )

    WexButtonShell(
        text = text,
        onClick = onClick,
        enabled = enabled,
        container = container,
        contentColor = if (enabled) colors.onLime else colors.ink4,
        border = null,
        elevation = if (enabled) WexTheme.spacing.sm else 0.dp,
        icon = icon,
        modifier = modifier,
    )
}

/**
 * Второстепенное действие рядом с главным: та же форма и высота, но только кромка.
 *
 * Заливки нет намеренно — на экране подтверждения перевода заливка должна быть
 * ровно одна, иначе «Отправить» и «Изменить» весят одинаково.
 */
@Composable
fun WexSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = WexTheme.colors

    WexButtonShell(
        text = text,
        onClick = onClick,
        enabled = enabled,
        container = Color.Transparent,
        contentColor = if (enabled) colors.ink else colors.ink4,
        border = if (enabled) colors.edgeStrong else colors.edge,
        elevation = 0.dp,
        icon = icon,
        modifier = modifier,
    )
}

/**
 * Третья ступень: «Назад», «Пропустить», «Не сейчас».
 *
 * Без кромки и заливки, текст приглушён до `ink2`. Такое действие уводит
 * из потока, и его вес должен быть заметно ниже кромочной кнопки — иначе на
 * экране резервной фразы отказ читается как равноправный вариант.
 */
@Composable
fun WexGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = WexTheme.colors

    WexButtonShell(
        text = text,
        onClick = onClick,
        enabled = enabled,
        container = Color.Transparent,
        contentColor = if (enabled) colors.ink2 else colors.ink4,
        border = null,
        elevation = 0.dp,
        icon = icon,
        modifier = modifier,
    )
}

/** Общий корпус всех трёх кнопок: разница между ними только в цвете и кромке. */
@Composable
private fun WexButtonShell(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    container: Color,
    contentColor: Color,
    border: Color?,
    elevation: Dp,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
) {
    val shape: Shape = WexTheme.shapes.button

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WexTheme.touch.button)
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(container)
            .borderOrNone(border, shape)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ButtonLabel(text = text, icon = icon, color = contentColor)
    }
}

/** Кромка ставится не всегда, а `Modifier.border` не умеет «без цвета». */
private fun Modifier.borderOrNone(color: Color?, shape: Shape): Modifier =
    if (color == null) this else border(WexBorderWidth, color, shape)

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(WexTheme.spacing.xl),
            )
            Spacer(Modifier.width(WexTheme.spacing.sm))
        }
        Text(
            text = text,
            color = color,
            style = WexTheme.type.headline,
        )
    }
}

@PreviewLightDark
@Composable
private fun WexButtonsPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(WexTheme.touch.gap),
        ) {
            WexPrimaryButton(text = "Отправить 0,5 ETH", onClick = {})
            WexPrimaryButton(text = "Отправить 0,5 ETH", onClick = {}, enabled = false)
            WexSecondaryButton(text = "Восстановить по фразе", onClick = {})
            WexGhostButton(text = "Назад", onClick = {})
        }
    }
}
