package wex.wallet.data.activity

/** Поиск по ленте. Чистая функция — состояние запроса живёт в UI. */
object ActivitySearch {

    fun apply(items: List<ActivityItem>, query: String): List<ActivityItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return items
        return items.filter { matches(it, normalized) }
    }

    fun matches(item: ActivityItem, normalizedQuery: String): Boolean {
        if (normalizedQuery.isEmpty()) return true
        return item.symbol.lowercase().contains(normalizedQuery) ||
            item.networkName.lowercase().contains(normalizedQuery) ||
            item.counterparty.lowercase().contains(normalizedQuery) ||
            item.txHash.lowercase().contains(normalizedQuery) ||
            item.methodLabel.lowercase().contains(normalizedQuery) ||
            item.displayAmount.toPlainString().contains(normalizedQuery) ||
            item.contractAddress?.lowercase()?.contains(normalizedQuery) == true
    }
}
