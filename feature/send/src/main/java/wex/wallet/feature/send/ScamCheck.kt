@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import java.math.BigInteger

data class ScamWarning(
    val level: Level,
    val message: String,
) {
    enum class Level { INFO, WARNING, DANGER }
}

object ScamChecker {

    fun check(
        toAddress: String,
        isFirstTransfer: Boolean,
        recentAddresses: List<String> = emptyList(),
        approveAmount: BigInteger? = null,
    ): List<ScamWarning> = buildList {
        if (isFirstTransfer) {
            add(ScamWarning(ScamWarning.Level.INFO, "First transfer to this address. Double-check it."))
        }
        checkSimilarAddresses(toAddress, recentAddresses)?.let { add(it) }
        checkUnlimitedApprove(approveAmount)?.let { add(it) }
        checkContractAddress(toAddress)?.let { add(it) }
    }

    private fun checkSimilarAddresses(
        toAddress: String,
        recentAddresses: List<String>,
    ): ScamWarning? {
        val to = toAddress.lowercase()
        for (recent in recentAddresses) {
            val r = recent.lowercase()
            if (r == to) continue
            if (isSimilar(to, r)) {
                return ScamWarning(
                    ScamWarning.Level.DANGER,
                    "This address looks similar to a recent recipient " +
                        "(${shortenAddr(recent)}). Possible address poisoning.",
                )
            }
        }
        return null
    }

    private fun isSimilar(a: String, b: String): Boolean {
        val aHex = a.removePrefix("0x")
        val bHex = b.removePrefix("0x")
        if (aHex.length != 40 || bHex.length != 40) return false
        val prefixMatch = aHex.take(4) == bHex.take(4)
        val suffixMatch = aHex.takeLast(4) == bHex.takeLast(4)
        return prefixMatch && suffixMatch && aHex != bHex
    }

    @Suppress("ReturnCount")
    private fun checkUnlimitedApprove(amount: BigInteger?): ScamWarning? {
        if (amount == null) return null
        val maxUint = BigInteger.valueOf(2).pow(255)
        if (amount >= maxUint) {
            return ScamWarning(
                ScamWarning.Level.DANGER,
                "Unlimited token approval requested. " +
                    "Consider approving only the exact amount needed.",
            )
        }
        return null
    }

    @Suppress("ReturnCount")
    private fun checkContractAddress(address: String): ScamWarning? {
        if (!address.startsWith("0x") && !address.startsWith("0X")) return null
        val hex = address.removePrefix("0x").removePrefix("0X")
        if (hex.length != 40) return null
        if (hex == "0".repeat(40)) {
            return ScamWarning(
                ScamWarning.Level.DANGER,
                "Sending to the zero address (0x000...000) will burn funds.",
            )
        }
        return null
    }

    private fun shortenAddr(address: String): String {
        if (address.length <= 12) return address
        return "${address.take(6)}...${address.takeLast(4)}"
    }
}
