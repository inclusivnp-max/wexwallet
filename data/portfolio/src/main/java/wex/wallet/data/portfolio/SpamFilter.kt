@file:Suppress("MagicNumber")

package wex.wallet.data.portfolio

import java.math.BigInteger

/**
 * Токен считается спамом, если его имя или символ выглядят как реклама.
 * Проверки консервативны: лучше показать спам, чем спрятать настоящий актив.
 */
object SpamFilter {

    private val SUSPICIOUS_PATTERNS = listOf(
        "visit", "claim", "airdrop", "reward", "bonus", "free",
        "http://", "https://", "www.", ".com", ".io", ".xyz", ".net",
        "t.me/", "telegram", "discord.gg",
    )

    private const val MAX_SYMBOL_LENGTH = 12
    private const val MAX_NAME_LENGTH = 64

    fun isSpam(token: TokenBalance): Boolean =
        isSpamText(token.symbol) ||
            isSpamText(token.name) ||
            token.symbol.length > MAX_SYMBOL_LENGTH ||
            token.name.length > MAX_NAME_LENGTH ||
            token.balance == BigInteger.ZERO

    fun filter(tokens: List<TokenBalance>): List<TokenBalance> = tokens.filterNot { isSpam(it) }

    private fun isSpamText(text: String): Boolean {
        val lower = text.lowercase()
        return SUSPICIOUS_PATTERNS.any { lower.contains(it) }
    }
}
