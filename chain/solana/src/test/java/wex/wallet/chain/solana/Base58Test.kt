package wex.wallet.chain.solana

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class Base58Test {

    @Test
    fun `encode empty bytes`() {
        assertEquals("", Base58.encode(ByteArray(0)))
    }

    @Test
    fun `encode single zero byte`() {
        assertEquals("1", Base58.encode(ByteArray(1)))
    }

    @Test
    fun `encode known vector`() {
        assertEquals("2j", Base58.encode(byteArrayOf(100)))
    }

    @Test
    fun `encode-decode round trip`() {
        val data = "48656c6c6f20576f726c6421".hexToByteArray()
        val encoded = Base58.encode(data)
        val decoded = Base58.decode(encoded)
        assertArrayEquals(data, decoded)
    }

    @Test
    fun `leading zeros preserved`() {
        val data = byteArrayOf(0, 0, 0, 1)
        val encoded = Base58.encode(data)
        assert(encoded.startsWith("111"))
        assertArrayEquals(data, Base58.decode(encoded))
    }

    @Test
    fun `Bitcoin genesis block hash`() {
        val hash = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"
            .hexToByteArray()
        val encoded = Base58.encode(hash)
        val decoded = Base58.decode(encoded)
        assertArrayEquals(hash, decoded)
    }
}
