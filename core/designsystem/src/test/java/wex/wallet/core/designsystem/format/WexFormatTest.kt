@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class WexFormatTest {

    private val nbsp = WexFormat.GROUP_SEPARATOR

    // ── Фиат ──────────────────────────────────────────────────

    @Test
    fun `фиат разбивается по разрядам`() {
        assertEquals("$12${nbsp}480,62", WexFormat.fiat(BigDecimal("12480.62")))
    }

    @Test
    fun `фиат всегда с двумя знаками`() {
        assertEquals("$5,00", WexFormat.fiat(BigDecimal("5")))
        assertEquals("$0,10", WexFormat.fiat(BigDecimal("0.1")))
    }

    @Test
    fun `фиат округляет до копеек`() {
        assertEquals("$1,01", WexFormat.fiat(BigDecimal("1.005")))
    }

    @Test
    fun `миллион разбивается на три группы`() {
        assertEquals("$1${nbsp}234${nbsp}567,00", WexFormat.fiat(BigDecimal("1234567")))
    }

    @Test
    fun `отрицательный фиат сохраняет знак перед символом валюты`() {
        assertTrue(WexFormat.fiat(BigDecimal("-42")).startsWith("-$"))
    }

    // ── Суммы в монете ────────────────────────────────────────

    @Test
    fun `ноль остаётся нулём а не пустотой`() {
        assertEquals("0 ETH", WexFormat.amount(BigDecimal.ZERO, "ETH"))
    }

    @Test
    fun `незначащие нули убираются`() {
        assertEquals("1,5 ETH", WexFormat.amount(BigDecimal("1.500000"), "ETH"))
    }

    @Test
    fun `очень малая сумма не показывается нулём`() {
        val result = WexFormat.amount(BigDecimal("0.0000000001"), "ETH")
        assertTrue("Ожидалось «меньше чем», получено: $result", result.startsWith("<"))
        assertTrue(result.contains("ETH"))
    }

    @Test
    fun `USDC показывается с двумя знаками`() {
        assertEquals(2, WexFormat.listDecimals("USDC"))
        assertEquals("1${nbsp}240,55 USDC", WexFormat.amount(BigDecimal("1240.5567"), "USDC"))
    }

    @Test
    fun `BTC показывается с восемью знаками`() {
        assertEquals(8, WexFormat.listDecimals("BTC"))
    }

    @Test
    fun `сумма усекается вниз а не округляется вверх`() {
        // Округление вверх показало бы денег больше, чем есть.
        assertEquals("0,01 USDC", WexFormat.amount(BigDecimal("0.019"), "USDC"))
    }

    @Test
    fun `сумма из минимальных единиц переводится по знакам токена`() {
        val oneEth = BigInteger("1000000000000000000")
        assertEquals("1 ETH", WexFormat.amountFromRaw(oneEth, 18, "ETH"))
    }

    @Test
    fun `сумма USDC из шести знаков`() {
        assertEquals("1,5 USDC", WexFormat.amountFromRaw(BigInteger("1500000"), 6, "USDC"))
    }

    // ── Изменение ─────────────────────────────────────────────

    @Test
    fun `рост со знаком плюс`() {
        assertTrue(WexFormat.signedFiat(BigDecimal("12.5")).startsWith("+"))
    }

    @Test
    fun `падение со знаком минус`() {
        assertTrue(WexFormat.signedFiat(BigDecimal("-12.5")).startsWith("−"))
    }

    // ── Адреса ────────────────────────────────────────────────

    @Test
    fun `EVM адрес сокращается по середине`() {
        val addr = "0x03f2C1a4B5d6E7f8A9b0C1d2E3f4A5b6C7d8886E"
        val short = WexFormat.shortAddress(addr)
        assertTrue("Начало должно сохраниться: $short", short.startsWith("0x03f2"))
        assertTrue("Хвост должен сохраниться: $short", short.endsWith("886E"))
        assertTrue("Середина должна быть скрыта: $short", short.contains("…"))
    }

    @Test
    fun `bech32 адрес сохраняет больше знаков в начале`() {
        val addr = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"
        val short = WexFormat.shortAddress(addr)
        assertTrue(short.startsWith("bc1qcr8t"))
        assertTrue(short.endsWith("306fyu"))
    }

    @Test
    fun `подмена адреса остаётся заметной после сокращения`() {
        // Атакующий подбирает совпадающие края; расходится середина.
        val real = "0x03f2C1a4B5d6E7f8A9b0C1d2E3f4A5b6C7d8886E"
        val fake = "0x03f2FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF886E"
        assertEquals(
            "Края совпадают — значит проверка на них бесполезна",
            WexFormat.shortAddress(real),
            WexFormat.shortAddress(fake),
        )
    }

    @Test
    fun `короткий адрес не сокращается`() {
        assertEquals("0xabc", WexFormat.shortAddress("0xabc"))
    }

    @Test
    fun `полный адрес разбивается на группы по четыре`() {
        val grouped = WexFormat.groupedAddress("bc1qcr8te4kr609gcaw")
        assertEquals("bc1q cr8t e4kr 609g caw", grouped)
    }

    @Test
    fun `хеш сокращается сильнее адреса`() {
        val hash = "0x26c6928bdbef89004e3a8b60afb2685edbb5eaf15e073e6712fd4bcd69b136e4"
        val short = WexFormat.shortHash(hash)
        assertTrue(short.length < 20)
        assertTrue(short.contains("…"))
    }

    // ── Комиссия ──────────────────────────────────────────────

    @Test
    fun `комиссия показывается в двух единицах`() {
        val result = WexFormat.fee(BigDecimal("0.0004"), "ETH", BigDecimal("1.02"))
        assertTrue("Должна быть монета: $result", result.contains("ETH"))
        assertTrue("Должен быть фиат: $result", result.contains("$1,02"))
    }

    @Test
    fun `без курса комиссия показывается только в монете`() {
        val result = WexFormat.fee(BigDecimal("0.0004"), "ETH", null)
        assertTrue(result.contains("ETH"))
        assertTrue(!result.contains("$"))
    }

    // ── Разделитель ───────────────────────────────────────────

    @Test
    fun `разделитель разрядов неразрывный`() {
        assertEquals(' ', WexFormat.GROUP_SEPARATOR)
    }

    @Test
    fun `десятичный разделитель это запятая`() {
        assertEquals(',', WexFormat.DECIMAL_SEPARATOR)
    }
}
