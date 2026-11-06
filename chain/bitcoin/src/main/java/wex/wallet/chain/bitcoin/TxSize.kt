@file:Suppress("MagicNumber")

package wex.wallet.chain.bitcoin

import kotlin.math.ceil

enum class ScriptType {
    /** BIP84, нативный segwit. */
    P2WPKH,

    /** BIP86, taproot, трата по ключевому пути. */
    P2TR,
}

/**
 * Оценка веса транзакции до её подписи.
 *
 * Оценка нужна именно заранее: комиссия зависит от размера, размер — от числа
 * входов, а число входов выбирается исходя из комиссии. Без предварительной
 * оценки этот круг не разорвать.
 *
 * Считается в виртуальных байтах: vsize = ceil(вес / 4), где вес — это
 * несвидетельские байты × 4 плюс свидетельские × 1.
 */
object TxSize {

    // --- Несвидетельская часть, байты ---
    private const val VERSION_BYTES = 4
    private const val LOCKTIME_BYTES = 4
    private const val OUTPOINT_BYTES = 36
    private const val EMPTY_SCRIPT_SIG_BYTES = 1
    private const val SEQUENCE_BYTES = 4
    private const val OUTPUT_VALUE_BYTES = 8

    private const val P2WPKH_SCRIPT_BYTES = 22
    private const val P2TR_SCRIPT_BYTES = 34
    private const val SCRIPT_LEN_PREFIX = 1

    // --- Свидетельская часть, байты ---
    private const val SEGWIT_MARKER_AND_FLAG = 2

    /** Число элементов, подпись DER с байтом sighash и сжатый публичный ключ. */
    private const val P2WPKH_WITNESS_BYTES = 1 + (1 + 72) + (1 + 33)

    /** Число элементов и schnorr-подпись 64 байта. */
    private const val P2TR_WITNESS_BYTES = 1 + (1 + 64)

    private const val WITNESS_SCALE = 4

    /**
     * Виртуальный размер транзакции.
     *
     * Подпись DER бывает на байт короче, поэтому оценка берётся сверху:
     * занизить размер опаснее, чем завысить — транзакция с недостаточной
     * комиссией зависнет в мемпуле.
     */
    fun vsize(inputs: List<ScriptType>, outputs: List<ScriptType>): Int {
        if (inputs.isEmpty() && outputs.isEmpty()) return 0

        val nonWitness = VERSION_BYTES +
            LOCKTIME_BYTES +
            varIntSize(inputs.size) +
            varIntSize(outputs.size) +
            inputs.size * (OUTPOINT_BYTES + EMPTY_SCRIPT_SIG_BYTES + SEQUENCE_BYTES) +
            outputs.sumOf { OUTPUT_VALUE_BYTES + SCRIPT_LEN_PREFIX + scriptBytes(it) }

        val witness = if (inputs.isEmpty()) {
            0
        } else {
            SEGWIT_MARKER_AND_FLAG + inputs.sumOf { witnessBytes(it) }
        }

        val weight = nonWitness * WITNESS_SCALE + witness
        return ceil(weight.toDouble() / WITNESS_SCALE).toInt()
    }

    /** Насколько вырастет транзакция от добавления одного выхода сдачи. */
    fun changeOutputVsize(type: ScriptType): Int =
        OUTPUT_VALUE_BYTES + SCRIPT_LEN_PREFIX + scriptBytes(type)

    fun fee(vsize: Int, satPerVByte: Int): Long = vsize.toLong() * satPerVByte

    /**
     * Порог пыли: выход, который дороже потратить, чем он стоит.
     * Значения соответствуют правилам ретрансляции Bitcoin Core.
     */
    fun dustThreshold(type: ScriptType): Long = when (type) {
        ScriptType.P2WPKH -> 294
        ScriptType.P2TR -> 330
    }

    private fun scriptBytes(type: ScriptType): Int = when (type) {
        ScriptType.P2WPKH -> P2WPKH_SCRIPT_BYTES
        ScriptType.P2TR -> P2TR_SCRIPT_BYTES
    }

    private fun witnessBytes(type: ScriptType): Int = when (type) {
        ScriptType.P2WPKH -> P2WPKH_WITNESS_BYTES
        ScriptType.P2TR -> P2TR_WITNESS_BYTES
    }

    private fun varIntSize(count: Int): Int = when {
        count < 0xFD -> 1
        count <= 0xFFFF -> 3
        else -> 5
    }
}
