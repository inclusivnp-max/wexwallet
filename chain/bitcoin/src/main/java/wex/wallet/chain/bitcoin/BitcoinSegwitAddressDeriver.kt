@file:Suppress("MagicNumber")

package wex.wallet.chain.bitcoin

import fr.acinq.bitcoin.Chain
import fr.acinq.bitcoin.PublicKey
import wex.wallet.chain.api.AddressDeriver
import wex.wallet.chain.api.ChainId
import wex.wallet.chain.api.KeyCurve

class BitcoinSegwitAddressDeriver(
    private val chain: Chain = Chain.Mainnet,
    override val derivationPath: String = "m/84'/0'/0'/0/0",
) : AddressDeriver {

    override val chainId: ChainId = ChainId.BITCOIN
    override val curve: KeyCurve = KeyCurve.SECP256K1

    override fun deriveAddress(publicKey: ByteArray): String {
        require(publicKey.size == 33) {
            "Expected 33-byte compressed secp256k1 public key"
        }
        val pubKey = PublicKey(publicKey)
        return pubKey.p2wpkhAddress(chain.chainHash)
    }
}
