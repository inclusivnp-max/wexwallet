package wex.wallet.data.activity

/**
 * Почему по конкретной сети нет данных.
 *
 * [unavailableReason] != null означает «не смогли посмотреть», а не «пусто».
 * Разница обязана быть видна пользователю.
 */
data class ChainActivityStatus(
    val chainId: Long,
    val networkName: String,
    val unavailableReason: String? = null,
) {
    val isAvailable: Boolean get() = unavailableReason == null
}

data class ActivityFeed(
    val items: List<ActivityItem> = emptyList(),
    val chains: List<ChainActivityStatus> = emptyList(),
    val hiddenSpamCount: Int = 0,
) {
    val hasAnyData: Boolean get() = items.isNotEmpty()

    /** Ни одна сеть не ответила — показываем причину, а не «транзакций нет». */
    val allUnavailable: Boolean
        get() = chains.isNotEmpty() && chains.none { it.isAvailable }

    val unavailableChains: List<ChainActivityStatus>
        get() = chains.filterNot { it.isAvailable }

    companion object {
        val Empty = ActivityFeed()
    }
}
