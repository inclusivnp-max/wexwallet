@file:Suppress("MagicNumber")

package wex.wallet.chain.solana

import wex.wallet.chain.api.AddressDeriver
import wex.wallet.chain.api.ChainId
import wex.wallet.chain.api.KeyCurve

class SolanaAddressDeriver(
    override val derivationPath: String = "m/44'/501'/0'/0'",
) : AddressDeriver {

    override val chainId: ChainId = ChainId.SOLANA
    override val curve: KeyCurve = KeyCurve.ED25519

    override fun deriveAddress(publicKey: ByteArray): String {
        require(publicKey.size == 32) {
            "Expected 32-byte Ed25519 public key"
        }
        return Base58.encode(publicKey)
    }
}
