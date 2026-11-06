@file:Suppress("MagicNumber")

package wex.wallet.security.mnemonic

import fr.acinq.bitcoin.MnemonicCode
import wex.wallet.core.common.SensitiveBytes
import java.security.SecureRandom

object Mnemonic {

    fun generate(wordCount: Int = 24): List<String> {
        require(wordCount in setOf(12, 15, 18, 21, 24)) {
            "Word count must be 12, 15, 18, 21, or 24"
        }
        val entropyBytes = wordCount * 4 / 3
        val entropy = ByteArray(entropyBytes)
        try {
            SecureRandom().nextBytes(entropy)
            return MnemonicCode.toMnemonics(entropy).toList()
        } finally {
            entropy.fill(0)
        }
    }

    fun validate(words: List<String>) {
        MnemonicCode.validate(words)
    }

    fun isValid(words: List<String>): Boolean =
        try {
            validate(words)
            true
        } catch (_: Exception) {
            false
        }

    fun toSeed(words: List<String>, passphrase: String = ""): SensitiveBytes =
        SensitiveBytes.wrap(MnemonicCode.toSeed(words, passphrase))
}
