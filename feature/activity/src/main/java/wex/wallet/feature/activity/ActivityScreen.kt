@file:Suppress("MagicNumber")

package wex.wallet.feature.activity

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.WexGhostButton
import wex.wallet.core.designsystem.component.WexTextField
import wex.wallet.core.designsystem.component.pressable
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.motion.ScreenTransitions
import wex.wallet.core.designsystem.theme.WexSpacing
import wex.wallet.core.designsystem.theme.WexTheme
import wex.wallet.data.activity.ActivityItem
import wex.wallet.data.activity.ActivitySearch
import wex.wallet.data.activity.ChainActivityStatus

/**
 * Ширина кнопки возврата.
 *
 * Кнопка стоит вверху и не тянется во всю строку: полноширинное действие внизу
 * экрана читается как главное, а уход из истории главным действием не является.
 */
private val BackButtonWidth: Dp = WexSpacing.huge * 2

private enum class ActivitySection { Loading, Items, Empty }

private data class ActivityContent(
    val section: ActivitySection,
    val items: List<ActivityItem>,
    val state: ActivityUiState,
)

@Composable
fun ActivityScreen(
    state: ActivityUiState,
    nowSeconds: Long,
    onQueryChange: (String) -> Unit,
    onItemClick: (ActivityItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = ActivitySearch.apply(state.feed.items, state.query)
    val content = ActivityContent(
        section = when {
            state.isLoading -> ActivitySection.Loading
            visible.isEmpty() -> ActivitySection.Empty
            else -> ActivitySection.Items
        },
        items = visible,
        state = state,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = WexTheme.spacing.screen),
    ) {
        Spacer(Modifier.height(WexTheme.spacing.sm))
        WexGhostButton(
            text = "Назад",
            onClick = onBack,
            modifier = Modifier.width(BackButtonWidth),
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
        )
        Spacer(Modifier.height(WexTheme.spacing.sm))
        ActivityHeaderCard(query = state.query, onQueryChange = onQueryChange)
        Spacer(Modifier.height(WexTheme.spacing.cardGap))
        ActivityContentSwitcher(
            content = content,
            nowSeconds = nowSeconds,
            onItemClick = onItemClick,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.height(WexTheme.spacing.screen))
    }
}

/** Заголовок и поиск живут в одной карточке: это один смысловой блок — «что показываем». */
@Composable
private fun ActivityHeaderCard(query: String, onQueryChange: (String) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Активность",
                style = WexTheme.type.title,
                color = WexTheme.colors.ink,
            )
            Spacer(Modifier.height(WexTheme.spacing.md))
            WexTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Сеть, адрес, хеш или сумма",
                trailingIcon = {
                    ClearQueryButton(visible = query.isNotEmpty(), onClear = { onQueryChange("") })
                },
            )
        }
    }
}

/** Крестик появляется вместе с запросом и уходит вместе с ним — место под него не резервируется. */
@Composable
private fun ClearQueryButton(visible: Boolean, onClear: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter(Motion.Duration.QUICK)) + scaleIn(Motion.springy(), initialScale = 0.7f),
        exit = fadeOut(Motion.exit(Motion.Duration.INSTANT)) + scaleOut(Motion.exit(), targetScale = 0.7f),
    ) {
        Box(
            modifier = Modifier
                .size(WexTheme.touch.buttonSmall)
                .pressable(onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Clear,
                contentDescription = "Очистить поиск",
                tint = WexTheme.colors.ink3,
                modifier = Modifier.size(WexTheme.spacing.xl),
            )
        }
    }
}

/**
 * Кроссфейд вместо мгновенной подмены: скелетон и список занимают одно место,
 * и резкая замена читается как сбой отрисовки.
 */
@Composable
private fun ActivityContentSwitcher(
    content: ActivityContent,
    nowSeconds: Long,
    onItemClick: (ActivityItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = content,
        modifier = modifier,
        transitionSpec = { ScreenTransitions.crossfade() },
        label = "activity-content",
        contentKey = { it.section },
    ) { current ->
        when (current.section) {
            ActivitySection.Loading -> ActivitySkeletonList()
            ActivitySection.Empty -> ActivityEmptyBlock(
                state = current.state,
                hasQuery = current.state.query.isNotBlank(),
            )
            ActivitySection.Items -> ActivityList(
                items = current.items,
                nowSeconds = nowSeconds,
                unavailableChains = current.state.feed.unavailableChains,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun ActivityList(
    items: List<ActivityItem>,
    nowSeconds: Long,
    unavailableChains: List<ChainActivityStatus>,
    onItemClick: (ActivityItem) -> Unit,
) {
    val listShownAt = remember { SystemClock.uptimeMillis() }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.rowGap),
    ) {
        if (unavailableChains.isNotEmpty()) {
            item(key = "unavailable-chains") {
                UnavailableChainsNotice(chains = unavailableChains)
            }
        }
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            ActivityRow(
                item = item,
                nowSeconds = nowSeconds,
                index = index,
                listShownAt = listShownAt,
                onItemClick = onItemClick,
                modifier = Modifier.animateItem(
                    fadeInSpec = Motion.enter(),
                    placementSpec = Motion.standard(),
                    fadeOutSpec = Motion.exit(),
                ),
            )
        }
    }
}
