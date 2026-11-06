@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexRadius
import wex.wallet.core.designsystem.theme.WexTheme

/**
 * Толщина кольца фокуса.
 *
 * Кольцо есть всегда и всегда одной толщины — меняется только его цвет.
 * Появляйся оно вместе с местом под себя, поле подпрыгивало бы на три пикселя
 * при каждом переходе между «адрес» и «сумма», а на форме отправки палец в этот
 * момент уже летит к следующему полю.
 */
private val FocusRingWidth: Dp = 3.dp

/** Внешний контур кольца: радиус поля плюс его собственная толщина. */
private val FocusRingShape = RoundedCornerShape(WexRadius.button + FocusRingWidth)

/**
 * Поле ввода кошелька.
 *
 * Три вещи, которых нет в поле общего назначения.
 *
 * Первое — [monospace]: адрес и хеш набираются шрифтом с постоянной шириной знака,
 * иначе `l`, `1` и `I` в `bc1q…` неотличимы, а ошибка в одном знаке отправляет
 * деньги в никуда.
 *
 * Второе — состояние ошибки объясняется словами в [errorText], а не только красной
 * кромкой: «поле покраснело» не говорит, адрес ли не той сети, не сходится ли
 * контрольная сумма или не хватает средств на комиссию.
 *
 * Третье — геометрия постоянна во всех состояниях. Цвет кромки и кольца
 * анимируется, толщина — нет.
 */
@Composable
@Suppress("LongParameterList")
fun WexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    monospace: Boolean = false,
) {
    val colors = WexTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val edge by animateColorAsState(
        targetValue = fieldEdgeColor(isError = isError, focused = focused),
        animationSpec = Motion.standard(Motion.Duration.QUICK),
        label = "field-edge",
    )
    val ring by animateColorAsState(
        targetValue = fieldRingColor(isError = isError, focused = focused),
        animationSpec = Motion.standard(Motion.Duration.QUICK),
        label = "field-ring",
    )
    val textStyle = fieldTextStyle(monospace = monospace)

    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle.copy(color = colors.ink),
            cursorBrush = SolidColor(if (isError) colors.danger else colors.lime),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            decorationBox = { inner ->
                FieldFrame(edge = edge, ring = ring, leading = leadingIcon, trailing = trailingIcon) {
                    FieldContent(
                        showPlaceholder = value.isEmpty(),
                        placeholder = placeholder,
                        textStyle = textStyle,
                        inner = inner,
                    )
                }
            },
        )
        FieldErrorText(isError = isError, errorText = errorText)
    }
}

/** Подпись над полем. Остаётся приглушённой и в ошибке: красным кричит только текст ошибки. */
@Composable
private fun FieldLabel(label: String?) {
    if (label == null) return
    Text(
        text = label,
        style = WexTheme.type.label,
        color = WexTheme.colors.ink2,
    )
    Spacer(Modifier.height(WexTheme.spacing.sm))
}

/**
 * Оправа поля: кольцо снаружи, подложка и кромка внутри.
 *
 * Место под кольцо занято всегда, поэтому смена состояния не двигает соседние блоки.
 */
@Composable
private fun FieldFrame(
    edge: Color,
    ring: Color,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(FocusRingWidth, ring, FocusRingShape)
            .padding(FocusRingWidth)
            .heightIn(min = WexTheme.touch.button)
            .clip(WexTheme.shapes.button)
            .background(WexTheme.colors.surface2)
            .border(WexBorderWidth, edge, WexTheme.shapes.button)
            .padding(horizontal = WexTheme.spacing.md, vertical = WexTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(WexTheme.spacing.sm))
        }
        Box(modifier = Modifier.weight(1f)) { content() }
        if (trailing != null) {
            Spacer(Modifier.width(WexTheme.spacing.sm))
            trailing()
        }
    }
}

/** Подсказка лежит под курсором, а не вместо него: своей строки она не занимает. */
@Composable
private fun FieldContent(
    showPlaceholder: Boolean,
    placeholder: String?,
    textStyle: TextStyle,
    inner: @Composable () -> Unit,
) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (showPlaceholder && placeholder != null) {
            Text(
                text = placeholder,
                style = textStyle,
                color = WexTheme.colors.ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        inner()
    }
}

/** Причина отказа словами. Без неё пользователь правит адрес наугад. */
@Composable
private fun FieldErrorText(isError: Boolean, errorText: String?) {
    if (!isError || errorText.isNullOrBlank()) return
    Spacer(Modifier.height(WexTheme.spacing.xs))
    Text(
        text = errorText,
        style = WexTheme.type.label,
        color = WexTheme.colors.danger,
    )
}

@Composable
private fun fieldEdgeColor(isError: Boolean, focused: Boolean): Color = when {
    isError -> WexTheme.colors.danger
    focused -> WexTheme.colors.lime
    else -> WexTheme.colors.edgeFaint
}

@Composable
private fun fieldRingColor(isError: Boolean, focused: Boolean): Color = when {
    isError -> WexTheme.colors.dangerWash
    focused -> WexTheme.colors.limeWash
    else -> Color.Transparent
}

@Composable
private fun fieldTextStyle(monospace: Boolean): TextStyle =
    if (monospace) WexTheme.type.mono else WexTheme.type.bodyLarge

@Composable
private fun TextFieldShowcase() {
    Column(
        modifier = Modifier
            .background(WexTheme.colors.ground)
            .padding(WexTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.lg),
    ) {
        WexTextField(
            value = "",
            onValueChange = {},
            label = "Адрес получателя",
            placeholder = "bc1q… или ENS-имя",
            monospace = true,
        )
        WexTextField(
            value = "0,0421",
            onValueChange = {},
            label = "Сумма",
            trailingIcon = {
                Text(text = "ETH", style = WexTheme.type.label, color = WexTheme.colors.ink3)
            },
        )
        WexTextField(
            value = "bc1qcr8te4kr7xz9",
            onValueChange = {},
            label = "Адрес получателя",
            isError = true,
            errorText = "Контрольная сумма не сходится — проверьте последние знаки",
            monospace = true,
        )
    }
}

@Preview(name = "Поле ввода · тёмная")
@Composable
private fun WexTextFieldDarkPreview() {
    WexTheme(darkTheme = true) { TextFieldShowcase() }
}

@Preview(name = "Поле ввода · светлая")
@Composable
private fun WexTextFieldLightPreview() {
    WexTheme(darkTheme = false) { TextFieldShowcase() }
}
