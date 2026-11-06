package wex.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.theme.WexTheme
import java.math.BigDecimal

private const val TeenFirst = 11
private const val TeenLast = 14
private const val Decade = 10
private const val Century = 100
private const val FewLast = 4

/**
 * Общий баланс кошелька.
 *
 * Единственная карточка на главном экране, ради которой приложение открывают
 * чаще всего. Сумма набрана `type.display` и меняется через [AnimatedBalance]:
 * балансы приходят из нескольких сетей вразнобой, и мгновенная подмена итога
 * читалась бы как сбой, а не как догрузка очередной сети.
 *
 * Строка «сетей · активов» стоит рядом с суммой намеренно: она объясняет, из чего
 * итог сложился, и заодно показывает, что учтено ещё не всё, если сетей меньше
 * ожидаемого.
 *
 * Изменение за сутки выводится через [WexFormat.signedFiat] — со знаком. Цвет
 * только усиливает знак и никогда его не заменяет: на монохромном скриншоте
 * и при дальтонизме «+12,50» и «−12,50» обязаны различаться текстом.
 */
@Composable
fun WexBalanceCard(
    totalFiat: BigDecimal,
    networkCount: Int,
    assetCount: Int,
    modifier: Modifier = Modifier,
    dailyChange: BigDecimal? = null,
    currency: String = "$",
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = WexTheme.shapes.card,
        contentPadding = PaddingValues(WexTheme.spacing.cardPadding),
        tint = WexTheme.colors.surface2,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Общий баланс",
                style = WexTheme.type.label,
                color = WexTheme.colors.ink3,
            )
            Spacer(Modifier.height(WexTheme.spacing.sm))
            AnimatedBalance(
                value = totalFiat.toDouble(),
                prefix = currency,
                style = WexTheme.type.display,
                color = WexTheme.colors.ink,
            )
            Spacer(Modifier.height(WexTheme.spacing.md))
            BalanceFooter(
                networkCount = networkCount,
                assetCount = assetCount,
                dailyChange = dailyChange,
            )
        }
    }
}

@Composable
private fun BalanceFooter(
    networkCount: Int,
    assetCount: Int,
    dailyChange: BigDecimal?,
) {
    val networks = plural(networkCount, "сеть", "сети", "сетей")
    val assets = plural(assetCount, "актив", "актива", "активов")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$networkCount $networks · $assetCount $assets",
            style = WexTheme.type.label,
            color = WexTheme.colors.ink3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(WexTheme.spacing.sm))
        if (dailyChange != null) {
            Text(
                text = WexFormat.signedFiat(dailyChange, currency = "$"),
                style = WexTheme.type.amount,
                color = if (dailyChange.signum() < 0) WexTheme.colors.negative else WexTheme.colors.positive,
            )
        }
    }
}

/**
 * Русский счётный падеж.
 *
 * «1 сетей» на главном экране кошелька читается как недоделка, а недоделка
 * в приложении с деньгами подрывает доверие ко всем остальным числам на экране.
 */
private fun plural(count: Int, one: String, few: String, many: String): String {
    val withinCentury = count % Century
    val withinDecade = count % Decade
    return when {
        withinCentury in TeenFirst..TeenLast -> many
        withinDecade == 1 -> one
        withinDecade in 2..FewLast -> few
        else -> many
    }
}

@PreviewLightDark
@Composable
private fun WexBalanceCardPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            WexBalanceCard(
                totalFiat = BigDecimal("12480.62"),
                networkCount = 4,
                assetCount = 11,
                dailyChange = BigDecimal("128.40"),
            )
            Spacer(Modifier.height(WexTheme.spacing.cardGap))
            WexBalanceCard(
                totalFiat = BigDecimal("312.05"),
                networkCount = 1,
                assetCount = 2,
                dailyChange = BigDecimal("-12.50"),
            )
        }
    }
}
