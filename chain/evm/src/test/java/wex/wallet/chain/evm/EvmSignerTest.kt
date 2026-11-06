@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class EvmSignerTest {

    @Test
    fun `sign EIP-1559 transaction produces valid hex`() {
        val privKey = BigInteger(
            "4c0883a69102937d6231471b5dbb6204fe512961708279f73e5d4b5e5b5c2e1b",
            16,
        ).toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }

        val tx = EvmTransaction(
            chainId = 1,
            nonce = 0,
            maxPriorityFeePerGas = BigInteger.valueOf(1_500_000_000),
            maxFeePerGas = BigInteger.valueOf(30_000_000_000),
            gasLimit = BigInteger.valueOf(21_000),
            to = "0x3535353535353535353535353535353535353535",
            value = BigInteger.valueOf(1_000_000_000_000_000_000),
        )

        val signedHex = EvmSigner.signToHex(privKey, tx)
        assertTrue("Signed tx should start with 0x02", signedHex.startsWith("0x02"))
        assertTrue("Signed tx should be non-trivial length", signedHex.length > 100)
    }

    @Test
    fun `sign produces deterministic results`() {
        val privKey = "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80".hexToBytes()

        val tx = EvmTransaction(
            chainId = 1,
            nonce = 0,
            maxPriorityFeePerGas = BigInteger.valueOf(1_000_000_000),
            maxFeePerGas = BigInteger.valueOf(20_000_000_000),
            gasLimit = BigInteger.valueOf(21_000),
            to = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            value = BigInteger.valueOf(100_000_000_000_000_000),
        )

        val signed1 = EvmSigner.signToHex(privKey, tx)
        val signed2 = EvmSigner.signToHex(privKey, tx)
        assertEquals("Deterministic signing (RFC 6979)", signed1, signed2)
    }

    @Test
    fun `sign zero value transaction`() {
        val privKey = "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80".hexToBytes()

        val tx = EvmTransaction(
            chainId = 8453,
            nonce = 5,
            maxPriorityFeePerGas = BigInteger.valueOf(100_000),
            maxFeePerGas = BigInteger.valueOf(1_000_000),
            gasLimit = BigInteger.valueOf(50_000),
            to = "0x3535353535353535353535353535353535353535",
        )

        val signedHex = EvmSigner.signToHex(privKey, tx)
        assertTrue(signedHex.startsWith("0x02"))
    }
}
