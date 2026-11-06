@file:Suppress("MagicNumber")

package wex.wallet.data.portfolio

import wex.wallet.chain.evm.Erc20
import wex.wallet.chain.evm.toHex
import wex.wallet.core.network.EvmRpcClient
import java.math.BigInteger

class EvmBalanceRepository(private val rpc: EvmRpcClient) {

    suspend fun getNativeBalance(address: String): BigInteger = rpc.getBalance(address)

    suspend fun getTokenBalance(walletAddress: String, contractAddress: String): BigInteger {
        val calldata = "0x" + Erc20.encodeBalanceOf(walletAddress).toHex()
        val result = rpc.ethCall(to = contractAddress, data = calldata)
        return Erc20.decodeUint256(result)
    }

    suspend fun getTokenInfo(contractAddress: String): TokenInfo {
        val symbolData = "0x" + Erc20.encodeSymbol().toHex()
        val decimalsData = "0x" + Erc20.encodeDecimals().toHex()
        val nameData = "0x" + Erc20.encodeName().toHex()

        val symbolResult = rpc.ethCall(to = contractAddress, data = symbolData)
        val decimalsResult = rpc.ethCall(to = contractAddress, data = decimalsData)
        val nameResult = rpc.ethCall(to = contractAddress, data = nameData)

        return TokenInfo(
            contractAddress = contractAddress,
            symbol = Erc20.decodeString(symbolResult),
            name = Erc20.decodeString(nameResult),
            decimals = Erc20.decodeUint256(decimalsResult).toInt(),
        )
    }

    suspend fun getFullBalance(address: String, tokenContracts: List<String> = emptyList()): EvmBalance {
        val nativeBalance = getNativeBalance(address)
        val tokens = tokenContracts.mapNotNull { contract ->
            try {
                val info = getTokenInfo(contract)
                val balance = getTokenBalance(address, contract)
                TokenBalance(
                    contractAddress = contract,
                    symbol = info.symbol,
                    name = info.name,
                    decimals = info.decimals,
                    balance = balance,
                )
            } catch (_: Exception) {
                null
            }
        }
        return EvmBalance(address = address, nativeBalanceWei = nativeBalance, tokens = tokens)
    }
}

data class TokenInfo(
    val contractAddress: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
)
