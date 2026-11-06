@file:Suppress("MagicNumber")

package wex.wallet.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import wex.wallet.core.designsystem.component.BrandMark
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.IconTile
import wex.wallet.core.designsystem.component.WexGhostButton
import wex.wallet.core.designsystem.component.WexPrimaryButton
import wex.wallet.core.designsystem.component.WexSecondaryButton
import wex.wallet.core.designsystem.motion.Motion

private data class WalletFeature(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

private val Features = listOf(
    WalletFeature(
        icon = Icons.Rounded.Lock,
        title = "Ключи только у вас",
        subtitle = "Seed-фраза шифруется на устройстве и никуда не отправляется",
    ),
    WalletFeature(
        icon = Icons.Rounded.Shield,
        title = "Понятная подпись",
        subtitle = "Перед отправкой видно, что уходит, куда и сколько стоит",
    ),
    WalletFeature(
        icon = Icons.Rounded.Bolt,
        title = "Четыре сети сразу",
        subtitle = "EVM, Bitcoin, Solana и TON в одном кошельке",
    ),
)

@Composable
fun WelcomeScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit,
    modifier: Modifier = Modifier,
    /** Вход в витрину компонентов. Передаётся только в отладочной сборке. */
    onOpenGallery: (() -> Unit)? = null,
) {
    var stage by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        // Экран собирается по частям: знак, обещание, доводы, действия.
        // Одновременное появление читалось бы как рывок отрисовки.
        repeat(4) {
            stage = it + 1
            delay(90)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeHero(stage = stage, modifier = Modifier.weight(1f))

        Spacer(Modifier.height(28.dp))

        RiseIn(visible = stage >= 4) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WexPrimaryButton(
                    text = "Создать кошелёк",
                    icon = Icons.Rounded.Add,
                    onClick = onCreateWallet,
                )
                WexSecondaryButton(
                    text = "Восстановить по фразе",
                    icon = Icons.Rounded.Download,
                    onClick = onImportWallet,
                )
                if (onOpenGallery != null) {
                    WexGhostButton(text = "Витрина компонентов", onClick = onOpenGallery)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WelcomeHero(stage: Int, modifier: Modifier = Modifier) {
    // Блок центрируется в свободном месте: на высоких экранах прижатый
    // к верху заголовок оставлял бы посреди экрана пустоту.
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RiseIn(visible = stage >= 1) {
            BrandMark(size = 92.dp)
        }

        Spacer(Modifier.height(24.dp))

        RiseIn(visible = stage >= 2) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Wex Wallet",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Некастодиальный мультичейн-кошелёк",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        RiseIn(visible = stage >= 3) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Features.forEach { FeatureRow(it) }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(feature: WalletFeature) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(size = 42.dp) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = feature.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun RiseIn(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter()) + slideInVertically(Motion.enter()) { it / 3 },
    ) {
        content()
    }
}
