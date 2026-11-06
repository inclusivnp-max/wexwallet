@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import org.junit.Assert.assertEquals
import org.junit.Test
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric
import java.math.BigInteger

class EvmSignerCrossVerifyTest {

    private val testPrivKeyHex = "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"
    private val testPrivKey = testPrivKeyHex.hexToBytes()

    @Test
    fun `EIP-1559 signature matches web3j for simple ETH transfer`() {
        val chainId = 1L
        val nonce = 0L
        val maxPriorityFee = BigInteger.valueOf(1_500_000_000)
        val maxFee = BigInteger.valueOf(30_000_000_000)
        val gasLimit = BigInteger.valueOf(21_000)
        val to = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
        val value = BigInteger.valueOf(1_000_000_000_000_000_000)

        val ourTx = EvmTransaction(
            chainId = chainId,
            nonce = nonce,
            maxPriorityFeePerGas = maxPriorityFee,
            maxFeePerGas = maxFee,
            gasLimit = gasLimit,
            to = to,
            value = value,
        )
        val ourSigned = EvmSigner.signToHex(testPrivKey, ourTx)

        val web3jTx = RawTransaction.createTransaction(
            chainId,
            BigInteger.valueOf(nonce),
            gasLimit,
            to,
            value,
            "",
            maxPriorityFee,
            maxFee,
        )
        val credentials = Credentials.create(testPrivKeyHex)
        val web3jSigned = Numeric.toHexString(TransactionEncoder.signMessage(web3jTx, chainId, credentials))

        assertEquals("Signed tx must match web3j", web3jSigned, ourSigned)
    }

    @Test
    fun `EIP-1559 signature matches web3j for zero value tx`() {
        val chainId = 8453L
        val nonce = 42L
        val maxPriorityFee = BigInteger.valueOf(100_000)
        val maxFee = BigInteger.valueOf(1_000_000)
        val gasLimit = BigInteger.valueOf(50_000)
        val to = "0x3535353535353535353535353535353535353535"

        val ourTx = EvmTransaction(
            chainId = chainId,
            nonce = nonce,
            maxPriorityFeePerGas = maxPriorityFee,
            maxFeePerGas = maxFee,
            gasLimit = gasLimit,
            to = to,
        )
        val ourSigned = EvmSigner.signToHex(testPrivKey, ourTx)

        val web3jTx = RawTransaction.createTransaction(
            chainId,
            BigInteger.valueOf(nonce),
            gasLimit,
            to,
            BigInteger.ZERO,
            "",
            maxPriorityFee,
            maxFee,
        )
        val credentials = Credentials.create(testPrivKeyHex)
        val web3jSigned = Numeric.toHexString(TransactionEncoder.signMessage(web3jTx, chainId, credentials))

        assertEquals("Zero-value tx must match web3j", web3jSigned, ourSigned)
    }

    @Test
    fun `EIP-1559 signature matches web3j with data payload`() {
        val chainId = 1L
        val nonce = 7L
        val maxPriorityFee = BigInteger.valueOf(2_000_000_000)
        val maxFee = BigInteger.valueOf(50_000_000_000)
        val gasLimit = BigInteger.valueOf(60_000)
        val to = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        val data = Erc20.encodeTransfer(
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            BigInteger.valueOf(1_000_000),
        )

        val ourTx = EvmTransaction(
            chainId = chainId,
            nonce = nonce,
            maxPriorityFeePerGas = maxPriorityFee,
            maxFeePerGas = maxFee,
            gasLimit = gasLimit,
            to = to,
            data = data,
        )
        val ourSigned = EvmSigner.signToHex(testPrivKey, ourTx)

        val web3jTx = RawTransaction.createTransaction(
            chainId,
            BigInteger.valueOf(nonce),
            gasLimit,
            to,
            BigInteger.ZERO,
            Numeric.toHexStringNoPrefix(data),
            maxPriorityFee,
            maxFee,
        )
        val credentials = Credentials.create(testPrivKeyHex)
        val web3jSigned = Numeric.toHexString(TransactionEncoder.signMessage(web3jTx, chainId, credentials))

        assertEquals("ERC-20 transfer tx must match web3j", web3jSigned, ourSigned)
    }
}
