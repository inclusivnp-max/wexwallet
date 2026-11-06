@file:Suppress("MagicNumber")

package wex.wallet.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.IconTile
import wex.wallet.core.designsystem.component.WexAssetRow
import wex.wallet.core.designsystem.component.WexBalanceCard
import wex.wallet.core.designsystem.component.WexEmptyState
import wex.wallet.core.designsystem.component.WexEmptyStateKind
import wex.wallet.core.designsystem.component.WexTextField
import wex.wallet.core.designsystem.component.pressable
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme
import java.math.BigDecimal

private const val ROW_STAGGER_MS = 40

/** Потолок задержки: без него хвост длинного списка выезжал бы уже после того, как по нему проскроллили. */
private const val ROW_STAGGER_CAP_MS = 300

/**
 * Главный экран кошелька.
 *
 * Весь экран — один список, включая карточку баланса и действия. Отдельная шапка
 * над прокруткой съедала бы треть экрана на телефоне: до активов приходилось
 * тянуться, а ради них экран и открывают.
 */
@Composable
fun HomeScreen(
    assets: List<AssetItem>,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onActivity: () -> Unit = {},
    onAssetClick: (AssetItem) -> Unit = {},
    modifier: Modifier = Modifier,
    totalUsd: String = "",
    query: String = "",
    onQueryChange: (String) -> Unit = {},
) {
    val visible = AssetFilter.apply(assets, query)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = WexTheme.spacing.screen,
            end = WexTheme.spacing.screen,
            top = WexTheme.spacing.xxl,
            bottom = WexTheme.spacing.section,
        ),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.rowGap),
    ) {
        item(key = "balance") {
            HomeBalance(assets = assets, totalUsd = totalUsd)
        }
        item(key = "actions") {
            HomeActions(onSend = onSend, onReceive = onReceive, onActivity = onActivity)
        }
        item(key = "assets-header") {
            AssetsSection(count = visible.size, query = query, onQueryChange = onQueryChange)
        }
        assetItems(assets = visible, query = query, onAssetClick = onAssetClick)
    }
}

/**
 * Итог приходит готовой строкой от вызывающего кода. Неразобранный итог
 * показывается нулём: карточка остаётся на месте, и балансы сетей доезжают в неё
 * анимацией, а не подменяют экран целиком.
 */
@Composable
private fun HomeBalance(assets: List<AssetItem>, totalUsd: String) {
    val total = parseTotal(totalUsd)
    WexBalanceCard(
        totalFiat = total,
        networkCount = assets.countNetworks(),
        assetCount = assets.size,
    )
}

private fun parseTotal(totalUsd: String): BigDecimal = parseDecimal(totalUsd) ?: BigDecimal.ZERO

@Composable
private fun HomeActions(onSend: () -> Unit, onReceive: () -> Unit, onActivity: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm),
    ) {
        ActionTile(
            icon = Icons.Rounded.ArrowUpward,
            label = "Отправить",
            onClick = onSend,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            icon = Icons.Rounded.ArrowDownward,
            label = "Получить",
            onClick = onReceive,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            icon = Icons.Rounded.History,
            label = "История",
            onClick = onActivity,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Плитка действия. Радиус строки, а не карточки: три плитки в ряд стоят ниже
 * карточки баланса, и одинаковый с ней радиус сделал бы ряд второй шапкой.
 */
@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.pressable(onClick = onClick),
        shape = RoundedCornerShape(WexTheme.radius.row),
        contentPadding = PaddingValues(
            horizontal = WexTheme.spacing.sm,
            vertical = WexTheme.spacing.md,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconTile {
                Icon(imageVector = icon, contentDescription = null)
            }
            Spacer(Modifier.height(WexTheme.spacing.sm))
            Text(
                text = label,
                style = WexTheme.type.label,
                color = WexTheme.colors.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Заголовок раздела и поиск стоят в одной карточке: поиск ищет ровно по тому, что названо выше. */
@Composable
private fun AssetsSection(count: Int, query: String, onQueryChange: (String) -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(WexTheme.spacing.lg),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AssetsHeader(count = count)
            Spacer(Modifier.height(WexTheme.spacing.md))
            AssetSearchField(query = query, onQueryChange = onQueryChange)
        }
    }
}

/**
 * Сколько за списком стоит блокчейнов.
 *
 * Считать строки нельзя: у Bitcoin два типа адресов, Segwit и Taproot, и это
 * по-прежнему одна сеть. Иначе подпись «10 сетей · 10 активов» дважды повторяет
 * одно число и ничего не сообщает.
 */
private fun List<AssetItem>.countNetworks(): Int =
    map { it.chainLabel.substringBefore(" (").trim() }.distinct().size

@Composable
private fun AssetsHeader(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Активы",
            style = WexTheme.type.caption,
            color = WexTheme.colors.ink2,
        )
        Spacer(Modifier.width(WexTheme.spacing.sm))
        Text(
            text = count.toString(),
            style = WexTheme.type.caption,
            color = WexTheme.colors.ink4,
        )
    }
}

@Composable
private fun AssetSearchField(query: String, onQueryChange: (String) -> Unit) {
    WexTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "Поиск по активам",
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = WexTheme.colors.ink3,
            )
        },
        trailingIcon = {
            ClearQueryButton(visible = query.isNotEmpty(), onClear = { onQueryChange("") })
        },
    )
}

/**
 * Крестик появляется только при непустом запросе: пустое поле нечего очищать,
 * а постоянная кнопка в углу поля читается как отмена всего поиска.
 */
@Composable
private fun ClearQueryButton(visible: Boolean, onClear: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter(Motion.Duration.QUICK)) + scaleIn(Motion.springy(), initialScale = 0.7f),
        exit = fadeOut(Motion.exit(Motion.Duration.INSTANT)) + scaleOut(Motion.exit(), targetScale = 0.7f),
    ) {
        Box(
            modifier = Modifier
                .size(WexTheme.touch.minimum)
                .pressable(onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Очистить поиск",
                tint = WexTheme.colors.ink3,
            )
        }
    }
}

private fun LazyListScope.assetItems(
    assets: List<AssetItem>,
    query: String,
    onAssetClick: (AssetItem) -> Unit,
) {
    if (assets.isEmpty()) {
        item(key = "assets-empty") { AssetsEmpty(query = query) }
        return
    }
    itemsIndexed(assets) { index, asset ->
        StaggeredAssetRow(asset = asset, index = index, onAssetClick = onAssetClick)
    }
}

/**
 * Пусто по двум разным причинам, и разводить их обязательно: под запросом активов
 * нет, потому что их нет в кошельке, а без запроса список пуст, пока сети не ответили.
 * Второе — не ошибка сети: сюда приходит уже полученный список, поэтому вид один.
 */
@Composable
private fun AssetsEmpty(query: String) {
    val searching = query.isNotBlank()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        WexEmptyState(
            title = if (searching) "Ничего не найдено" else "Активов пока нет",
            reason = if (searching) {
                "По запросу «$query» активов нет. Проверьте символ, сеть или адрес контракта."
            } else {
                "Балансы ещё не пришли из сетей. Как только сети ответят, активы появятся здесь."
            },
            kind = WexEmptyStateKind.Empty,
        )
    }
}

@Composable
private fun StaggeredAssetRow(asset: AssetItem, index: Int, onAssetClick: (AssetItem) -> Unit) {
    // Сохранённый флаг не даёт строке проигрывать появление заново при каждом возврате скроллом.
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * ROW_STAGGER_MS).coerceAtMost(ROW_STAGGER_CAP_MS).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = Motion.enter()) +
            slideInVertically(animationSpec = Motion.enter()) { height -> height / 3 },
    ) {
        WexAssetRow(
            symbol = asset.symbol,
            network = asset.chainLabel,
            amount = asset.amountOrNull(),
            fiatValue = asset.fiatOrNull(),
            loading = asset.isLoading,
            onClick = { onAssetClick(asset) },
        )
    }
}
