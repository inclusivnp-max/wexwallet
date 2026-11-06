@file:Suppress("MagicNumber")

package wex.wallet.chain.bitcoin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinSelectorTest {

    private fun utxo(value: Long, confirmed: Boolean = true, index: Int = 0) = SelectableUtxo(
        txid = "0".repeat(63) + index,
        vout = index,
        value = value,
        confirmed = confirmed,
    )

    private fun success(result: CoinSelectionResult): CoinSelectionResult.Success {
        assertTrue("Ожидался успех, получено: $result", result is CoinSelectionResult.Success)
        return result as CoinSelectionResult.Success
    }

    @Test
    fun `выбирает один крупный вход вместо нескольких мелких`() {
        val utxos = listOf(utxo(100_000, index = 1), utxo(5_000, index = 2), utxo(5_000, index = 3))
        val result = success(CoinSelector.select(utxos, targetAmount = 50_000, satPerVByte = 5))
        assertEquals(1, result.inputs.size)
        assertEquals(100_000, result.inputs[0].value)
    }

    @Test
    fun `баланс сходится - вход равен сумме плюс комиссия плюс сдача`() {
        val utxos = listOf(utxo(100_000))
        val result = success(CoinSelector.select(utxos, targetAmount = 50_000, satPerVByte = 5))
        assertEquals(result.totalInput, result.sendAmount + result.fee + result.change)
    }

    @Test
    fun `комиссия соответствует расчётному размеру`() {
        val result = success(
            CoinSelector.select(listOf(utxo(100_000)), targetAmount = 50_000, satPerVByte = 7),
        )
        assertEquals(TxSize.fee(result.vsize, 7), result.fee)
    }

    @Test
    fun `набирает несколько входов когда одного мало`() {
        val utxos = listOf(utxo(30_000, index = 1), utxo(30_000, index = 2), utxo(30_000, index = 3))
        val result = success(CoinSelector.select(utxos, targetAmount = 70_000, satPerVByte = 2))
        assertTrue("Нужно минимум 3 входа, выбрано ${result.inputs.size}", result.inputs.size >= 3)
    }

    @Test
    fun `сдача меньше пыли уходит в комиссию а не в выход`() {
        // Подбираем вход так, чтобы остаток был заведомо ниже 294 сатоши.
        val vsizeNoChange = TxSize.vsize(listOf(ScriptType.P2WPKH), listOf(ScriptType.P2WPKH))
        val target = 50_000L
        val exact = target + TxSize.fee(vsizeNoChange, 5) + 100

        val result = success(CoinSelector.select(listOf(utxo(exact)), target, satPerVByte = 5))
        assertEquals(0, result.change)
        assertTrue("Пыль обязана уйти в комиссию", result.fee > TxSize.fee(vsizeNoChange, 5))
        assertEquals(result.totalInput, result.sendAmount + result.fee)
    }

    @Test
    fun `сдача выше пыли создаёт отдельный выход`() {
        val result = success(CoinSelector.select(listOf(utxo(200_000)), 50_000, satPerVByte = 5))
        assertTrue(result.hasChange)
        assertTrue(result.change >= TxSize.dustThreshold(ScriptType.P2WPKH))
    }

    @Test
    fun `неподтверждённые входы не используются`() {
        val utxos = listOf(utxo(100_000, confirmed = false))
        val result = CoinSelector.select(utxos, targetAmount = 50_000, satPerVByte = 5)
        assertTrue(result is CoinSelectionResult.InsufficientFunds)
    }

    @Test
    fun `нехватка средств сообщает конкретную недостачу`() {
        val utxos = listOf(utxo(10_000))
        val result = CoinSelector.select(utxos, targetAmount = 50_000, satPerVByte = 5)
        val insufficient = result as CoinSelectionResult.InsufficientFunds
        assertEquals(10_000, insufficient.available)
        assertTrue("Недостача должна быть положительной", insufficient.shortfall > 40_000)
    }

    @Test
    fun `недостача учитывает комиссию а не только сумму`() {
        val utxos = listOf(utxo(50_000))
        val result = CoinSelector.select(utxos, targetAmount = 50_000, satPerVByte = 10)
        val insufficient = result as CoinSelectionResult.InsufficientFunds
        assertTrue("Не хватает именно на комиссию", insufficient.shortfall > 0)
    }

    @Test
    fun `сумма ниже порога пыли отклоняется`() {
        val result = CoinSelector.select(listOf(utxo(100_000)), targetAmount = 100, satPerVByte = 5)
        assertTrue(result is CoinSelectionResult.AmountTooSmall)
        assertEquals(294L, (result as CoinSelectionResult.AmountTooSmall).minimum)
    }

    @Test
    fun `пустой набор входов это нехватка средств`() {
        val result = CoinSelector.select(emptyList(), targetAmount = 50_000, satPerVByte = 5)
        assertTrue(result is CoinSelectionResult.InsufficientFunds)
        assertEquals(0, (result as CoinSelectionResult.InsufficientFunds).available)
    }

    @Test
    fun `отправить максимум вычитает комиссию`() {
        val utxos = listOf(utxo(100_000))
        val max = CoinSelector.maxSendable(utxos, satPerVByte = 5)
        val expectedFee = TxSize.fee(
            TxSize.vsize(listOf(ScriptType.P2WPKH), listOf(ScriptType.P2WPKH)),
            5,
        )
        assertEquals(100_000 - expectedFee, max)
    }

    @Test
    fun `максимум действительно проходит отбор`() {
        val utxos = listOf(utxo(100_000))
        val max = CoinSelector.maxSendable(utxos, satPerVByte = 5)
        val result = success(CoinSelector.select(utxos, targetAmount = max, satPerVByte = 5))
        assertEquals(0, result.change)
        assertEquals(100_000, result.sendAmount + result.fee)
    }

    @Test
    fun `максимум использует все подтверждённые входы`() {
        val utxos = listOf(utxo(50_000, index = 1), utxo(50_000, index = 2))
        val max = CoinSelector.maxSendable(utxos, satPerVByte = 3)
        val fee = TxSize.fee(
            TxSize.vsize(List(2) { ScriptType.P2WPKH }, listOf(ScriptType.P2WPKH)),
            3,
        )
        assertEquals(100_000 - fee, max)
    }

    @Test
    fun `максимум при пустом кошельке равен нулю`() {
        assertEquals(0, CoinSelector.maxSendable(emptyList(), satPerVByte = 5))
    }

    @Test
    fun `максимум не уходит в минус при дорогой комиссии`() {
        val utxos = listOf(utxo(100))
        assertTrue(CoinSelector.maxSendable(utxos, satPerVByte = 500) >= 0)
    }

    @Test
    fun `более высокая ставка даёт большую комиссию`() {
        val cheap = success(CoinSelector.select(listOf(utxo(200_000)), 50_000, satPerVByte = 1))
        val costly = success(CoinSelector.select(listOf(utxo(200_000)), 50_000, satPerVByte = 20))
        assertTrue(costly.fee > cheap.fee)
        assertTrue(costly.change < cheap.change)
    }

    @Test
    fun `выход получателя taproot учитывается в комиссии`() {
        val toSegwit = success(
            CoinSelector.select(listOf(utxo(200_000)), 50_000, 5, recipientType = ScriptType.P2WPKH),
        )
        val toTaproot = success(
            CoinSelector.select(listOf(utxo(200_000)), 50_000, 5, recipientType = ScriptType.P2TR),
        )
        assertTrue("Выход P2TR крупнее, комиссия выше", toTaproot.fee > toSegwit.fee)
    }
}
