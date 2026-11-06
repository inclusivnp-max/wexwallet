@file:Suppress("MagicNumber", "TooManyFunctions")

package wex.wallet.core.network

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigInteger

class EvmRpcClient(private val rpc: JsonRpcClient) {

    suspend fun getBalance(address: String, block: String = "latest"): BigInteger {
        val result = rpc.call("eth_getBalance", listOf(JsonPrimitive(address), JsonPrimitive(block)))
        return result.jsonPrimitive.content.hexToBigInteger()
    }

    suspend fun getTransactionCount(address: String, block: String = "latest"): Long {
        val result = rpc.call(
            "eth_getTransactionCount",
            listOf(JsonPrimitive(address), JsonPrimitive(block)),
        )
        return result.jsonPrimitive.content.hexToBigInteger().toLong()
    }

    suspend fun getChainId(): Long {
        val result = rpc.call("eth_chainId")
        return result.jsonPrimitive.content.hexToBigInteger().toLong()
    }

    suspend fun estimateGas(
        from: String,
        to: String,
        value: BigInteger = BigInteger.ZERO,
        data: String = "0x",
    ): BigInteger {
        val txObj = buildJsonObject {
            put("from", JsonPrimitive(from))
            put("to", JsonPrimitive(to))
            put("value", JsonPrimitive("0x${value.toString(16)}"))
            if (data != "0x") put("data", JsonPrimitive(data))
        }
        val result = rpc.call("eth_estimateGas", listOf(txObj))
        return result.jsonPrimitive.content.hexToBigInteger()
    }

    suspend fun getGasPrice(): BigInteger {
        val result = rpc.call("eth_gasPrice")
        return result.jsonPrimitive.content.hexToBigInteger()
    }

    suspend fun getMaxPriorityFeePerGas(): BigInteger {
        val result = rpc.call("eth_maxPriorityFeePerGas")
        return result.jsonPrimitive.content.hexToBigInteger()
    }

    suspend fun getBaseFeePerGas(): BigInteger {
        val block = rpc.call("eth_getBlockByNumber", listOf(JsonPrimitive("latest"), JsonPrimitive(false)))
        return block.jsonObject["baseFeePerGas"]?.jsonPrimitive?.content?.hexToBigInteger()
            ?: getGasPrice()
    }

    suspend fun sendRawTransaction(signedTxHex: String): String {
        val result = rpc.call("eth_sendRawTransaction", listOf(JsonPrimitive(signedTxHex)))
        return result.jsonPrimitive.content
    }

    suspend fun getTransactionReceipt(txHash: String): TransactionReceipt? {
        val result = rpc.call("eth_getTransactionReceipt", listOf(JsonPrimitive(txHash)))
        if (result is kotlinx.serialization.json.JsonNull) return null
        val obj = result.jsonObject
        return TransactionReceipt(
            transactionHash = obj["transactionHash"]!!.jsonPrimitive.content,
            status = obj["status"]?.jsonPrimitive?.content == "0x1",
            blockNumber = obj["blockNumber"]?.jsonPrimitive?.content?.hexToBigInteger()?.toLong() ?: 0L,
            gasUsed = obj["gasUsed"]?.jsonPrimitive?.content?.hexToBigInteger() ?: BigInteger.ZERO,
        )
    }

    suspend fun ethCall(
        to: String,
        data: String,
        block: String = "latest",
    ): String {
        val txObj = buildJsonObject {
            put("to", JsonPrimitive(to))
            put("data", JsonPrimitive(data))
        }
        val result = rpc.call("eth_call", listOf(txObj, JsonPrimitive(block)))
        return result.jsonPrimitive.content
    }

    suspend fun getFeeHistory(
        blockCount: Int = 4,
        newestBlock: String = "latest",
        rewardPercentiles: List<Double> = listOf(25.0, 50.0, 75.0),
    ): FeeHistory {
        val percArray = buildJsonArray { rewardPercentiles.forEach { add(JsonPrimitive(it)) } }
        val result = rpc.call(
            "eth_feeHistory",
            listOf(JsonPrimitive("0x${blockCount.toString(16)}"), JsonPrimitive(newestBlock), percArray),
        )
        val obj = result.jsonObject
        val baseFees = obj["baseFeePerGas"]!!.jsonArray.map { it.jsonPrimitive.content.hexToBigInteger() }
        val rewards = obj["reward"]?.jsonArray?.map { block ->
            block.jsonArray.map { it.jsonPrimitive.content.hexToBigInteger() }
        } ?: emptyList()
        return FeeHistory(baseFeePerGas = baseFees, reward = rewards)
    }
}

data class TransactionReceipt(
    val transactionHash: String,
    val status: Boolean,
    val blockNumber: Long,
    val gasUsed: BigInteger,
)

data class FeeHistory(
    val baseFeePerGas: List<BigInteger>,
    val reward: List<List<BigInteger>>,
)

private fun String.hexToBigInteger(): BigInteger {
    val hex = removePrefix("0x").removePrefix("0X")
    if (hex.isEmpty()) return BigInteger.ZERO
    return BigInteger(hex, 16)
}
