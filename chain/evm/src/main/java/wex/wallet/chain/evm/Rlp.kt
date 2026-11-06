@file:Suppress("MagicNumber")

package wex.wallet.chain.evm

import java.math.BigInteger

object Rlp {

    fun encode(value: ByteArray): ByteArray {
        if (value.size == 1 && value[0].toInt() and 0xFF < 0x80) {
            return value
        }
        return encodeLength(value.size, 0x80) + value
    }

    fun encode(value: BigInteger): ByteArray = encode(value.toMinimalBytes())

    fun encode(value: Long): ByteArray {
        if (value == 0L) return encode(ByteArray(0))
        return encode(BigInteger.valueOf(value))
    }

    fun encodeList(items: List<ByteArray>): ByteArray {
        val payload = items.fold(ByteArray(0)) { acc, item -> acc + item }
        return encodeLength(payload.size, 0xC0) + payload
    }

    fun encodeList(vararg items: ByteArray): ByteArray = encodeList(items.toList())

    private fun encodeLength(length: Int, offset: Int): ByteArray {
        return if (length < 56) {
            byteArrayOf((offset + length).toByte())
        } else {
            val lengthBytes = length.toBigInteger().toMinimalBytes()
            byteArrayOf((offset + 55 + lengthBytes.size).toByte()) + lengthBytes
        }
    }
}

fun BigInteger.toMinimalBytes(): ByteArray {
    if (this == BigInteger.ZERO) return ByteArray(0)
    val bytes = toByteArray()
    val start = if (bytes[0] == 0.toByte()) 1 else 0
    return bytes.copyOfRange(start, bytes.size)
}

fun ByteArray.toHex(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        sb.append(HEX_CHARS[(b.toInt() shr 4) and 0x0F])
        sb.append(HEX_CHARS[b.toInt() and 0x0F])
    }
    return sb.toString()
}

fun String.hexToBytes(): ByteArray {
    val hex = removePrefix("0x").removePrefix("0X")
    require(hex.length % 2 == 0) { "Odd-length hex string" }
    return ByteArray(hex.length / 2) { i ->
        val h = Character.digit(hex[2 * i], 16)
        val l = Character.digit(hex[2 * i + 1], 16)
        require(h != -1 && l != -1) { "Invalid hex char" }
        ((h shl 4) or l).toByte()
    }
}

private val HEX_CHARS = "0123456789abcdef".toCharArray()
