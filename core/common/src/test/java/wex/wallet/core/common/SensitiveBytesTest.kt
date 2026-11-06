package wex.wallet.core.common

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveBytesTest {

    @Test
    fun `wrap provides access via use`() {
        val data = byteArrayOf(1, 2, 3, 4)
        val sensitive = SensitiveBytes.wrap(data)
        sensitive.use { bytes ->
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), bytes)
        }
    }

    @Test
    fun `close zeroes underlying data`() {
        val data = byteArrayOf(0x41, 0x42, 0x43)
        val sensitive = SensitiveBytes.wrap(data)
        sensitive.close()
        assertArrayEquals(byteArrayOf(0, 0, 0), data)
        assertTrue(sensitive.isClosed)
    }

    @Test(expected = IllegalStateException::class)
    fun `use after close throws`() {
        val sensitive = SensitiveBytes.wrap(byteArrayOf(1))
        sensitive.close()
        sensitive.use { it }
    }

    @Test
    fun `copy creates independent copy`() {
        val original = byteArrayOf(10, 20, 30)
        val sensitive = SensitiveBytes.copy(original)
        original[0] = 0
        sensitive.use { bytes ->
            assertEquals(10.toByte(), bytes[0])
        }
    }

    @Test
    fun `copyBytes returns a copy`() {
        val sensitive = SensitiveBytes.wrap(byteArrayOf(5, 6, 7))
        val copy = sensitive.copyBytes()
        sensitive.close()
        assertArrayEquals(byteArrayOf(5, 6, 7), copy)
    }

    @Test
    fun `double close is safe`() {
        val sensitive = SensitiveBytes.wrap(byteArrayOf(1))
        sensitive.close()
        sensitive.close()
        assertTrue(sensitive.isClosed)
    }

    @Test
    fun `toString does not leak content`() {
        val sensitive = SensitiveBytes.wrap(byteArrayOf(0xDE.toByte(), 0xAD.toByte()))
        val str = sensitive.toString()
        assertFalse(str.contains("DE", ignoreCase = true))
        assertFalse(str.contains("AD", ignoreCase = true))
        assertTrue(str.contains("2 bytes"))
    }

    @Test
    fun `identity equality`() {
        val a = SensitiveBytes.wrap(byteArrayOf(1, 2, 3))
        val b = SensitiveBytes.wrap(byteArrayOf(1, 2, 3))
        assertNotEquals(a, b)
        assertEquals(a, a)
    }

    @Test
    fun `size reflects data length`() {
        assertEquals(16, SensitiveBytes.wrap(ByteArray(16)).size)
        assertEquals(32, SensitiveBytes.wrap(ByteArray(32)).size)
    }
}
