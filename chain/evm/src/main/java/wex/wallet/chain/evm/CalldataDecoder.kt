@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import org.bouncycastle.jcajce.provider.digest.Keccak
import java.math.BigInteger

/**
 * Расшифровка вызова контракта.
 *
 * [isKnown] = false означает «мы честно не знаем, что делает этот вызов».
 * Показывать такой вызов как безобидный нельзя — пользователь должен видеть,
 * что предпросмотр неполный.
 */
data class DecodedCall(
    val methodId: String,
    val signature: String? = null,
    val name: String? = null,
    val args: List<DecodedArg> = emptyList(),
    val dataSize: Int = 0,
) {
    val isKnown: Boolean get() = signature != null

    /** Короткое человеческое описание для экрана подтверждения и ленты активности. */
    val humanLabel: String
        get() = when {
            methodId.isEmpty() || methodId == "0x" -> "Перевод монеты"
            name != null -> name
            else -> "Неизвестный вызов ($methodId)"
        }
}

data class DecodedArg(
    val name: String,
    val type: String,
    val value: String,
)

object CalldataDecoder {

    private val TWO: BigInteger = BigInteger.valueOf(2)

    /** Максимум uint256 — каноничный «безлимитный» approve. */
    val MAX_UINT256: BigInteger = TWO.pow(256) - BigInteger.ONE

    /** Порог, с которого approve считается фактически безлимитным. */
    val UNLIMITED_APPROVE_THRESHOLD: BigInteger = TWO.pow(255)

    private data class Signature(
        val text: String,
        val name: String,
        val argNames: List<String>,
        val argTypes: List<String>,
    )

    /**
     * Локальная база сигнатур. Селекторы не хардкодятся, а считаются из текста
     * сигнатуры — так они не могут разойтись с реальностью из-за опечатки.
     */
    private val SIGNATURES: Map<String, Signature> = listOf(
        sig("transfer(address,uint256)", "Перевод токена", listOf("получатель", "сумма")),
        sig("transferFrom(address,address,uint256)", "Перевод токена от имени", listOf("откуда", "куда", "сумма")),
        sig("approve(address,uint256)", "Разрешение на трату", listOf("кому", "сумма")),
        sig("increaseAllowance(address,uint256)", "Увеличение разрешения", listOf("кому", "на сколько")),
        sig("decreaseAllowance(address,uint256)", "Уменьшение разрешения", listOf("кому", "на сколько")),
        sig("safeTransferFrom(address,address,uint256)", "Перевод NFT", listOf("откуда", "куда", "id")),
        sig("setApprovalForAll(address,bool)", "Разрешение на все NFT", listOf("кому", "включено")),
        sig("deposit()", "Депозит", emptyList()),
        sig("withdraw(uint256)", "Вывод", listOf("сумма")),
        sig("mint(address,uint256)", "Выпуск токена", listOf("кому", "сумма")),
        sig("burn(uint256)", "Сжигание токена", listOf("сумма")),
        sig("stake(uint256)", "Стейкинг", listOf("сумма")),
        sig("unstake(uint256)", "Вывод из стейкинга", listOf("сумма")),
        sig("claim()", "Получение награды", emptyList()),
        sig(
            "swapExactTokensForTokens(uint256,uint256,address[],address,uint256)",
            "Обмен токенов",
            listOf("отдаёте", "минимум получите", "маршрут", "получатель", "дедлайн"),
        ),
        sig(
            "swapExactETHForTokens(uint256,address[],address,uint256)",
            "Обмен монеты на токены",
            listOf("минимум получите", "маршрут", "получатель", "дедлайн"),
        ),
        sig(
            "swapExactTokensForETH(uint256,uint256,address[],address,uint256)",
            "Обмен токенов на монету",
            listOf("отдаёте", "минимум получите", "маршрут", "получатель", "дедлайн"),
        ),
        sig("multicall(bytes[])", "Пакет вызовов", listOf("вызовы")),
        sig("execute(address,uint256,bytes)", "Выполнение вызова", listOf("куда", "сумма", "данные")),
    ).associateBy { selectorOf(it.text) }

    @Suppress("ReturnCount")
    fun decode(calldata: String?): DecodedCall {
        val hex = (calldata ?: "").removePrefix("0x")
        if (hex.isEmpty()) return DecodedCall(methodId = "", dataSize = 0)
        if (hex.length < 8) return DecodedCall(methodId = "0x$hex", dataSize = hex.length / 2)

        val methodId = "0x" + hex.take(8)
        val signature = SIGNATURES[methodId]
            ?: return DecodedCall(methodId = methodId, dataSize = hex.length / 2)

        return DecodedCall(
            methodId = methodId,
            signature = signature.text,
            name = signature.name,
            args = decodeArgs(hex.drop(8), signature),
            dataSize = hex.length / 2,
        )
    }

    /** Approve на сумму, которую невозможно потратить осмысленно, — красный флаг. */
    @Suppress("ReturnCount")
    fun isUnlimitedApprove(call: DecodedCall): Boolean {
        if (call.signature != "approve(address,uint256)") return false
        val amount = call.args.lastOrNull()?.value?.toBigIntegerOrNull() ?: return false
        return amount >= UNLIMITED_APPROVE_THRESHOLD
    }

    /** Адрес, которому выдаётся разрешение, если это approve. */
    fun approveSpender(call: DecodedCall): String? =
        if (call.signature == "approve(address,uint256)") call.args.firstOrNull()?.value else null

    private fun decodeArgs(body: String, signature: Signature): List<DecodedArg> =
        signature.argTypes.mapIndexedNotNull { index, type ->
            val word = body.wordAt(index) ?: return@mapIndexedNotNull null
            DecodedArg(
                name = signature.argNames.getOrElse(index) { "аргумент ${index + 1}" },
                type = type,
                value = decodeWord(word, type),
            )
        }

    private fun decodeWord(word: String, type: String): String = when (type) {
        "address" -> "0x" + word.takeLast(40)
        "bool" -> if (BigInteger(word, 16) != BigInteger.ZERO) "да" else "нет"
        "uint256" -> BigInteger(word, 16).toString()
        else -> "0x$word"
    }

    /** 32-байтное слово ABI по индексу; `null`, если данных не хватает. */
    private fun String.wordAt(index: Int): String? {
        val start = index * 64
        val end = start + 64
        return if (length >= end) substring(start, end) else null
    }

    private fun String.toBigIntegerOrNull(): BigInteger? =
        runCatching { BigInteger(this) }.getOrNull()

    private fun sig(text: String, name: String, argNames: List<String>): Signature {
        val types = text.substringAfter('(').substringBeforeLast(')')
            .split(',')
            .filter { it.isNotBlank() }
        return Signature(text = text, name = name, argNames = argNames, argTypes = types)
    }

    private fun selectorOf(signature: String): String {
        val digest = Keccak.Digest256().digest(signature.toByteArray(Charsets.US_ASCII))
        return "0x" + digest.copyOfRange(0, 4).joinToString("") { "%02x".format(it) }
    }
}
