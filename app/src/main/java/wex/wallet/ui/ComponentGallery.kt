@file:Suppress("MagicNumber")

package wex.wallet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import wex.wallet.core.designsystem.component.ChainBadge
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.WexGhostButton
import wex.wallet.core.designsystem.component.WexNotice
import wex.wallet.core.designsystem.component.WexNoticeTone
import wex.wallet.core.designsystem.component.WexPill
import wex.wallet.core.designsystem.component.WexPillTone
import wex.wallet.core.designsystem.component.WexPrimaryButton
import wex.wallet.core.designsystem.component.WexSecondaryButton
import wex.wallet.core.designsystem.component.WexStepper
import wex.wallet.core.designsystem.component.WexTextField
import wex.wallet.core.designsystem.theme.WexTheme

/**
 * Витрина компонентов.
 *
 * Экран существует ради проверки: собранный на устройстве список — единственный
 * способ увидеть, как токены ведут себя на реальной плотности экрана и поверх
 * подвижного фона. В сборку для магазина не попадает.
 */
@Composable
fun ComponentGallery(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = WexTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.cardGap),
    ) {
        item { GallerySection("Кнопки") { GalleryButtons() } }
        item { GallerySection("Поле ввода") { GalleryFields() } }
        item { GallerySection("Статусы") { GalleryPills() } }
        item { GallerySection("Уведомления") { GalleryNotices() } }
        item { GallerySection("Сети") { GalleryBadges() } }
        item { GallerySection("Шаги отправки") { WexStepper(totalSteps = 4, currentStep = 2) } }
        item {
            WexGhostButton(text = "Назад", onClick = onBack)
        }
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
        Text(
            text = title,
            style = WexTheme.type.caption,
            color = WexTheme.colors.ink3,
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.md)) {
                content()
            }
        }
    }
}

@Composable
private fun GalleryButtons() {
    WexPrimaryButton(text = "Отправить 0,5 ETH", onClick = {})
    WexPrimaryButton(text = "Недостаточно средств", onClick = {}, enabled = false)
    WexSecondaryButton(text = "Восстановить по фразе", onClick = {})
    WexGhostButton(text = "Пропустить", onClick = {})
}

@Composable
private fun GalleryFields() {
    var address by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf("0xZZZ") }

    WexTextField(
        value = address,
        onValueChange = { address = it },
        label = "Получатель",
        placeholder = "bc1q… или адрес 0x…",
        monospace = true,
    )
    WexTextField(
        value = wrong,
        onValueChange = { wrong = it },
        label = "Получатель",
        isError = true,
        errorText = "Это не похоже на адрес Ethereum",
        monospace = true,
    )
}

@Composable
private fun GalleryPills() {
    Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
        WexPill(text = "Подтверждено", tone = WexPillTone.Ok)
        WexPill(text = "В обработке", tone = WexPillTone.Warning)
        WexPill(text = "Отклонено", tone = WexPillTone.Danger)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
        WexPill(text = "Base Sepolia", tone = WexPillTone.Muted)
        WexPill(text = "Тестовая сеть", tone = WexPillTone.Info)
    }
}

@Composable
private fun GalleryNotices() {
    WexNotice(
        tone = WexNoticeTone.Danger,
        title = "Похоже на адрес из вашей истории",
        message = "Отличается двумя символами в середине. Сверьте получателя целиком.",
    )
    WexNotice(
        tone = WexNoticeTone.Warning,
        title = "Первый перевод на этот адрес",
        message = "Проверьте его перед отправкой: транзакцию нельзя отменить.",
    )
    WexNotice(
        tone = WexNoticeTone.Info,
        title = "История недоступна на Base",
        message = "Бесплатный тариф Etherscan не покрывает эту сеть.",
    )
}

@Composable
private fun GalleryBadges() {
    Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
        ChainBadge(symbol = "ETH")
        ChainBadge(symbol = "BTC")
        ChainBadge(symbol = "SOL")
        ChainBadge(symbol = "TON")
        ChainBadge(symbol = "USDC")
        ChainBadge(symbol = "PEPE")
    }
}
