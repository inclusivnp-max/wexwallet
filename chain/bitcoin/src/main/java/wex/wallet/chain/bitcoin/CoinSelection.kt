@file:Suppress("MagicNumber")

package wex.wallet.chain.bitcoin

/** Вход-кандидат: то, что уже подтверждено сетью и принадлежит кошельку. */
data class SelectableUtxo(
    val txid: String,
    val vout: Int,
    val value: Long,
    val scriptType: ScriptType = ScriptType.P2WPKH,
    val confirmed: Boolean = true,
)

sealed interface CoinSelectionResult {

    data class Success(
        val inputs: List<SelectableUtxo>,
        val sendAmount: Long,
        val fee: Long,
        val change: Long,
        val vsize: Int,
    ) : CoinSelectionResult {
        val hasChange: Boolean get() = change > 0
        val totalInput: Long get() = inputs.sumOf { it.value }
    }

    /**
     * Денег не хватает. [shortfall] — сколько именно недостаёт с учётом комиссии,
     * чтобы сообщение было конкретным, а не «недостаточно средств».
     */
    data class InsufficientFunds(
        val available: Long,
        val required: Long,
    ) : CoinSelectionResult {
        val shortfall: Long get() = (required - available).coerceAtLeast(0)
    }

    /** Сумма настолько мала, что выход был бы пылью. */
    data class AmountTooSmall(val minimum: Long) : CoinSelectionResult
}

/**
 * Выбор входов под заданную сумму и ставку комиссии.
 *
 * Стратегия — «сначала крупные»: она даёт мало входов, а значит небольшую
 * транзакцию и низкую комиссию. Платой за это является постепенное дробление
 * мелких выходов, что приемлемо для кошелька, где важнее предсказуемая комиссия.
 */
object CoinSelector {

    @Suppress("ReturnCount")
    fun select(
        utxos: List<SelectableUtxo>,
        targetAmount: Long,
        satPerVByte: Int,
        recipientType: ScriptType = ScriptType.P2WPKH,
        changeType: ScriptType = ScriptType.P2WPKH,
    ): CoinSelectionResult {
        if (targetAmount < TxSize.dustThreshold(recipientType)) {
            return CoinSelectionResult.AmountTooSmall(TxSize.dustThreshold(recipientType))
        }

        val spendable = utxos.filter { it.confirmed && it.value > 0 }
            .sortedByDescending { it.value }
        val available = spendable.sumOf { it.value }

        val chosen = mutableListOf<SelectableUtxo>()
        var accumulated = 0L

        for (utxo in spendable) {
            chosen += utxo
            accumulated += utxo.value

            val attempt = settle(chosen, accumulated, targetAmount, satPerVByte, recipientType, changeType)
            if (attempt != null) return attempt
        }

        val minimalFee = TxSize.fee(
            TxSize.vsize(spendable.map { it.scriptType }, listOf(recipientType)),
            satPerVByte,
        )
        return CoinSelectionResult.InsufficientFunds(
            available = available,
            required = targetAmount + minimalFee,
        )
    }

    /**
     * Максимум, который можно отправить одним переводом: все входы, один выход,
     * без сдачи. Комиссия вычитается из суммы, иначе перевод не пройдёт.
     */
    fun maxSendable(
        utxos: List<SelectableUtxo>,
        satPerVByte: Int,
        recipientType: ScriptType = ScriptType.P2WPKH,
    ): Long {
        val spendable = utxos.filter { it.confirmed && it.value > 0 }
        if (spendable.isEmpty()) return 0

        val vsize = TxSize.vsize(spendable.map { it.scriptType }, listOf(recipientType))
        val fee = TxSize.fee(vsize, satPerVByte)
        return (spendable.sumOf { it.value } - fee).coerceAtLeast(0)
    }

    /**
     * Пробует закрыть сумму имеющимися входами. `null`, если ещё не хватает.
     *
     * Сдача меньше порога пыли не создаётся: такой выход дороже потратить,
     * чем он стоит, поэтому остаток уходит в комиссию.
     */
    private fun settle(
        chosen: List<SelectableUtxo>,
        accumulated: Long,
        targetAmount: Long,
        satPerVByte: Int,
        recipientType: ScriptType,
        changeType: ScriptType,
    ): CoinSelectionResult.Success? {
        val inputTypes = chosen.map { it.scriptType }

        val vsizeNoChange = TxSize.vsize(inputTypes, listOf(recipientType))
        val feeNoChange = TxSize.fee(vsizeNoChange, satPerVByte)
        if (accumulated < targetAmount + feeNoChange) return null

        val vsizeWithChange = TxSize.vsize(inputTypes, listOf(recipientType, changeType))
        val feeWithChange = TxSize.fee(vsizeWithChange, satPerVByte)
        val change = accumulated - targetAmount - feeWithChange

        return if (change >= TxSize.dustThreshold(changeType)) {
            CoinSelectionResult.Success(
                inputs = chosen.toList(),
                sendAmount = targetAmount,
                fee = feeWithChange,
                change = change,
                vsize = vsizeWithChange,
            )
        } else {
            // Сдача — пыль: отдаём её майнеру вместо создания непотратимого выхода.
            CoinSelectionResult.Success(
                inputs = chosen.toList(),
                sendAmount = targetAmount,
                fee = accumulated - targetAmount,
                change = 0,
                vsize = vsizeNoChange,
            )
        }
    }
}
