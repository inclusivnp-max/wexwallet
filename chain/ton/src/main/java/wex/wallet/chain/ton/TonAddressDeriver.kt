@file:Suppress("MagicNumber")

package wex.wallet.chain.ton

import org.ton.api.pub.PublicKeyEd25519
import org.ton.block.AddrStd
import org.ton.contract.wallet.WalletV4R2Contract
import wex.wallet.chain.api.AddressDeriver
import wex.wallet.chain.api.ChainId
import wex.wallet.chain.api.KeyCurve

class TonAddressDeriver(
    override val derivationPath: String = "m/44'/607'/0'",
    private val bounceable: Boolean = true,
    private val testOnly: Boolean = false,
) : AddressDeriver {

    override val chainId: ChainId = ChainId.TON
    override val curve: KeyCurve = KeyCurve.ED25519

    override fun deriveAddress(publicKey: ByteArray): String {
        require(publicKey.size == 32) {
            "Expected 32-byte Ed25519 public key"
        }
        val tonPubKey = PublicKeyEd25519(publicKey)
        val data = WalletV4R2Contract.Data(seqno = 0, publicKey = tonPubKey)
        val stateInitRef = WalletV4R2Contract.stateInit(data)
        val address = AddrStd(0, stateInitRef.hash())
        return address.toString(
            userFriendly = true,
            urlSafe = true,
            testOnly = testOnly,
            bounceable = bounceable,
        )
    }
}
