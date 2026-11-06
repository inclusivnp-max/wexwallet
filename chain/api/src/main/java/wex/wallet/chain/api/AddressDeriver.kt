package wex.wallet.chain.api

interface AddressDeriver {
    val chainId: ChainId
    val curve: KeyCurve
    val derivationPath: String
    fun deriveAddress(publicKey: ByteArray): String
}
