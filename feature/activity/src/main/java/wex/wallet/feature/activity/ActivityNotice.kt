@file:Suppress("MagicNumber")

package wex.wallet.feature.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.WexEmptyState
import wex.wallet.core.designsystem.component.WexEmptyStateKind
import wex.wallet.core.designsystem.component.WexNotice
import wex.wallet.core.designsystem.component.WexNoticeTone
import wex.wallet.core.designsystem.theme.WexTheme
import wex.wallet.data.activity.ChainActivityStatus

/** Что показывать вместо списка: заголовок, причина и род пустоты. */
private data class ActivityEmptyContent(
    val title: String,
    val reason: String,
    val kind: WexEmptyStateKind,
)

/**
 * Пустое состояние обязано объяснять причину: «не смогли посмотреть» и
 * «транзакций нет» — разные вещи, и путать их нельзя. Первое означает, что на
 * счету может лежать невидимый перевод, второе — что ждать нечего.
 */
@Composable
internal fun ActivityEmptyBlock(state: ActivityUiState, hasQuery: Boolean) {
    val content = emptyContent(state, hasQuery)
    val chains = state.feed.unavailableChains
    Column(modifier = Modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            WexEmptyState(
                title = content.title,
                reason = content.reason,
                kind = content.kind,
            )
        }
        // Когда причина уже перечислена в самом пустом состоянии, повторять её нечем.
        if (!hasQuery && chains.isNotEmpty() && !state.feed.allUnavailable) {
            Spacer(Modifier.height(WexTheme.spacing.cardGap))
            UnavailableChainsNotice(chains = chains)
        }
    }
}

/**
 * Причина берётся из данных: текст ошибки загрузки или ответ конкретной сети.
 * Придумывать её нельзя — по ней пользователь решает, повторить или ждать.
 */
private fun emptyContent(state: ActivityUiState, hasQuery: Boolean): ActivityEmptyContent = when {
    hasQuery -> ActivityEmptyContent(
        title = "Ничего не найдено",
        reason = "По запросу «${state.query.trim()}» совпадений нет. Ищем по сети, адресу, хешу и сумме.",
        kind = WexEmptyStateKind.Empty,
    )

    state.error != null -> ActivityEmptyContent(
        title = "История не загрузилась",
        reason = state.error,
        kind = WexEmptyStateKind.Unavailable,
    )

    state.feed.allUnavailable -> ActivityEmptyContent(
        title = "История недоступна",
        reason = chainReasons(state.feed.unavailableChains),
        kind = WexEmptyStateKind.Unavailable,
    )

    else -> ActivityEmptyContent(
        title = "Операций пока нет",
        reason = "Первая появится здесь, как только вы отправите или получите монеты.",
        kind = WexEmptyStateKind.Empty,
    )
}

/**
 * Недоступные сети — не примечание мелким шрифтом.
 *
 * Серый текст под списком пользователь прочитает как «операций нет», хотя на
 * деле часть истории просто не пришла. Поэтому отдельный блок с перечислением
 * сетей и причиной по каждой.
 */
@Composable
internal fun UnavailableChainsNotice(
    chains: List<ChainActivityStatus>,
    modifier: Modifier = Modifier,
) {
    WexNotice(
        title = "Показаны не все операции",
        modifier = modifier,
        tone = WexNoticeTone.Info,
        message = chainReasons(chains),
    )
}

/** Сеть и её ответ через двоеточие, по строке на сеть. */
private fun chainReasons(chains: List<ChainActivityStatus>): String =
    chains.joinToString(separator = "\n") { chain ->
        listOfNotNull(chain.networkName, chain.unavailableReason).joinToString(separator = ": ")
    }
