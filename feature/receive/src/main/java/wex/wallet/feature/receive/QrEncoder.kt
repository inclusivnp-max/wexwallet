@file:Suppress(
    "MagicNumber",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "TooManyFunctions",
    "ComplexCondition",
    "LoopWithTooManyJumpStatements",
)

package wex.wallet.feature.receive

object QrEncoder {

    fun encode(data: String): Array<BooleanArray> {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val version = selectVersion(bytes.size)
        val size = version * 4 + 17

        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        placeFinders(matrix, reserved, size)
        placeTimingPatterns(matrix, reserved, size)
        if (version >= 2) placeAlignmentPatterns(matrix, reserved, version, size)

        val dataBits = encodeData(bytes, version)
        placeData(matrix, reserved, dataBits, size)
        applyMask(matrix, reserved, size)
        placeFormatInfo(matrix, size)

        return matrix
    }

    private fun selectVersion(dataLen: Int): Int {
        val capacities = intArrayOf(
            17, 32, 53, 78, 106, 134, 154, 192, 230, 271,
            321, 367, 425, 458, 520, 586, 644, 718, 792, 858,
        )
        for (v in capacities.indices) {
            if (dataLen <= capacities[v]) return v + 1
        }
        return 20
    }

    private fun encodeData(bytes: ByteArray, version: Int): BooleanArray {
        val totalCodewords = getTotalCodewords(version)
        val bits = mutableListOf<Boolean>()

        bits.addAll(intToBits(4, 4))
        val lengthBits = if (version <= 9) 8 else 16
        bits.addAll(intToBits(bytes.size, lengthBits))

        for (b in bytes) {
            bits.addAll(intToBits(b.toInt() and 0xFF, 8))
        }

        bits.addAll(intToBits(0, 4.coerceAtMost(totalCodewords * 8 - bits.size)))

        while (bits.size % 8 != 0) bits.add(false)

        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bits.size < totalCodewords * 8) {
            bits.addAll(intToBits(padBytes[padIdx % 2], 8))
            padIdx++
        }

        return bits.toBooleanArray()
    }

    private fun getTotalCodewords(version: Int): Int {
        val total = intArrayOf(
            26, 44, 70, 100, 134, 172, 196, 242, 292, 346,
            404, 466, 532, 581, 655, 733, 815, 901, 991, 1085,
        )
        return if (version in 1..20) total[version - 1] else total.last()
    }

    private fun intToBits(value: Int, count: Int): List<Boolean> {
        return (count - 1 downTo 0).map { (value shr it) and 1 == 1 }
    }

    private fun placeFinders(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        size: Int,
    ) {
        val positions = listOf(0 to 0, size - 7 to 0, 0 to size - 7)
        for ((row, col) in positions) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val y = row + r
                    val x = col + c
                    if (y < 0 || y >= size || x < 0 || x >= size) continue
                    reserved[y][x] = true
                    matrix[y][x] = when {
                        r in 0..6 && c in 0..6 -> {
                            r == 0 || r == 6 || c == 0 || c == 6 ||
                                (r in 2..4 && c in 2..4)
                        }
                        else -> false
                    }
                }
            }
        }
    }

    private fun placeTimingPatterns(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        size: Int,
    ) {
        for (i in 8 until size - 8) {
            if (!reserved[6][i]) {
                matrix[6][i] = i % 2 == 0
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = i % 2 == 0
                reserved[i][6] = true
            }
        }
    }

    private fun placeAlignmentPatterns(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        version: Int,
        size: Int,
    ) {
        val positions = getAlignmentPositions(version)
        for (row in positions) {
            for (col in positions) {
                if (reserved[row][col]) continue
                for (r in -2..2) {
                    for (c in -2..2) {
                        val y = row + r
                        val x = col + c
                        if (y in 0 until size && x in 0 until size) {
                            reserved[y][x] = true
                            matrix[y][x] = r == -2 || r == 2 || c == -2 || c == 2 ||
                                (r == 0 && c == 0)
                        }
                    }
                }
            }
        }
    }

    private fun getAlignmentPositions(version: Int): List<Int> {
        if (version == 1) return emptyList()
        val last = version * 4 + 10
        val first = 6
        val count = version / 7 + 2
        val step = if (count > 2) {
            val raw = ((last - first).toDouble() / (count - 1) + 0.5).toInt()
            if (raw % 2 != 0) raw + 1 else raw
        } else {
            last - first
        }

        val result = mutableListOf(first)
        var pos = last
        while (result.size < count) {
            result.add(1, pos)
            pos -= step
        }
        return result
    }

    private fun placeData(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        bits: BooleanArray,
        size: Int,
    ) {
        var bitIdx = 0
        var x = size - 1
        while (x >= 1) {
            if (x == 6) x--
            var upward = true
            for (step in 0 until size * 2) {
                val row = if (upward) size - 1 - step / 2 else step / 2
                val col = x - step % 2
                if (row < 0 || row >= size || col < 0 || col >= size) continue
                if (reserved[row][col]) continue
                if (bitIdx < bits.size) {
                    matrix[row][col] = bits[bitIdx]
                    bitIdx++
                }
            }
            x -= 2
        }
    }

    private fun applyMask(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        size: Int,
    ) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!reserved[y][x] && (y + x) % 2 == 0) {
                    matrix[y][x] = !matrix[y][x]
                }
            }
        }
    }

    private fun placeFormatInfo(matrix: Array<BooleanArray>, size: Int) {
        val formatBits = intArrayOf(1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0)
        for (i in 0..5) {
            matrix[8][i] = formatBits[i] == 1
            matrix[i][8] = formatBits[14 - i] == 1
        }
        matrix[8][7] = formatBits[6] == 1
        matrix[8][8] = formatBits[7] == 1
        matrix[7][8] = formatBits[8] == 1

        for (i in 0..6) {
            matrix[size - 1 - i][8] = formatBits[i] == 1
            matrix[8][size - 1 - i] = formatBits[14 - i] == 1
        }
        matrix[8][size - 8] = formatBits[7] == 1
        matrix[size - 8][8] = true
    }
}
