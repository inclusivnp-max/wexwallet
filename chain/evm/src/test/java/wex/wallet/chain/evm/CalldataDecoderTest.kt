@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class CalldataDecoderTest {

    // Каноничные селекторы ERC-20, широко задокументированные.
    private val transferSelector = "0xa9059cbb"
    private val approveSelector = "0x095ea7b3"
    private val transferFromSelector = "0x23b872dd"

    private val alice = "0x1234567890abcdef1234567890abcdef12345678"
    private val bob = "0xfedcba0987654321fedcba0987654321fedcba09"

    private fun pad(hex: String) = hex.removePrefix("0x").padStart(64, '0')

    @Test
    fun `пустой calldata это перевод монеты`() {
        val call = CalldataDecoder.decode("0x")
        assertFalse(call.isKnown)
        assertEquals("Перевод монеты", call.humanLabel)
    }

    @Test
    fun `null calldata это перевод монеты`() {
        assertEquals("Перевод монеты", CalldataDecoder.decode(null).humanLabel)
    }

    @Test
    fun `селектор transfer совпадает с каноничным`() {
        val data = transferSelector + pad(alice) + pad("64")
        val call = CalldataDecoder.decode(data)
        assertTrue(call.isKnown)
        assertEquals("transfer(address,uint256)", call.signature)
    }

    @Test
    fun `селектор approve совпадает с каноничным`() {
        val data = approveSelector + pad(bob) + pad("ff")
        assertEquals("approve(address,uint256)", CalldataDecoder.decode(data).signature)
    }

    @Test
    fun `селектор transferFrom совпадает с каноничным`() {
        val data = transferFromSelector + pad(alice) + pad(bob) + pad("1")
        assertEquals("transferFrom(address,address,uint256)", CalldataDecoder.decode(data).signature)
    }

    @Test
    fun `декодер извлекает получателя и сумму из transfer`() {
        val amount = BigInteger.valueOf(1_500_000)
        val data = transferSelector + pad(alice) + pad(amount.toString(16))
        val call = CalldataDecoder.decode(data)

        assertEquals(2, call.args.size)
        assertEquals(alice, call.args[0].value)
        assertEquals(amount.toString(), call.args[1].value)
    }

    @Test
    fun `декодер согласован с кодировщиком Erc20`() {
        val amount = BigInteger.valueOf(42)
        val encoded = "0x" + Erc20.encodeTransfer(alice, amount).toHex()
        val call = CalldataDecoder.decode(encoded)

        assertEquals("transfer(address,uint256)", call.signature)
        assertEquals(alice, call.args[0].value)
        assertEquals("42", call.args[1].value)
    }

    @Test
    fun `безлимитный approve распознаётся`() {
        val data = approveSelector + pad(bob) + "f".repeat(64)
        val call = CalldataDecoder.decode(data)
        assertTrue(CalldataDecoder.isUnlimitedApprove(call))
    }

    @Test
    fun `обычный approve не считается безлимитным`() {
        val data = approveSelector + pad(bob) + pad(BigInteger.valueOf(1000).toString(16))
        val call = CalldataDecoder.decode(data)
        assertFalse(CalldataDecoder.isUnlimitedApprove(call))
    }

    @Test
    fun `transfer не считается approve`() {
        val data = transferSelector + pad(alice) + "f".repeat(64)
        assertFalse(CalldataDecoder.isUnlimitedApprove(CalldataDecoder.decode(data)))
    }

    @Test
    fun `approve отдаёт адрес получателя разрешения`() {
        val data = approveSelector + pad(bob) + pad("1")
        assertEquals(bob, CalldataDecoder.approveSpender(CalldataDecoder.decode(data)))
    }

    @Test
    fun `у не-approve нет получателя разрешения`() {
        val data = transferSelector + pad(alice) + pad("1")
        assertNull(CalldataDecoder.approveSpender(CalldataDecoder.decode(data)))
    }

    @Test
    fun `неизвестный вызов помечается честно`() {
        val call = CalldataDecoder.decode("0xdeadbeef" + pad("1"))
        assertFalse(call.isKnown)
        assertNull(call.signature)
        assertTrue(call.humanLabel.contains("Неизвестный"))
        assertTrue(call.humanLabel.contains("0xdeadbeef"))
    }

    @Test
    fun `обрезанный calldata не роняет декодер`() {
        val call = CalldataDecoder.decode("0xa9059cbb1234")
        assertEquals("transfer(address,uint256)", call.signature)
        assertTrue(call.args.isEmpty())
    }

    @Test
    fun `слишком короткий calldata не роняет декодер`() {
        val call = CalldataDecoder.decode("0xab")
        assertFalse(call.isKnown)
    }

    @Test
    fun `bool декодируется в да и нет`() {
        val setApprovalForAll = CalldataDecoder.decode(
            "0xa22cb465" + pad(bob) + pad("1"),
        )
        assertEquals("setApprovalForAll(address,bool)", setApprovalForAll.signature)
        assertEquals("да", setApprovalForAll.args[1].value)
    }

    @Test
    fun `размер данных считается в байтах`() {
        val data = transferSelector + pad(alice) + pad("1")
        assertEquals(68, CalldataDecoder.decode(data).dataSize)
    }
}
