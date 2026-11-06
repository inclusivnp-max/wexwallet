@file:Suppress("MagicNumber", "TooManyFunctions")

package wex.wallet.chain.evm

import org.bouncycastle.jcajce.provider.digest.Keccak
import java.math.BigInteger

object Erc20 {

    private val TRANSFER_SELECTOR = functionSelector("transfer(address,uint256)")
    private val BALANCE_OF_SELECTOR = functionSelector("balanceOf(address)")
    private val APPROVE_SELECTOR = functionSelector("approve(address,uint256)")
    private val ALLOWANCE_SELECTOR = functionSelector("allowance(address,address)")
    private val DECIMALS_SELECTOR = functionSelector("decimals()")
    private val SYMBOL_SELECTOR = functionSelector("symbol()")
    private val NAME_SELECTOR = functionSelector("name()")

    fun encodeTransfer(to: String, amount: BigInteger): ByteArray {
        return TRANSFER_SELECTOR + encodeAddress(to) + encodeUint256(amount)
    }

    fun encodeBalanceOf(address: String): ByteArray {
        return BALANCE_OF_SELECTOR + encodeAddress(address)
    }

    fun encodeApprove(spender: String, amount: BigInteger): ByteArray {
        return APPROVE_SELECTOR + encodeAddress(spender) + encodeUint256(amount)
    }

    fun encodeAllowance(owner: String, spender: String): ByteArray {
        return ALLOWANCE_SELECTOR + encodeAddress(owner) + encodeAddress(spender)
    }

    fun encodeDecimals(): ByteArray = DECIMALS_SELECTOR

    fun encodeSymbol(): ByteArray = SYMBOL_SELECTOR

    fun encodeName(): ByteArray = NAME_SELECTOR

    fun decodeUint256(data: String): BigInteger {
        val hex = data.removePrefix("0x")
        if (hex.isEmpty() || hex == "0") return BigInteger.ZERO
        return BigInteger(hex, 16)
    }

    fun decodeString(data: String): String {
        val hex = data.removePrefix("0x")
        if (hex.length < 128) return ""
        val offset = BigInteger(hex.substring(0, 64), 16).toInt() * 2
        val length = BigInteger(hex.substring(offset, offset + 64), 16).toInt()
        val strHex = hex.substring(offset + 64, offset + 64 + length * 2)
        return strHex.hexToBytes().toString(Charsets.UTF_8)
    }

    private fun functionSelector(signature: String): ByteArray {
        return Keccak.Digest256().digest(signature.toByteArray(Charsets.US_ASCII)).copyOfRange(0, 4)
    }

    private fun encodeAddress(address: String): ByteArray {
        val bytes = address.removePrefix("0x").hexToBytes()
        val padded = ByteArray(32)
        bytes.copyInto(padded, 32 - bytes.size)
        return padded
    }

    private fun encodeUint256(value: BigInteger): ByteArray {
        val bytes = value.toByteArray().let { raw ->
            if (raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
        }
        require(bytes.size <= 32) { "Value too large for uint256" }
        val padded = ByteArray(32)
        bytes.copyInto(padded, 32 - bytes.size)
        return padded
    }
}
