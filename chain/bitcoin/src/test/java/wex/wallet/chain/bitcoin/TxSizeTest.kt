@file:Suppress("MagicNumber")

package wex.wallet.chain.bitcoin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Эталонные размеры взяты из общеизвестных значений для segwit-транзакций:
 * P2WPKH 1-вход/1-выход ≈ 110 vB, 1/2 ≈ 141 vB, 2/2 ≈ 209 vB,
 * P2TR 1/1 ≈ 111 vB. Ошибка здесь напрямую means неверная комиссия.
 */
class TxSizeTest {

    @Test
    fun `P2WPKH один вход один выход`() {
        val vsize = TxSize.vsize(listOf(ScriptType.P2WPKH), listOf(ScriptType.P2WPKH))
        assertEquals(110, vsize)
    }

    @Test
    fun `P2WPKH один вход два выхода`() {
        val vsize = TxSize.vsize(
            listOf(ScriptType.P2WPKH),
            listOf(ScriptType.P2WPKH, ScriptType.P2WPKH),
        )
        assertEquals(141, vsize)
    }

    @Test
    fun `P2WPKH два входа два выхода`() {
        val vsize = TxSize.vsize(
            listOf(ScriptType.P2WPKH, ScriptType.P2WPKH),
            listOf(ScriptType.P2WPKH, ScriptType.P2WPKH),
        )
        assertEquals(209, vsize)
    }

    @Test
    fun `P2TR один вход один выход`() {
        val vsize = TxSize.vsize(listOf(ScriptType.P2TR), listOf(ScriptType.P2TR))
        assertEquals(111, vsize)
    }

    @Test
    fun `taproot вход легче segwit входа`() {
        val taproot = TxSize.vsize(listOf(ScriptType.P2TR), listOf(ScriptType.P2WPKH))
        val segwit = TxSize.vsize(listOf(ScriptType.P2WPKH), listOf(ScriptType.P2WPKH))
        assertTrue("Taproot вход должен быть компактнее: $taproot vs $segwit", taproot < segwit)
    }

    @Test
    fun `taproot выход тяжелее segwit выхода`() {
        assertTrue(
            TxSize.changeOutputVsize(ScriptType.P2TR) >
                TxSize.changeOutputVsize(ScriptType.P2WPKH),
        )
    }

    @Test
    fun `размер растёт с числом входов`() {
        val one = TxSize.vsize(listOf(ScriptType.P2WPKH), listOf(ScriptType.P2WPKH))
        val two = TxSize.vsize(List(2) { ScriptType.P2WPKH }, listOf(ScriptType.P2WPKH))
        val three = TxSize.vsize(List(3) { ScriptType.P2WPKH }, listOf(ScriptType.P2WPKH))
        assertTrue(one < two && two < three)
        // Каждый дополнительный вход добавляет примерно 68 vB.
        assertEquals(68, two - one)
        assertEquals(68, three - two)
    }

    @Test
    fun `varint учитывается при большом числе входов`() {
        val small = TxSize.vsize(List(252) { ScriptType.P2WPKH }, listOf(ScriptType.P2WPKH))
        val large = TxSize.vsize(List(253) { ScriptType.P2WPKH }, listOf(ScriptType.P2WPKH))
        // Один вход даёт +68 vB, плюс расширение varint с 1 до 3 байт даёт ещё +2.
        assertEquals(70, large - small)
    }

    @Test
    fun `пустая транзакция имеет нулевой размер`() {
        assertEquals(0, TxSize.vsize(emptyList(), emptyList()))
    }

    @Test
    fun `комиссия равна размеру на ставку`() {
        assertEquals(1_100L, TxSize.fee(110, 10))
        assertEquals(0L, TxSize.fee(110, 0))
    }

    @Test
    fun `порог пыли у taproot выше чем у segwit`() {
        assertEquals(294L, TxSize.dustThreshold(ScriptType.P2WPKH))
        assertEquals(330L, TxSize.dustThreshold(ScriptType.P2TR))
    }
}
