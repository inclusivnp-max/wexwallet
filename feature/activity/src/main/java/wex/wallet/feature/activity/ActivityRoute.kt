package wex.wallet.feature.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import wex.wallet.data.activity.ActivityFeed
import wex.wallet.data.activity.ActivityItem

/**
 * Держит состояние ленты и запускает загрузку. Экран остаётся stateless,
 * поэтому его можно рендерить в тестах и превью без сети.
 */
@Composable
fun ActivityRoute(
    address: String,
    nowSeconds: Long,
    loadFeed: suspend (String) -> ActivityFeed,
    onItemClick: (ActivityItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(ActivityUiState(isLoading = true)) }

    LaunchedEffect(address) {
        if (address.isBlank()) {
            state = state.copy(isLoading = false)
            return@LaunchedEffect
        }
        state = state.copy(isLoading = true, error = null)
        state = runCatching { loadFeed(address) }.fold(
            onSuccess = { state.copy(isLoading = false, feed = it) },
            onFailure = { state.copy(isLoading = false, error = it.message ?: "Не удалось загрузить") },
        )
    }

    ActivityScreen(
        state = state,
        nowSeconds = nowSeconds,
        onQueryChange = { state = state.copy(query = it) },
        onItemClick = onItemClick,
        onBack = onBack,
        modifier = modifier,
    )
}
