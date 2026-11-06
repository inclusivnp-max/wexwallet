package wex.wallet.security.mnemonic

import wex.wallet.core.common.SensitiveBytes
import java.io.Closeable

class DerivedKey(
    val privateKey: SensitiveBytes,
    val publicKey: ByteArray,
    val chainCode: ByteArray,
) : Closeable {

    override fun close() {
        privateKey.close()
        chainCode.fill(0)
    }

    override fun toString(): String = "DerivedKey(pub=${publicKey.size} bytes)"
}
