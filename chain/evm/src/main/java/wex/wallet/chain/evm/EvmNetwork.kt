@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

data class EvmNetwork(
    val chainId: Long,
    val name: String,
    val symbol: String,
    val decimals: Int = 18,
    val isTestnet: Boolean = false,
    val blockExplorerUrl: String = "",
) {
    companion object {
        val ETHEREUM = EvmNetwork(
            chainId = 1,
            name = "Ethereum",
            symbol = "ETH",
            blockExplorerUrl = "https://etherscan.io",
        )
        val BASE = EvmNetwork(
            chainId = 8453,
            name = "Base",
            symbol = "ETH",
            blockExplorerUrl = "https://basescan.org",
        )
        val ARBITRUM = EvmNetwork(
            chainId = 42161,
            name = "Arbitrum",
            symbol = "ETH",
            blockExplorerUrl = "https://arbiscan.io",
        )
        val OPTIMISM = EvmNetwork(
            chainId = 10,
            name = "Optimism",
            symbol = "ETH",
            blockExplorerUrl = "https://optimistic.etherscan.io",
        )
        val POLYGON = EvmNetwork(
            chainId = 137,
            name = "Polygon",
            symbol = "POL",
            blockExplorerUrl = "https://polygonscan.com",
        )
        val BSC = EvmNetwork(
            chainId = 56,
            name = "BNB Chain",
            symbol = "BNB",
            blockExplorerUrl = "https://bscscan.com",
        )
        val BASE_SEPOLIA = EvmNetwork(
            chainId = 84532,
            name = "Base Sepolia",
            symbol = "ETH",
            isTestnet = true,
            blockExplorerUrl = "https://sepolia.basescan.org",
        )

        val MAINNETS = listOf(ETHEREUM, BASE, ARBITRUM, OPTIMISM, POLYGON, BSC)
        val ALL = MAINNETS + BASE_SEPOLIA

        fun byChainId(chainId: Long): EvmNetwork? = ALL.find { it.chainId == chainId }
    }

    fun explorerTxUrl(txHash: String): String = "$blockExplorerUrl/tx/$txHash"
    fun explorerAddressUrl(address: String): String = "$blockExplorerUrl/address/$address"
}
