package wex.wallet.core.common

import java.io.Closeable

class SensitiveBytes private constructor(private val data: ByteArray) : Closeable {

    @Volatile
    private var closed = false

    val size: Int get() = data.size

    fun <T> use(block: (ByteArray) -> T): T {
        check(!closed) { "SensitiveBytes already closed" }
        return block(data)
    }

    fun copyBytes(): ByteArray {
        check(!closed) { "SensitiveBytes already closed" }
        return data.copyOf()
    }

    override fun close() {
        if (!closed) {
            data.fill(0)
            closed = true
        }
    }

    val isClosed: Boolean get() = closed

    override fun toString(): String = "SensitiveBytes(${data.size} bytes)"

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    companion object {
        fun wrap(data: ByteArray): SensitiveBytes = SensitiveBytes(data)

        fun copy(source: ByteArray): SensitiveBytes = SensitiveBytes(source.copyOf())
    }
}
