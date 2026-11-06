@file:Suppress("MagicNumber", "NestedBlockDepth")

package wex.wallet.feature.receive

import android.graphics.Bitmap
import android.graphics.Color

object QrCodeGenerator {

    fun generate(data: String, size: Int = 512): Bitmap {
        val matrix = QrEncoder.encode(data)
        val moduleSize = size / matrix.size
        val pixels = IntArray(size * size) { Color.WHITE }

        for (y in matrix.indices) {
            for (x in matrix[y].indices) {
                if (!matrix[y][x]) continue
                fillModule(pixels, x, y, moduleSize, size)
            }
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }

    private fun fillModule(
        pixels: IntArray,
        x: Int,
        y: Int,
        moduleSize: Int,
        totalSize: Int,
    ) {
        for (dy in 0 until moduleSize) {
            for (dx in 0 until moduleSize) {
                val px = x * moduleSize + dx
                val py = y * moduleSize + dy
                if (px < totalSize && py < totalSize) {
                    pixels[py * totalSize + px] = Color.BLACK
                }
            }
        }
    }
}
