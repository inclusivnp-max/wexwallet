package wex.wallet.data.accounts

import wex.wallet.chain.api.AddressDeriver
import wex.wallet.chain.api.KeyCurve
import wex.wallet.core.common.SensitiveBytes
import wex.wallet.security.mnemonic.HdKeyDeriver

object AccountDeriver {

    fun deriveAddress(seed: SensitiveBytes, deriver: AddressDeriver): String {
        val key = when (deriver.curve) {
            KeyCurve.SECP256K1 -> HdKeyDeriver.deriveBip32(seed, deriver.derivationPath)
            KeyCurve.ED25519 -> HdKeyDeriver.deriveSlip0010Ed25519(seed, deriver.derivationPath)
        }
        return try {
            deriver.deriveAddress(key.publicKey)
        } finally {
            key.close()
        }
    }

    fun deriveAllAddresses(
        seed: SensitiveBytes,
        derivers: List<AddressDeriver>,
    ): Map<AddressDeriver, String> {
        return derivers.associateWith { deriver -> deriveAddress(seed, deriver) }
    }
}
