@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class RlpTest {

    @Test
    fun `encode single byte below 0x80`() {
        assertArrayEquals(byteArrayOf(0x00), Rlp.encode(byteArrayOf(0x00)))
        assertArrayEquals(byteArrayOf(0x7f), Rlp.encode(byteArrayOf(0x7f)))
    }

    @Test
    fun `encode single byte at 0x80`() {
        val encoded = Rlp.encode(byteArrayOf(0x80.toByte()))
        assertEquals("81", encoded[0].toUByte().toString(16))
    }

    @Test
    fun `encode empty bytes`() {
        assertArrayEquals(byteArrayOf(0x80.toByte()), Rlp.encode(ByteArray(0)))
    }

    @Test
    fun `encode short string`() {
        val data = "dog".toByteArray()
        val encoded = Rlp.encode(data)
        assertEquals(0x83.toByte(), encoded[0])
        assertEquals("dog", String(encoded, 1, 3))
    }

    @Test
    fun `encode BigInteger zero`() {
        assertArrayEquals(byteArrayOf(0x80.toByte()), Rlp.encode(BigInteger.ZERO))
    }

    @Test
    fun `encode BigInteger 1`() {
        assertArrayEquals(byteArrayOf(0x01), Rlp.encode(BigInteger.ONE))
    }

    @Test
    fun `encode BigInteger 127`() {
        assertArrayEquals(byteArrayOf(0x7f), Rlp.encode(BigInteger.valueOf(127)))
    }

    @Test
    fun `encode BigInteger 128`() {
        val encoded = Rlp.encode(BigInteger.valueOf(128))
        assertEquals(0x81.toByte(), encoded[0])
        assertEquals(0x80.toByte(), encoded[1])
    }

    @Test
    fun `encode empty list`() {
        val encoded = Rlp.encodeList(emptyList())
        assertArrayEquals(byteArrayOf(0xC0.toByte()), encoded)
    }

    @Test
    fun `encode list with items`() {
        val items = listOf(
            Rlp.encode("cat".toByteArray()),
            Rlp.encode("dog".toByteArray()),
        )
        val encoded = Rlp.encodeList(items)
        assertEquals(0xC8.toByte(), encoded[0])
    }
}
