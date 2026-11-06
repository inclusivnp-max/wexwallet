package wex.wallet.core.network

import java.io.Closeable

class EvmRpcRegistry(private val endpoints: Map<Long, String>) : Closeable {

    private val clients = mutableMapOf<Long, EvmRpcClient>()

    fun clientForChain(chainId: Long): EvmRpcClient {
        return clients.getOrPut(chainId) {
            val url = endpoints[chainId]
                ?: error("No RPC endpoint configured for chainId=$chainId")
            EvmRpcClient(JsonRpcClient(url))
        }
    }

    override fun close() {
        clients.values.forEach { /* JsonRpcClient is Closeable */ }
        clients.clear()
    }
}
