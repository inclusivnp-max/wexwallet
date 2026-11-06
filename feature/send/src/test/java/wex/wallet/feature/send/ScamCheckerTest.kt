@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class ScamCheckerTest {

    @Test
    fun `first transfer shows info warning`() {
        val warnings = ScamChecker.check(
            toAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            isFirstTransfer = true,
        )
        assertEquals(1, warnings.size)
        assertEquals(ScamWarning.Level.INFO, warnings[0].level)
    }

    @Test
    fun `no warnings for known address`() {
        val warnings = ScamChecker.check(
            toAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            isFirstTransfer = false,
        )
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `similar address detected`() {
        val target = "0x70991111111111111111111111111111111179C8"
        val recent = "0x70992222222222222222222222222222222279C8"
        val warnings = ScamChecker.check(
            toAddress = target,
            isFirstTransfer = false,
            recentAddresses = listOf(recent),
        )
        assertTrue(warnings.any { it.level == ScamWarning.Level.DANGER })
        assertTrue(warnings.any { it.message.contains("poisoning") })
    }

    @Test
    fun `same address not flagged as similar`() {
        val addr = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
        val warnings = ScamChecker.check(
            toAddress = addr,
            isFirstTransfer = false,
            recentAddresses = listOf(addr),
        )
        assertTrue(warnings.none { it.message.contains("poisoning") })
    }

    @Test
    fun `unlimited approve flagged`() {
        val maxUint256 = BigInteger.valueOf(2).pow(256) - BigInteger.ONE
        val warnings = ScamChecker.check(
            toAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            isFirstTransfer = false,
            approveAmount = maxUint256,
        )
        assertTrue(warnings.any { it.level == ScamWarning.Level.DANGER })
        assertTrue(warnings.any { it.message.contains("Unlimited") })
    }

    @Test
    fun `normal approve amount not flagged`() {
        val warnings = ScamChecker.check(
            toAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            isFirstTransfer = false,
            approveAmount = BigInteger.valueOf(1_000_000),
        )
        assertTrue(warnings.none { it.message.contains("Unlimited") })
    }

    @Test
    fun `zero address flagged`() {
        val warnings = ScamChecker.check(
            toAddress = "0x0000000000000000000000000000000000000000",
            isFirstTransfer = false,
        )
        assertTrue(warnings.any { it.level == ScamWarning.Level.DANGER })
        assertTrue(warnings.any { it.message.contains("burn") })
    }
}
