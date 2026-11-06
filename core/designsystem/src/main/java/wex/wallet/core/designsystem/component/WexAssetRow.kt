package wex.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.theme.WexSpacing
import wex.wallet.core.designsystem.theme.WexTheme
import java.math.BigDecimal

/**
 * Высота места под баланс. Совпадает с междустрочным интервалом `type.amount`,
 * поэтому заглушка стоит ровно там, где потом окажется число.
 */
private val AmountSlotHeight: Dp = WexSpacing.xl

/** То же для второй строки: междустрочный интервал `type.amountSecondary`. */
private val FiatSlotHeight: Dp = WexSpacing.lg

/** Ширина заглушки баланса — примерно «1,5 ETH». */
private val AmountSkeletonWidth: Dp = WexSpacing.giant + WexSpacing.xl

/** Ширина заглушки стоимости — примерно «$3 210,45». */
private val FiatSkeletonWidth: Dp = WexSpacing.huge + WexSpacing.sm

private val AmountSkeletonHeight: Dp = WexSpacing.md

private val FiatSkeletonHeight: Dp = WexSpacing.sm

/**
 * Строка актива в списке кошелька.
 *
 * Числа прижаты к правому краю и набраны табличными цифрами: владелец сверяет
 * балансы столбцом сверху вниз, и разъезжающиеся разряды заставляют перечитывать
 * каждую строку заново.
 *
 * Пока баланс не пришёл из сети, на его месте стоят заглушки той же высоты,
 * что и будущие числа. Балансы приходят вразнобой, по одной сети за ответ, и без
 * зарезервированного места список подпрыгивал бы под пальцем — а промах открыл бы
 * отправку не того актива.
 *
 * На вход идут примитивы, а не модель актива: та же строка нужна и главному
 * экрану, и выбору актива в отправке, а эти экраны живут в разных модулях
 * и своих моделей друг другу не показывают.
 */
@Composable
fun WexAssetRow(
    symbol: String,
    network: String,
    amount: BigDecimal?,
    fiatValue: BigDecimal?,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WexTheme.touch.row)
            .pressable(enabled = onClick != null) { onClick?.invoke() }
            .clip(WexTheme.shapes.row)
            .background(WexTheme.colors.surface2)
            .padding(horizontal = WexTheme.spacing.lg, vertical = WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChainBadge(symbol = symbol)
        Spacer(Modifier.width(WexTheme.spacing.md))
        AssetTitles(
            symbol = symbol,
            network = network,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(WexTheme.spacing.sm))
        AssetValues(
            symbol = symbol,
            amount = amount,
            fiatValue = fiatValue,
            loading = loading,
        )
    }
}

/** Символ и сеть. Сеть обязательна: один и тот же USDT живёт сразу в нескольких. */
@Composable
private fun AssetTitles(
    symbol: String,
    network: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = symbol,
            style = WexTheme.type.headline,
            color = WexTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = network,
            style = WexTheme.type.label,
            color = WexTheme.colors.ink3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AssetValues(
    symbol: String,
    amount: BigDecimal?,
    fiatValue: BigDecimal?,
    loading: Boolean,
) {
    Column(horizontalAlignment = Alignment.End) {
        ValueSlot(height = AmountSlotHeight) {
            if (loading || amount == null) {
                Skeleton(width = AmountSkeletonWidth, height = AmountSkeletonHeight)
            } else {
                Text(
                    text = WexFormat.amount(amount, symbol),
                    style = WexTheme.type.amount,
                    color = WexTheme.colors.ink,
                    maxLines = 1,
                )
            }
        }
        ValueSlot(height = FiatSlotHeight) {
            FiatValue(fiatValue = fiatValue, loading = loading)
        }
    }
}

/**
 * Курса может не быть вовсе: у свежего токена нет пары на бирже. Прочерк честнее
 * пустоты — пустое место читается как не догрузившееся значение.
 */
@Composable
private fun FiatValue(fiatValue: BigDecimal?, loading: Boolean) {
    when {
        loading -> Skeleton(width = FiatSkeletonWidth, height = FiatSkeletonHeight)

        fiatValue == null -> Text(
            text = "—",
            style = WexTheme.type.amountSecondary,
            color = WexTheme.colors.ink4,
        )

        else -> Text(
            text = WexFormat.fiat(fiatValue),
            style = WexTheme.type.amountSecondary,
            color = WexTheme.colors.ink3,
            maxLines = 1,
        )
    }
}

/** Место постоянной высоты: заглушка и число занимают его одинаково. */
@Composable
private fun ValueSlot(height: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.height(height),
        contentAlignment = Alignment.CenterEnd,
    ) {
        content()
    }
}

@PreviewLightDark
@Composable
private fun WexAssetRowPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            WexAssetRow(
                symbol = "ETH",
                network = "Ethereum",
                amount = BigDecimal("1.482913"),
                fiatValue = BigDecimal("5412.08"),
            )
            Spacer(Modifier.height(WexTheme.spacing.rowGap))
            WexAssetRow(
                symbol = "USDT",
                network = "TON",
                amount = BigDecimal("240.5"),
                fiatValue = BigDecimal("240.62"),
            )
            Spacer(Modifier.height(WexTheme.spacing.rowGap))
            WexAssetRow(
                symbol = "BTC",
                network = "Bitcoin",
                amount = null,
                fiatValue = null,
                loading = true,
            )
        }
    }
}
