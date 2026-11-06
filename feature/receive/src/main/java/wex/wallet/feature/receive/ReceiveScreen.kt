package wex.wallet.feature.receive

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import wex.wallet.core.designsystem.component.ChainBadge
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.WexAddressFull
import wex.wallet.core.designsystem.component.WexEmptyState
import wex.wallet.core.designsystem.component.WexEmptyStateKind
import wex.wallet.core.designsystem.component.WexNotice
import wex.wallet.core.designsystem.component.WexNoticeTone
import wex.wallet.core.designsystem.theme.WexSpacing
import wex.wallet.core.designsystem.theme.WexTheme

/**
 * Сторона QR-кода.
 *
 * Меньше — и код перестают читать дешёвые камеры с другого телефона, а именно
 * так адрес и передают: экран показывают, а не пересылают файлом.
 */
private val QrSize: Dp = WexSpacing.giant * 4

/**
 * Тикеры сетей.
 *
 * [ChainBadge] узнаёт сеть по тикеру, а не по названию: «Ethereum» для него
 * незнакомая строка, и кружок получил бы случайный оттенок вместо цвета сети.
 */
private val ChainSymbols: Map<String, String> = mapOf(
    "ETHEREUM" to "ETH",
    "BASE" to "BASE",
    "BASE SEPOLIA" to "BASE",
    "ARBITRUM" to "ARB",
    "OPTIMISM" to "OP",
    "POLYGON" to "POL",
    "BNB CHAIN" to "BNB",
)

/**
 * Приём средств.
 *
 * Адрес показан полностью и группами по четыре знака: перед пополнением его
 * сверяют глазами, а сплошную строку из сорока знаков не сверяет никто.
 */
@Composable
fun ReceiveScreen(
    address: String,
    chainName: String,
    modifier: Modifier = Modifier,
    symbol: String = "",
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WexTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.cardGap),
    ) {
        if (address.isBlank()) {
            ReceiveAddressMissing(chainName = chainName)
        } else {
            ReceiveContent(address = address, chainName = chainName, symbol = symbol)
        }
    }
}

/**
 * Порядок блоков задан риском: сначала сеть и код, потом адрес, и только потом
 * предупреждение — оно остаётся на глазах в тот момент, когда пользователь
 * уходит вставлять скопированный адрес в чужое приложение.
 */
@Composable
private fun ReceiveContent(address: String, chainName: String, symbol: String) {
    val qrBitmap: Bitmap = remember(address) { QrCodeGenerator.generate(address) }
    val badgeSymbol = remember(chainName, symbol) { symbol.ifBlank { chainSymbol(chainName) } }

    ReceiveQrCard(chainName = chainName, symbol = badgeSymbol, qrBitmap = qrBitmap)
    ReceiveAddressCard(address = address, chainName = chainName)
    WexNotice(
        title = "Отправляйте на этот адрес только активы сети $chainName",
        tone = WexNoticeTone.Warning,
        message = "Монеты другой сети уйдут безвозвратно: такой перевод не отменить " +
            "и не вернуть — ни нам, ни бирже, ни поддержке.",
    )
}

/** Сеть и QR-код — один блок: код без названия сети опознать невозможно. */
@Composable
private fun ReceiveQrCard(chainName: String, symbol: String, qrBitmap: Bitmap) {
    val qrShape = RoundedCornerShape(
        WexTheme.radius.nested(WexTheme.radius.card, WexTheme.spacing.cardPadding),
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReceiveChainHeader(chainName = chainName, symbol = symbol)
            Spacer(Modifier.height(WexTheme.spacing.xl))
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR-код адреса в сети $chainName",
                modifier = Modifier
                    .size(QrSize)
                    .clip(qrShape),
            )
        }
    }
}

@Composable
private fun ReceiveChainHeader(chainName: String, symbol: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChainBadge(symbol = symbol)
        Spacer(Modifier.width(WexTheme.spacing.md))
        Column {
            Text(
                text = "Пополнение",
                style = WexTheme.type.label,
                color = WexTheme.colors.ink2,
            )
            Text(
                text = chainName,
                style = WexTheme.type.headline,
                color = WexTheme.colors.ink,
            )
        }
    }
}

@Composable
private fun ReceiveAddressCard(address: String, chainName: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Ваш адрес в сети $chainName",
                style = WexTheme.type.label,
                color = WexTheme.colors.ink2,
            )
            Spacer(Modifier.height(WexTheme.spacing.md))
            WexAddressFull(address = address, copyable = true)
        }
    }
}

/** Без адреса показывать нечего, и QR-код пустой строки был бы рабочим, но бессмысленным. */
@Composable
private fun ReceiveAddressMissing(chainName: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        WexEmptyState(
            title = "Адрес недоступен",
            reason = "Кошелёк ещё не выдал адрес в сети $chainName, поэтому показывать и копировать нечего.",
            kind = WexEmptyStateKind.Unavailable,
        )
    }
}

private fun chainSymbol(chainName: String): String =
    ChainSymbols[chainName.uppercase()] ?: chainName
