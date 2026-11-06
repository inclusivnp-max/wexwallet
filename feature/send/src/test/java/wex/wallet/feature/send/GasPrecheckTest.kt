@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class GasPrecheckTest {

    @Test
    fun `sufficient balance passes`() {
        val result = GasPrecheck.check(
            balanceWei = BigInteger.valueOf(1_000_000_000_000_000_000),
            sendAmountWei = BigInteger.valueOf(500_000_000_000_000_000),
            estimatedGasWei = BigInteger.valueOf(21_000_000_000_000),
        )
        assertTrue(result.hasEnoughGas)
    }

    @Test
    fun `insufficient balance fails with shortfall`() {
        val result = GasPrecheck.check(
            balanceWei = BigInteger.valueOf(100_000),
            sendAmountWei = BigInteger.valueOf(50_000),
            estimatedGasWei = BigInteger.valueOf(60_000),
        )
        assertFalse(result.hasEnoughGas)
        assertEquals(BigInteger.valueOf(10_000), result.shortfall)
        assertTrue(result.message.contains("more ETH"))
    }

    @Test
    fun `exact balance passes`() {
        val result = GasPrecheck.check(
            balanceWei = BigInteger.valueOf(110_000),
            sendAmountWei = BigInteger.valueOf(50_000),
            estimatedGasWei = BigInteger.valueOf(60_000),
        )
        assertTrue(result.hasEnoughGas)
    }

    @Test
    fun `maxSendableWei subtracts gas`() {
        val max = GasPrecheck.maxSendableWei(
            balanceWei = BigInteger.valueOf(1_000_000),
            estimatedGasWei = BigInteger.valueOf(21_000),
        )
        assertEquals(BigInteger.valueOf(979_000), max)
    }

    @Test
    fun `maxSendableWei returns zero when gas exceeds balance`() {
        val max = GasPrecheck.maxSendableWei(
            balanceWei = BigInteger.valueOf(10_000),
            estimatedGasWei = BigInteger.valueOf(21_000),
        )
        assertEquals(BigInteger.ZERO, max)
    }

    @Test
    fun `custom symbol in message`() {
        val result = GasPrecheck.check(
            balanceWei = BigInteger.ZERO,
            sendAmountWei = BigInteger.ONE,
            estimatedGasWei = BigInteger.ONE,
            symbol = "BNB",
        )
        assertTrue(result.message.contains("BNB"))
    }
}
