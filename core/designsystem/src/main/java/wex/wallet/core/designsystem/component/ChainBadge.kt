@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wex.wallet.core.designsystem.theme.WexColors
import wex.wallet.core.designsystem.theme.WexTheme

/** Размер бейджа в строке актива. */
private val BadgeSize: Dp = 40.dp

/** Во сколько раз буква мельче кружка. Оптика внутри знака, к шкале отступов отношения не имеет. */
private const val LetterToBadgeRatio = 3.2f

/** Не больше трёх букв: «USDT» в кружке 40 dp уже не читается. */
private const val MaxLetters = 3

/**
 * Порог яркости подложки, за которым тёмная надпись становится контрастнее светлой.
 * Значение из WCAG: относительная яркость, дающая равный контраст с чёрным и белым.
 */
private const val ContrastPivot = 0.179f

/** EVM-мир пользователь воспринимает как одну среду, поэтому у L2 цвет тот же, что у эфира. */
/**
 * Канонические цвета монет.
 *
 * Раньше все EVM-активы красились одним цветом сети, и на главном экране POL,
 * BNB и ETH выглядели одинаково — бейдж, существующий ради различения, переставал
 * различать. Здесь цвет принадлежит активу, а не сети, и совпадает с тем, каким
 * его знают по биржам и другим кошелькам.
 */
private val AssetColors = mapOf(
    "ETH" to Color(0xFF627EEA),
    "WETH" to Color(0xFF627EEA),
    "BTC" to Color(0xFFF7931A),
    "WBTC" to Color(0xFFF7931A),
    "SOL" to Color(0xFF14F195),
    "TON" to Color(0xFF0098EA),
    "POL" to Color(0xFF8247E5),
    "MATIC" to Color(0xFF8247E5),
    "BNB" to Color(0xFFF3BA2F),
    "USDC" to Color(0xFF2775CA),
    "USDT" to Color(0xFF26A17B),
    "DAI" to Color(0xFFF5AC37),
)

private const val HashPrime = 31
private const val HueCount = 360
private const val FallbackSaturation = 0.58f
private const val FallbackLightnessDark = 0.55f
private const val FallbackLightnessLight = 0.42f

/**
 * Кружок с буквами тикера вместо логотипа.
 *
 * Логотипы пришлось бы тянуть по сети, а это и трафик, и утечка того, какими
 * активами владеет пользователь: по одному запросу картинки видно, что актив
 * лежит в кошельке. Поэтому знак рисуется на устройстве.
 *
 * Знакомые сети берут свой токен — эфир и его L2 циан, Solana и TON фиолет,
 * биткоин канонический оранжевый: цвет здесь работает как опознавательный знак,
 * и подмена его случайным сбивала бы проверку сети перед отправкой. Всё
 * остальное получает устойчивый оттенок из хеша символа, поэтому один и тот же
 * токен всегда выглядит одинаково, но ничего никуда не отправляется.
 */
@Composable
fun ChainBadge(
    symbol: String,
    modifier: Modifier = Modifier,
    size: Dp = BadgeSize,
) {
    val colors = WexTheme.colors
    val background = chainColor(symbol, colors)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol.take(MaxLetters).uppercase(),
            color = contrastInk(background, colors),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value / LetterToBadgeRatio).sp,
            style = WexTheme.type.label,
        )
    }
}

/** Цвет подложки: известный актив — свой канонический цвет, остальное — оттенок из символа. */
private fun chainColor(symbol: String, colors: WexColors): Color {
    val key = symbol.trim().uppercase()
    if (key.isEmpty()) return colors.ink3
    return AssetColors[key] ?: fallbackColor(key, colors)
}

/**
 * Оттенок выводится из хеша, а насыщенность и светлота заданы жёстко.
 *
 * Палитра из готовых цветов рано или поздно выдаёт незнакомому токену цвет
 * знакомой сети, и бейдж начинает врать. Фиксированная светлота вместо этого
 * гарантирует, что любой оттенок останется различимым и в тёмной теме, и в светлой.
 */
private fun fallbackColor(key: String, colors: WexColors): Color {
    val hash = key.fold(0) { acc, ch -> acc * HashPrime + ch.code }
    val lightness = if (colors.isDark) FallbackLightnessDark else FallbackLightnessLight
    return Color.hsl(
        hue = hash.mod(HueCount).toFloat(),
        saturation = FallbackSaturation,
        lightness = lightness,
    )
}

/**
 * Надпись на бейдже.
 *
 * Цвет сети от темы почти не зависит, поэтому контраст считается по яркости
 * подложки, а не по тому, тёмная сейчас тема или светлая: `onLime` тёмный
 * в обеих темах, `lime100` в обеих светлый.
 */
private fun contrastInk(background: Color, colors: WexColors): Color =
    if (background.luminance() > ContrastPivot) colors.onLime else colors.lime100

@PreviewLightDark
@Composable
private fun ChainBadgePreview() {
    WexTheme {
        Row(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm),
        ) {
            listOf("ETH", "BTC", "SOL", "TON", "ARB", "USDT", "PEPE").forEach { symbol ->
                ChainBadge(symbol = symbol)
            }
        }
    }
}
