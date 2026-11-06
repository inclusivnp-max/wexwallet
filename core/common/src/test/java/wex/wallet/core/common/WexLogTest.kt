package wex.wallet.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class WexLogTest {

    @Test
    fun `сокращает длинный адрес до начала и конца`() {
        val address = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"
        assertEquals("0x71C7…976F", WexLog.redact(address))
    }

    @Test
    fun `короткое значение скрывается целиком`() {
        assertEquals("…", WexLog.redact("0x1234"))
    }

    @Test
    fun `граничная длина не раскрывает значение целиком`() {
        val value = "abcdefghijk"
        val redacted = WexLog.redact(value)
        assertEquals("abcdef…hijk", redacted)
        assertEquals(value.length, redacted.length)
    }
}
