@file:Suppress("MagicNumber")

package wex.wallet.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import wex.wallet.core.designsystem.component.Skeleton
import wex.wallet.core.designsystem.theme.WexSpacing
import wex.wallet.core.designsystem.theme.WexTheme

/** Кружок сети: тот же размер, что у бейджа в настоящей строке. */
private val BadgeSize: Dp = WexSpacing.huge - WexSpacing.sm

/** Ширины заголовков разной длины: одинаковые полоски читаются как штрих-код, а не как список. */
private val TitleWidths = listOf(
    WexSpacing.giant * 2,
    WexSpacing.giant + WexSpacing.huge,
    WexSpacing.giant * 2 - WexSpacing.md,
)

/** Подпись сети с адресом и место под сумму: короче заголовка, как в настоящей строке. */
private val SubtitleWidth: Dp = WexSpacing.giant + WexSpacing.xxxl
private val AmountWidth: Dp = WexSpacing.giant + WexSpacing.sm

private const val SkeletonRows = 6

/**
 * Заглушка строки ленты.
 *
 * Повторяет геометрию настоящей строки вплоть до подложки и скругления, поэтому
 * при подстановке данных список не перестраивается: пользователь заранее видит,
 * что именно грузится, и не промахивается пальцем по уехавшей строке.
 */
@Composable
fun ActivitySkeletonRow(
    modifier: Modifier = Modifier,
    titleWidth: Dp = TitleWidths.first(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WexTheme.touch.row)
            .clip(WexTheme.shapes.row)
            .background(WexTheme.colors.surface2)
            .padding(horizontal = WexTheme.spacing.lg, vertical = WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Skeleton(width = BadgeSize, height = BadgeSize, cornerRadius = BadgeSize / 2)
        Spacer(Modifier.width(WexTheme.spacing.md))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.xs),
        ) {
            Skeleton(width = titleWidth, height = WexTheme.spacing.md)
            Skeleton(width = SubtitleWidth, height = WexTheme.spacing.md)
        }
        Spacer(Modifier.width(WexTheme.spacing.sm))
        Skeleton(width = AmountWidth, height = WexTheme.spacing.md)
    }
}

@Composable
internal fun ActivitySkeletonList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.rowGap),
    ) {
        repeat(SkeletonRows) { index ->
            ActivitySkeletonRow(titleWidth = TitleWidths[index % TitleWidths.size])
        }
    }
}
