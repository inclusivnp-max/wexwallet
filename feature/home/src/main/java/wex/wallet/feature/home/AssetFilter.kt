package wex.wallet.feature.home

/** Поиск по активам. Чистая функция — состояние запроса поднято в вызывающий код. */
object AssetFilter {

    fun apply(assets: List<AssetItem>, query: String): List<AssetItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return assets
        return assets.filter { matches(it, normalized) }
    }

    fun matches(asset: AssetItem, normalizedQuery: String): Boolean {
        if (normalizedQuery.isEmpty()) return true
        return asset.symbol.lowercase().contains(normalizedQuery) ||
            asset.name.lowercase().contains(normalizedQuery) ||
            asset.chainLabel.lowercase().contains(normalizedQuery) ||
            asset.contractAddress?.lowercase()?.contains(normalizedQuery) == true
    }
}
