package wex.wallet.core.network

/**
 * Исход запроса к Etherscan.
 *
 * Различать причины обязательно: пустой список из-за отсутствия ключа, из-за того,
 * что сеть не входит в бесплатный тариф, и из-за того, что транзакций действительно нет, —
 * это три разных сообщения пользователю. Схлопывать их в один пустой список нельзя:
 * тогда «история недоступна» выглядит как «история пуста».
 */
sealed interface EtherscanResult<out T> {

    data class Ok<T>(val data: T) : EtherscanResult<T>

    /** Транзакций действительно нет — не ошибка. */
    data object Empty : EtherscanResult<Nothing>

    /** Ключ не задан в secrets.properties. */
    data object NotConfigured : EtherscanResult<Nothing>

    /**
     * Сеть существует в API V2, но не покрыта бесплатным тарифом.
     * На момент написания так отвечают Base, Optimism и BNB Chain.
     */
    data class ChainNotCovered(val chainId: Long) : EtherscanResult<Nothing>

    /** Превышен лимит запросов. */
    data object RateLimited : EtherscanResult<Nothing>

    data class Failure(val message: String) : EtherscanResult<Nothing>

    fun dataOrEmpty(): List<Any> = when (this) {
        is Ok -> data as? List<Any> ?: emptyList()
        else -> emptyList()
    }
}

/** Список из успешного ответа, во всех остальных случаях — пустой. */
fun <T> EtherscanResult<List<T>>.orEmpty(): List<T> =
    if (this is EtherscanResult.Ok) data else emptyList()

/** Человеческое объяснение, почему данных нет. `null`, если данные есть. */
fun EtherscanResult<*>.unavailableReason(): String? = when (this) {
    is EtherscanResult.Ok, EtherscanResult.Empty -> null
    EtherscanResult.NotConfigured ->
        "История недоступна: не задан ключ Etherscan"
    is EtherscanResult.ChainNotCovered ->
        "История недоступна на этой сети: нужен платный тариф Etherscan"
    EtherscanResult.RateLimited ->
        "Слишком много запросов, попробуйте позже"
    is EtherscanResult.Failure -> message
}
