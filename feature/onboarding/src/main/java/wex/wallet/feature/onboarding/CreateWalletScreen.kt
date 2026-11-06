@file:Suppress("MagicNumber")

package wex.wallet.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.WexNotice
import wex.wallet.core.designsystem.component.WexNoticeTone
import wex.wallet.core.designsystem.component.WexPrimaryButton
import wex.wallet.core.designsystem.component.WexSeedChip
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme
import wex.wallet.security.mnemonic.Mnemonic

private const val WordStaggerMs = 25L
private const val MnemonicWordCount = 24
private const val SeedColumns = 3

@Composable
fun CreateWalletScreen(
    onProceed: (words: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var words by remember { mutableStateOf(emptyList<String>()) }

    DisposableEffect(Unit) {
        words = Mnemonic.generate(MnemonicWordCount)
        onDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WexTheme.spacing.screen),
    ) {
        Spacer(Modifier.height(WexTheme.spacing.section))
        RecoveryPhraseHeader()

        Spacer(Modifier.height(WexTheme.spacing.lg))
        WexNotice(
            title = "Запишите её на бумаге",
            tone = WexNoticeTone.Danger,
            message = "Кто угодно с этой фразой заберёт все средства. Мы не сможем её восстановить",
        )

        Spacer(Modifier.height(WexTheme.spacing.lg))
        if (words.isNotEmpty()) {
            MnemonicCard(words = words)
        }

        Spacer(Modifier.height(WexTheme.spacing.beforeCta))
        WexPrimaryButton(
            text = "Я записал фразу",
            onClick = { onProceed(words) },
            modifier = Modifier.fillMaxWidth(),
            enabled = words.isNotEmpty(),
        )
        Spacer(Modifier.height(WexTheme.spacing.section))
    }
}

@Composable
private fun RecoveryPhraseHeader() {
    Text(
        text = "Ваша фраза восстановления",
        style = WexTheme.type.title,
        color = WexTheme.colors.ink,
    )
    Spacer(Modifier.height(WexTheme.spacing.sm))
    Text(
        text = "Слова и их порядок — единственный способ вернуть доступ к кошельку",
        style = WexTheme.type.body,
        color = WexTheme.colors.ink2,
    )
}

/**
 * Слова разложены по трём колонкам: так вся фраза помещается на экран целиком
 * и переписывать её можно, не прокручивая список туда-обратно.
 */
@Composable
private fun MnemonicCard(words: List<String>, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
            words.chunked(SeedColumns).forEachIndexed { rowIndex, rowWords ->
                SeedRow(firstNumber = rowIndex * SeedColumns, words = rowWords)
            }
        }
    }
}

@Composable
private fun SeedRow(firstNumber: Int, words: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm),
    ) {
        words.forEachIndexed { column, word ->
            SeedCell(
                index = firstNumber + column,
                word = word,
                modifier = Modifier.weight(1f),
            )
        }
        // Неполный последний ряд не растягивает свои ячейки на всю ширину.
        repeat(SeedColumns - words.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SeedCell(index: Int, word: String, modifier: Modifier = Modifier) {
    val appearance = cascadeProgress(index)
    val rise = WexTheme.spacing.sm

    WexSeedChip(
        number = index + 1,
        word = word,
        modifier = modifier.graphicsLayer {
            alpha = appearance
            translationY = (1f - appearance) * rise.toPx()
        },
    )
}

/**
 * Слова выкладываются каскадом: порядок появления повторяет порядок записи,
 * поэтому фразу проще переписывать, не теряя строку.
 */
@Composable
private fun cascadeProgress(index: Int): Float {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * WordStaggerMs)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = Motion.enter(Motion.Duration.QUICK),
        label = "wordCascade",
    )
    return progress
}
