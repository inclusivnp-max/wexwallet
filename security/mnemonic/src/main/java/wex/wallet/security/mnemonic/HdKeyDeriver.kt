@file:Suppress("MagicNumber")

package wex.wallet.security.mnemonic

import fr.acinq.bitcoin.DeterministicWallet
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import wex.wallet.core.common.SensitiveBytes
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HdKeyDeriver {

    fun deriveBip32(seed: SensitiveBytes, path: String): DerivedKey {
        return seed.use { seedBytes ->
            val master = DeterministicWallet.generate(seedBytes)
            val derived = if (path == "m") master else master.derivePrivateKey(path)
            DerivedKey(
                privateKey = SensitiveBytes.wrap(derived.secretkeybytes.toByteArray()),
                publicKey = derived.publicKey.value.toByteArray(),
                chainCode = derived.chaincode.toByteArray(),
            )
        }
    }

    fun deriveSlip0010Ed25519(seed: SensitiveBytes, path: String): DerivedKey {
        return seed.use { seedBytes ->
            val masterI = hmacSha512("ed25519 seed".toByteArray(), seedBytes)
            var key = masterI.copyOfRange(0, 32)
            var chainCode = masterI.copyOfRange(32, 64)
            masterI.fill(0)

            val indices = parsePath(path)
            for (index in indices) {
                require(index >= HARDENED_OFFSET) {
                    "SLIP-0010 ed25519 requires hardened derivation only"
                }
                val data = ByteArray(1 + 32 + 4)
                data[0] = 0x00
                key.copyInto(data, 1)
                data[33] = ((index shr 24) and 0xFF).toByte()
                data[34] = ((index shr 16) and 0xFF).toByte()
                data[35] = ((index shr 8) and 0xFF).toByte()
                data[36] = (index and 0xFF).toByte()

                val childI = hmacSha512(chainCode, data)
                data.fill(0)
                key.fill(0)
                chainCode.fill(0)
                key = childI.copyOfRange(0, 32)
                chainCode = childI.copyOfRange(32, 64)
                childI.fill(0)
            }

            val privParams = Ed25519PrivateKeyParameters(key, 0)
            val pubKey = privParams.generatePublicKey().encoded

            DerivedKey(
                privateKey = SensitiveBytes.wrap(key),
                publicKey = pubKey,
                chainCode = chainCode,
            )
        }
    }

    private const val HARDENED_OFFSET = 0x80000000L

    private fun parsePath(path: String): List<Long> {
        require(path.startsWith("m")) { "Path must start with m" }
        val remainder = path.removePrefix("m").removePrefix("/")
        if (remainder.isEmpty()) return emptyList()
        return remainder.split("/").map { segment ->
            if (segment.endsWith("'") || segment.endsWith("h")) {
                segment.dropLast(1).toLong() + HARDENED_OFFSET
            } else {
                segment.toLong()
            }
        }
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        return mac.doFinal(data)
    }
}
