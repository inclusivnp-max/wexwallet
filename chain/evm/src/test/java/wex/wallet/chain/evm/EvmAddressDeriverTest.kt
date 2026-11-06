package wex.wallet.chain.evm

import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class EvmAddressDeriverTest {

    private val deriver = EvmAddressDeriver()

    @Test
    fun `derive address from compressed pubkey - BIP32 vector 1 master`() {
        val compressedPubKey =
            "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2"
                .hexToByteArray()
        val address = deriver.deriveAddress(compressedPubKey)
        assertEquals("0x", address.substring(0, 2))
        assertEquals(42, address.length)
    }

    @Test
    fun `EIP-55 checksum is deterministic`() {
        val compressedPubKey =
            "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2"
                .hexToByteArray()
        val address1 = deriver.deriveAddress(compressedPubKey)
        val address2 = deriver.deriveAddress(compressedPubKey)
        assertEquals(address1, address2)
    }

    @Test
    fun `derive address from uncompressed pubkey matches compressed`() {
        val compressed =
            "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2"
                .hexToByteArray()
        val addressFromCompressed = deriver.deriveAddress(compressed)

        val curve = SecP256K1Curve()
        val point = curve.decodePoint(compressed)
        val uncompressed = point.getEncoded(false)
        val addressFromUncompressed = deriver.deriveAddress(uncompressed)

        assertEquals(addressFromCompressed, addressFromUncompressed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject invalid key size`() {
        deriver.deriveAddress(ByteArray(20))
    }

    @Test
    fun `address starts with 0x and has 40 hex chars`() {
        val key = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2"
            .hexToByteArray()
        val address = deriver.deriveAddress(key)
        assert(address.startsWith("0x"))
        assertEquals(42, address.length)
        assert(address.substring(2).all { it in "0123456789abcdefABCDEF" })
    }

    @Test
    fun `chain id and curve are correct`() {
        assertEquals(wex.wallet.chain.api.ChainId.EVM, deriver.chainId)
        assertEquals(wex.wallet.chain.api.KeyCurve.SECP256K1, deriver.curve)
    }
}
