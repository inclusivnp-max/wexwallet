@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import wex.wallet.chain.api.AddressDeriver
import wex.wallet.chain.api.ChainId
import wex.wallet.chain.api.KeyCurve

class EvmAddressDeriver(
    override val derivationPath: String = "m/44'/60'/0'/0/0",
) : AddressDeriver {

    override val chainId: ChainId = ChainId.EVM
    override val curve: KeyCurve = KeyCurve.SECP256K1

    override fun deriveAddress(publicKey: ByteArray): String {
        require(publicKey.size == 33 || publicKey.size == 65) {
            "Expected compressed (33) or uncompressed (65) secp256k1 public key"
        }

        val uncompressed = if (publicKey.size == 33) {
            decompressPublicKey(publicKey)
        } else {
            publicKey
        }

        val pubKeyNoPrefix = uncompressed.copyOfRange(1, 65)
        val hash = keccak256(pubKeyNoPrefix)
        val addressBytes = hash.copyOfRange(12, 32)

        return eip55Checksum(addressBytes)
    }

    private fun decompressPublicKey(compressed: ByteArray): ByteArray {
        val ecCurve = SecP256K1Curve()
        val point = ecCurve.decodePoint(compressed)
        val encoded = point.getEncoded(false)
        return encoded
    }

    private fun keccak256(data: ByteArray): ByteArray {
        val digest = Keccak.Digest256()
        return digest.digest(data)
    }

    private fun eip55Checksum(addressBytes: ByteArray): String {
        val hex = addressBytes.toHex()
        val hashHex = keccak256(hex.toByteArray(Charsets.US_ASCII)).toHex()

        val result = StringBuilder("0x")
        for (i in hex.indices) {
            val c = hex[i]
            if (c in '0'..'9') {
                result.append(c)
            } else {
                val hashNibble = Character.digit(hashHex[i], 16)
                result.append(if (hashNibble >= 8) c.uppercaseChar() else c)
            }
        }
        return result.toString()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(HEX_CHARS[(b.toInt() shr 4) and 0x0F])
            sb.append(HEX_CHARS[b.toInt() and 0x0F])
        }
        return sb.toString()
    }

    companion object {
        private val HEX_CHARS = "0123456789abcdef".toCharArray()
    }
}
