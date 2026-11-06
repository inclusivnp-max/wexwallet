@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class Erc20Test {

    @Test
    fun `encodeTransfer has correct selector`() {
        val data = Erc20.encodeTransfer(
            "0x3535353535353535353535353535353535353535",
            BigInteger.valueOf(1_000_000),
        )
        assertEquals(4 + 32 + 32, data.size)
        val selector = data.copyOfRange(0, 4).toHex()
        assertEquals("a9059cbb", selector)
    }

    @Test
    fun `encodeBalanceOf has correct selector`() {
        val data = Erc20.encodeBalanceOf("0x3535353535353535353535353535353535353535")
        assertEquals(4 + 32, data.size)
        val selector = data.copyOfRange(0, 4).toHex()
        assertEquals("70a08231", selector)
    }

    @Test
    fun `encodeApprove has correct selector`() {
        val data = Erc20.encodeApprove(
            "0x3535353535353535353535353535353535353535",
            BigInteger.valueOf(1_000_000),
        )
        assertEquals("095ea7b3", data.copyOfRange(0, 4).toHex())
    }

    @Test
    fun `decodeUint256 parses hex value`() {
        val hex = "0x" + "0".repeat(56) + "000f4240"
        val value = Erc20.decodeUint256(hex)
        assertEquals(BigInteger.valueOf(1_000_000), value)
    }

    @Test
    fun `decodeUint256 handles zero`() {
        assertEquals(BigInteger.ZERO, Erc20.decodeUint256("0x0"))
    }

    @Test
    fun `encodeTransfer address is left-padded`() {
        val data = Erc20.encodeTransfer(
            "0x0000000000000000000000000000000000000001",
            BigInteger.ONE,
        )
        val addrSlot = data.copyOfRange(4, 36)
        assertTrue(addrSlot.take(31).all { it == 0.toByte() })
        assertEquals(1.toByte(), addrSlot[31])
    }
}
