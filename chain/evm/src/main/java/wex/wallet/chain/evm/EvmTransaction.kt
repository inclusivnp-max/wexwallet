package wex.wallet.chain.evm

import java.math.BigInteger

data class EvmTransaction(
    val chainId: Long,
    val nonce: Long,
    val maxPriorityFeePerGas: BigInteger,
    val maxFeePerGas: BigInteger,
    val gasLimit: BigInteger,
    val to: String,
    val value: BigInteger = BigInteger.ZERO,
    val data: ByteArray = ByteArray(0),
) {

    fun rlpForSigning(): ByteArray {
        val fields = listOf(
            Rlp.encode(chainId),
            Rlp.encode(nonce),
            Rlp.encode(maxPriorityFeePerGas),
            Rlp.encode(maxFeePerGas),
            Rlp.encode(gasLimit),
            Rlp.encode(to.hexToBytes()),
            Rlp.encode(value),
            Rlp.encode(data),
            Rlp.encodeList(emptyList()),
        )
        return byteArrayOf(0x02) + Rlp.encodeList(fields)
    }

    fun rlpSigned(v: Int, r: BigInteger, s: BigInteger): ByteArray {
        val fields = listOf(
            Rlp.encode(chainId),
            Rlp.encode(nonce),
            Rlp.encode(maxPriorityFeePerGas),
            Rlp.encode(maxFeePerGas),
            Rlp.encode(gasLimit),
            Rlp.encode(to.hexToBytes()),
            Rlp.encode(value),
            Rlp.encode(data),
            Rlp.encodeList(emptyList()),
            Rlp.encode(v.toLong()),
            Rlp.encode(r),
            Rlp.encode(s),
        )
        return byteArrayOf(0x02) + Rlp.encodeList(fields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EvmTransaction) return false
        return chainId == other.chainId && nonce == other.nonce &&
            maxPriorityFeePerGas == other.maxPriorityFeePerGas &&
            maxFeePerGas == other.maxFeePerGas && gasLimit == other.gasLimit &&
            to == other.to && value == other.value && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = chainId.hashCode()
        result = 31 * result + nonce.hashCode()
        result = 31 * result + to.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
