@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class TxAccelerationTest {

    @Test
    fun `speedUpGas increases fee by 10 percent`() {
        val original = PendingTx(
            txHash = "0xabc",
            chainId = 1,
            nonce = 5,
            to = "0x1234567890123456789012345678901234567890",
            value = BigInteger.ZERO,
            maxFeePerGas = BigInteger.valueOf(10_000_000_000),
        )
        val (newMaxFee, _) = TxAcceleration.speedUpGas(original)
        assertEquals(BigInteger.valueOf(11_000_000_000), newMaxFee)
    }

    @Test
    fun `speedUpGas priority fee is 10 percent of new max fee`() {
        val original = PendingTx(
            txHash = "0xabc",
            chainId = 1,
            nonce = 5,
            to = "0x1234567890123456789012345678901234567890",
            value = BigInteger.ZERO,
            maxFeePerGas = BigInteger.valueOf(10_000_000_000),
        )
        val (newMaxFee, newPriorityFee) = TxAcceleration.speedUpGas(original)
        assertEquals(newMaxFee / BigInteger.TEN, newPriorityFee)
    }

    @Test
    fun `speedUpGas always results in higher fee`() {
        val original = PendingTx(
            txHash = "0xabc",
            chainId = 1,
            nonce = 5,
            to = "0x1234567890123456789012345678901234567890",
            value = BigInteger.ZERO,
            maxFeePerGas = BigInteger.valueOf(1),
        )
        val (newMaxFee, _) = TxAcceleration.speedUpGas(original)
        assertTrue(newMaxFee >= original.maxFeePerGas)
    }
}
