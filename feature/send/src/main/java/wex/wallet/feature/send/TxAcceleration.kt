@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import wex.wallet.chain.evm.EvmSigner
import wex.wallet.chain.evm.EvmTransaction
import wex.wallet.core.network.EvmRpcClient
import java.math.BigInteger

object TxAcceleration {

    private val SPEED_UP_MULTIPLIER = BigInteger.valueOf(110)
    private val PERCENTAGE_DIVISOR = BigInteger.valueOf(100)

    fun speedUpGas(original: PendingTx): Pair<BigInteger, BigInteger> {
        val newMaxFee = original.maxFeePerGas * SPEED_UP_MULTIPLIER / PERCENTAGE_DIVISOR
        val newPriorityFee = newMaxFee / BigInteger.valueOf(10)
        return newMaxFee to newPriorityFee
    }

    suspend fun speedUp(
        rpc: EvmRpcClient,
        original: PendingTx,
        privateKey: ByteArray,
    ): String {
        val (newMaxFee, newPriorityFee) = speedUpGas(original)
        val tx = EvmTransaction(
            chainId = original.chainId,
            nonce = original.nonce,
            maxPriorityFeePerGas = newPriorityFee,
            maxFeePerGas = newMaxFee,
            gasLimit = BigInteger.valueOf(21_000),
            to = original.to,
            value = original.value,
        )
        val signedHex = EvmSigner.signToHex(privateKey, tx)
        return rpc.sendRawTransaction(signedHex)
    }

    suspend fun cancel(
        rpc: EvmRpcClient,
        original: PendingTx,
        senderAddress: String,
        privateKey: ByteArray,
    ): String {
        val (newMaxFee, newPriorityFee) = speedUpGas(original)
        val tx = EvmTransaction(
            chainId = original.chainId,
            nonce = original.nonce,
            maxPriorityFeePerGas = newPriorityFee,
            maxFeePerGas = newMaxFee,
            gasLimit = BigInteger.valueOf(21_000),
            to = senderAddress,
            value = BigInteger.ZERO,
        )
        val signedHex = EvmSigner.signToHex(privateKey, tx)
        return rpc.sendRawTransaction(signedHex)
    }
}
