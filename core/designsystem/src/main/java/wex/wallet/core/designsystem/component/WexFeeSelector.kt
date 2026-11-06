package wex.wallet.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme
import java.math.BigDecimal

/**
 * Выбор скорости комиссии.
 *
 * Столбцом, а не тремя плитками в ряд: комиссия показывается сразу в монете и в
 * фиате, и в узкой плитке эта строка либо переносится, либо режется. Цена
 * ошибки здесь несимметрична — заниженная комиссия оставит перевод висеть
 * часами, поэтому обе величины должны быть видны до нажатия, а не после.
 *
 * Выбранный вариант держится на кромке и подложке акцента одновременно: одной
 * заливки мало при ярком солнце, одной кромки — при беглом взгляде.
 *
 * @param selectedIndex индекс выбранного варианта; вне диапазона — ничего не выбрано
 */
@Composable
fun WexFeeSelector(
    options: List<WexFeeOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.rowGap),
    ) {
        options.forEachIndexed { index, option ->
            FeeOptionRow(
                option = option,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

/**
 * Один вариант скорости.
 *
 * Плоский набор строк и чисел вместо оценки комиссии из доменного слоя: у BTC
 * это sat/vByte, у EVM — maxFeePerGas с приоритетной надбавкой, у TON — вообще
 * другая механика. Компоненту нужно нарисовать три варианта, а не знать, чем
 * они отличаются внутри.
 *
 * @param title название варианта: «Медленно», «Обычно», «Быстро»
 * @param eta ожидаемое время подтверждения в готовом виде
 * @param coinAmount комиссия в монете сети
 * @param symbol тикер монеты, которой платится комиссия
 * @param fiatValue та же комиссия в фиате; null, если курс неизвестен
 */
@Immutable
data class WexFeeOption(
    val title: String,
    val eta: String,
    val coinAmount: BigDecimal,
    val symbol: String,
    val fiatValue: BigDecimal? = null,
)

@Composable
private fun FeeOptionRow(
    option: WexFeeOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = WexTheme.colors
    val shape = WexTheme.shapes.row
    val container by animateColorAsState(
        targetValue = if (selected) colors.limeWash else colors.surface2,
        animationSpec = Motion.standard(),
        label = "fee-container",
    )
    val edge by animateColorAsState(
        targetValue = if (selected) colors.lime else colors.edgeFaint,
        animationSpec = Motion.standard(),
        label = "fee-edge",
    )
    val title by animateColorAsState(
        targetValue = if (selected) colors.onLimeWash else colors.ink,
        animationSpec = Motion.standard(),
        label = "fee-title",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WexTheme.touch.row)
            .clip(shape)
            .background(container)
            .border(WexBorderWidth, edge, shape)
            .pressable(onClick = onClick)
            .padding(horizontal = WexTheme.spacing.lg, vertical = WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = option.title, style = WexTheme.type.headline, color = title)
            Text(text = option.eta, style = WexTheme.type.caption, color = colors.ink3)
        }
        Text(
            text = WexFormat.fee(option.coinAmount, option.symbol, option.fiatValue),
            style = WexTheme.type.label,
            color = colors.ink2,
            textAlign = TextAlign.End,
        )
    }
}

private val PreviewFeeOptions = listOf(
    WexFeeOption(
        title = "Медленно",
        eta = "около часа",
        coinAmount = BigDecimal("0.00001240"),
        symbol = "BTC",
        fiatValue = BigDecimal("0.81"),
    ),
    WexFeeOption(
        title = "Обычно",
        eta = "20–30 минут",
        coinAmount = BigDecimal("0.00002980"),
        symbol = "BTC",
        fiatValue = BigDecimal("1.94"),
    ),
    WexFeeOption(
        title = "Быстро",
        eta = "следующий блок",
        coinAmount = BigDecimal("0.00006510"),
        symbol = "BTC",
        fiatValue = BigDecimal("4.23"),
    ),
)

@PreviewLightDark
@Composable
private fun WexFeeSelectorPreview() {
    WexTheme {
        Box(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            WexFeeSelector(
                options = PreviewFeeOptions,
                selectedIndex = 1,
                onSelect = {},
            )
        }
    }
}
