@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme
import java.math.BigDecimal

/**
 * Денежная сумма, которая доезжает до нового значения, а не подменяется рывком.
 *
 * Баланс меняется сам, без действий пользователя, и мгновенная подмена цифры
 * неотличима от сбоя отрисовки. Плавный счёт показывает, что пришли новые
 * данные, а не моргнул экран.
 *
 * Стиль по умолчанию — `display`: в нём включены табличные цифры, поэтому при
 * смене разряда число не дёргается по горизонтали.
 *
 * Форматирование идёт через [WexFormat], а не через `String.format`: на главном
 * экране эта сумма стоит рядом со строками, отформатированными им же, и разные
 * разделители разрядов в соседних числах читаются как ошибка приложения.
 */
@Composable
fun AnimatedBalance(
    value: Double,
    modifier: Modifier = Modifier,
    prefix: String = "$",
    style: TextStyle = WexTheme.type.display,
    fontWeight: FontWeight? = null,
    color: Color = WexTheme.colors.ink,
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = Motion.standard(Motion.Duration.SLOW),
        label = "balance",
    )
    Text(
        text = WexFormat.fiat(BigDecimal(animated.toDouble()), currency = prefix),
        modifier = modifier,
        style = style,
        fontWeight = fontWeight,
        color = color,
    )
}

@PreviewLightDark
@Composable
private fun AnimatedBalancePreview() {
    WexTheme {
        Box(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            AnimatedBalance(value = 12_480.62)
        }
    }
}
