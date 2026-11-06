@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import kotlinx.coroutines.delay
import wex.wallet.core.network.EvmRpcClient
import java.math.BigInteger

data class PendingTx(
    val txHash: String,
    val chainId: Long,
    val nonce: Long,
    val to: String,
    val value: BigInteger,
    val maxFeePerGas: BigInteger,
    val status: TxStatus = TxStatus.PENDING,
    val blockNumber: Long = 0,
)

enum class TxStatus { PENDING, CONFIRMED, FAILED }

class PendingTxTracker {

    private val pending = mutableListOf<PendingTx>()

    fun add(tx: PendingTx) {
        pending.add(tx)
    }

    fun allPending(): List<PendingTx> = pending.filter { it.status == TxStatus.PENDING }

    fun all(): List<PendingTx> = pending.toList()

    suspend fun checkReceipt(rpc: EvmRpcClient, txHash: String): TxStatus {
        val receipt = rpc.getTransactionReceipt(txHash) ?: return TxStatus.PENDING
        val newStatus = if (receipt.status) TxStatus.CONFIRMED else TxStatus.FAILED
        val idx = pending.indexOfFirst { it.txHash == txHash }
        if (idx >= 0) {
            pending[idx] = pending[idx].copy(
                status = newStatus,
                blockNumber = receipt.blockNumber,
            )
        }
        return newStatus
    }

    suspend fun pollUntilConfirmed(
        rpc: EvmRpcClient,
        txHash: String,
        intervalMs: Long = 3_000,
        maxAttempts: Int = 100,
    ): TxStatus {
        repeat(maxAttempts) {
            val status = checkReceipt(rpc, txHash)
            if (status != TxStatus.PENDING) return status
            delay(intervalMs)
        }
        return TxStatus.PENDING
    }

    fun remove(txHash: String) {
        pending.removeAll { it.txHash == txHash }
    }
}
