package wex.wallet.feature.activity

import wex.wallet.data.activity.ActivityFeed

data class ActivityUiState(
    val isLoading: Boolean = false,
    val feed: ActivityFeed = ActivityFeed.Empty,
    val query: String = "",
    val error: String? = null,
)
