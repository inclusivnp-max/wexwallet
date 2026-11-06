@file:Suppress("MagicNumber")

package wex.wallet.chain.solana

object Base58 {

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        var leadingZeros = 0
        while (leadingZeros < input.size && input[leadingZeros] == 0.toByte()) {
            leadingZeros++
        }

        val encoded = StringBuilder()
        var num = java.math.BigInteger(1, input)
        val base = java.math.BigInteger.valueOf(58)
        val zero = java.math.BigInteger.ZERO

        while (num > zero) {
            val (quotient, remainder) = num.divideAndRemainder(base)
            encoded.append(ALPHABET[remainder.toInt()])
            num = quotient
        }

        repeat(leadingZeros) { encoded.append('1') }

        return encoded.reverse().toString()
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        var leadingOnes = 0
        while (leadingOnes < input.length && input[leadingOnes] == '1') {
            leadingOnes++
        }

        var num = java.math.BigInteger.ZERO
        val base = java.math.BigInteger.valueOf(58)

        for (c in input) {
            val digit = ALPHABET.indexOf(c)
            require(digit >= 0) { "Invalid Base58 character: $c" }
            num = num.multiply(base).add(java.math.BigInteger.valueOf(digit.toLong()))
        }

        val decoded = num.toByteArray()
        val stripSign = if (decoded.isNotEmpty() && decoded[0] == 0.toByte()) {
            decoded.copyOfRange(1, decoded.size)
        } else {
            decoded
        }

        return ByteArray(leadingOnes) + stripSign
    }
}
