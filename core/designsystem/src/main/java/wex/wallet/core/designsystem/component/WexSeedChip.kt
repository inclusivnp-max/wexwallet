package wex.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import wex.wallet.core.designsystem.theme.WexTheme

/**
 * Длина маски скрытого слова.
 *
 * Число точек постоянно и с длиной слова не связано. Слова BIP-39 длиной от трёх
 * до восьми букв, и точная длина заметно сужает перебор для того, кто смотрит
 * через плечо: показывать её ради красоты не стоит.
 */
private const val HiddenWordLength = 6

private const val HiddenWordMask = "•"

/**
 * Слово seed-фразы с порядковым номером.
 *
 * Номер приглушён до [WexTheme] `ink3`: при записи на бумагу читают слово,
 * а номер нужен лишь для того, чтобы не сбиться со строки. Если номер набрать
 * тем же весом, что и слово, глаз начинает спотыкаться на каждой ячейке.
 *
 * Слово идёт моноширинным: похожие в пропорциональном наборе `rn` и `m`, `l`
 * и `I` разъезжаются, а ошибка в одной букве стоит доступа ко всем средствам.
 *
 * @param hidden показывает маску вместо слова, пока владелец не подтвердил,
 * что рядом никого нет.
 */
@Composable
fun WexSeedChip(
    number: Int,
    word: String,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
) {
    Row(
        modifier = modifier
            .heightIn(min = WexTheme.touch.buttonSmall)
            .clip(WexTheme.shapes.chip)
            .background(WexTheme.colors.surface2)
            .padding(horizontal = WexTheme.spacing.md, vertical = WexTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString(),
            style = WexTheme.type.caption,
            color = WexTheme.colors.ink3,
            textAlign = TextAlign.End,
            // Номера выровнены по правому краю: одно- и двузначные не сдвигают слова.
            modifier = Modifier.widthIn(min = WexTheme.spacing.lg),
        )
        Spacer(Modifier.width(WexTheme.spacing.sm))
        Text(
            text = if (hidden) HiddenWordMask.repeat(HiddenWordLength) else word,
            style = WexTheme.type.mono,
            color = if (hidden) WexTheme.colors.ink3 else WexTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewLightDark
@Composable
private fun WexSeedChipPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
                WexSeedChip(number = 1, word = "abandon")
                WexSeedChip(number = 12, word = "zoo")
            }
            Spacer(Modifier.height(WexTheme.spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
                WexSeedChip(number = 1, word = "abandon", hidden = true)
                WexSeedChip(number = 12, word = "zoo", hidden = true)
            }
        }
    }
}
