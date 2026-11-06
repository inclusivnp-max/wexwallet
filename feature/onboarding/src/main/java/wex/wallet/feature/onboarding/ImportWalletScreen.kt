@file:Suppress("MagicNumber")

package wex.wallet.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.WexPrimaryButton
import wex.wallet.core.designsystem.component.WexTextField
import wex.wallet.core.designsystem.theme.WexTheme
import wex.wallet.security.mnemonic.Mnemonic

private val ValidWordCounts = setOf(12, 15, 18, 21, 24)
private const val FullWordCount = 24
private const val InvalidPhraseText =
    "Такой фразы не существует. Проверьте слова и их порядок"

/**
 * Подсказки и автозамена превратили бы слово BIP-39 в похожее словарное,
 * а ошибка в одном слове стоит доступа ко всем средствам.
 */
private val SeedKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    imeAction = ImeAction.Done,
)

@Composable
fun ImportWalletScreen(
    onProceed: (words: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }

    val words = input.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    val isValid = words.size in ValidWordCounts && Mnemonic.isValid(words)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = WexTheme.spacing.screen),
    ) {
        Spacer(Modifier.height(WexTheme.spacing.section))
        ImportHeader()

        Spacer(Modifier.height(WexTheme.spacing.lg))
        PhraseInputCard(
            input = input,
            wordCount = words.size,
            invalid = invalid,
            onInputChange = {
                input = it
                invalid = false
            },
        )

        Spacer(Modifier.weight(1f))
        WexPrimaryButton(
            text = "Продолжить",
            onClick = { if (isValid) onProceed(words) else invalid = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(WexTheme.spacing.section))
    }
}

@Composable
private fun ImportHeader() {
    Text(
        text = "Восстановление кошелька",
        style = WexTheme.type.title,
        color = WexTheme.colors.ink,
    )
    Spacer(Modifier.height(WexTheme.spacing.sm))
    Text(
        text = "Введите фразу восстановления: слова через пробел, в том же порядке",
        style = WexTheme.type.body,
        color = WexTheme.colors.ink2,
    )
}

@Composable
private fun PhraseInputCard(
    input: String,
    wordCount: Int,
    invalid: Boolean,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            WexTextField(
                value = input,
                onValueChange = onInputChange,
                label = "Фраза восстановления",
                placeholder = "abandon ability able ...",
                isError = invalid,
                errorText = InvalidPhraseText,
                singleLine = false,
                keyboardOptions = SeedKeyboard,
                monospace = true,
            )
            Spacer(Modifier.height(WexTheme.spacing.sm))
            WordCounter(wordCount = wordCount)
        }
    }
}

/** Счётчик заменяет проверку вслух: сбитое количество слов видно до нажатия кнопки. */
@Composable
private fun WordCounter(wordCount: Int) {
    Text(
        text = "введено $wordCount из $FullWordCount слов",
        style = WexTheme.type.caption,
        color = if (wordCount in ValidWordCounts) WexTheme.colors.ink2 else WexTheme.colors.ink3,
    )
}
