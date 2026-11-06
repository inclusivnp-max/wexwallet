@file:Suppress("MagicNumber", "ReturnCount")

package wex.wallet.chain.evm

import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

object EvmSigner {

    private val CURVE_PARAMS: X9ECParameters = CustomNamedCurves.getByName("secp256k1")
    private val CURVE = ECDomainParameters(
        CURVE_PARAMS.curve,
        CURVE_PARAMS.g,
        CURVE_PARAMS.n,
        CURVE_PARAMS.h,
    )
    private val HALF_CURVE_ORDER: BigInteger = CURVE_PARAMS.n.shiftRight(1)

    fun sign(privateKey: ByteArray, transaction: EvmTransaction): ByteArray {
        val payload = transaction.rlpForSigning()
        val hash = keccak256(payload)
        val (r, s, v) = ecdsaSign(privateKey, hash)
        return transaction.rlpSigned(v, r, s)
    }

    fun signToHex(privateKey: ByteArray, transaction: EvmTransaction): String {
        return "0x" + sign(privateKey, transaction).toHex()
    }

    private fun ecdsaSign(
        privateKeyBytes: ByteArray,
        messageHash: ByteArray,
    ): Triple<BigInteger, BigInteger, Int> {
        val privKey = BigInteger(1, privateKeyBytes)
        val keyParams = ECPrivateKeyParameters(privKey, CURVE)

        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        signer.init(true, keyParams)
        val components = signer.generateSignature(messageHash)
        var r = components[0]
        var s = components[1]

        if (s > HALF_CURVE_ORDER) {
            s = CURVE_PARAMS.n - s
        }

        val pubKeyPoint = CURVE.g.multiply(privKey).normalize()
        val pubKeyBytes = pubKeyPoint.getEncoded(false)
        val v = recoverV(messageHash, r, s, pubKeyBytes)

        return Triple(r, s, v)
    }

    private fun recoverV(
        hash: ByteArray,
        r: BigInteger,
        s: BigInteger,
        publicKey: ByteArray,
    ): Int {
        for (recId in 0..1) {
            val recovered = recoverPublicKey(hash, r, s, recId) ?: continue
            if (recovered.contentEquals(publicKey)) return recId
        }
        error("Could not recover V from signature")
    }

    private fun recoverPublicKey(
        hash: ByteArray,
        r: BigInteger,
        s: BigInteger,
        recId: Int,
    ): ByteArray? {
        val n = CURVE_PARAMS.n
        val x = r.add(BigInteger.valueOf(recId.toLong() / 2) * n)
        if (x >= CURVE_PARAMS.curve.field.characteristic) return null

        val rPoint = decompressPoint(x, recId and 1 == 1) ?: return null
        if (!rPoint.multiply(n).isInfinity) return null

        val e = BigInteger(1, hash)
        val eInv = BigInteger.ZERO - e.mod(n)
        val rInv = r.modInverse(n)
        val q = sumOfTwoMultiplies(CURVE.g, eInv.mod(n), rPoint, s).multiply(rInv).normalize()
        return q.getEncoded(false)
    }

    private fun decompressPoint(
        x: BigInteger,
        yBit: Boolean,
    ): ECPoint? {
        return try {
            val compactByte = if (yBit) 0x03.toByte() else 0x02.toByte()
            val xBytes = x.toByteArray().let {
                if (it.size > 32 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
            }
            val encoded = ByteArray(33)
            encoded[0] = compactByte
            xBytes.copyInto(encoded, 33 - xBytes.size)
            CURVE_PARAMS.curve.decodePoint(encoded)
        } catch (_: Exception) {
            null
        }
    }

    private fun sumOfTwoMultiplies(
        p1: ECPoint,
        a: BigInteger,
        p2: ECPoint,
        b: BigInteger,
    ): ECPoint = p1.multiply(a).add(p2.multiply(b))

    private fun keccak256(data: ByteArray): ByteArray = Keccak.Digest256().digest(data)
}
