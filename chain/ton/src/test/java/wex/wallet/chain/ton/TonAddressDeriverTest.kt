package wex.wallet.chain.ton

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TonAddressDeriverTest {

    private val deriver = TonAddressDeriver()

    @Test
    fun `bounceable address starts with EQ`() {
        val pubKey = ByteArray(32) { (it + 1).toByte() }
        val address = deriver.deriveAddress(pubKey)
        assertTrue("Bounceable address should start with EQ: $address", address.startsWith("EQ"))
    }

    @Test
    fun `non-bounceable address starts with UQ`() {
        val nonBounceable = TonAddressDeriver(bounceable = false)
        val pubKey = ByteArray(32) { (it + 1).toByte() }
        val address = nonBounceable.deriveAddress(pubKey)
        assertTrue("Non-bounceable address should start with UQ: $address", address.startsWith("UQ"))
    }

    @Test
    fun `same key produces same address`() {
        val pubKey = ByteArray(32) { (it + 1).toByte() }
        val addr1 = deriver.deriveAddress(pubKey)
        val addr2 = deriver.deriveAddress(pubKey)
        assertEquals(addr1, addr2)
    }

    @Test
    fun `different keys produce different addresses`() {
        val key1 = ByteArray(32) { (it + 1).toByte() }
        val key2 = ByteArray(32) { (it + 2).toByte() }
        assertTrue(deriver.deriveAddress(key1) != deriver.deriveAddress(key2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject wrong key size`() {
        deriver.deriveAddress(ByteArray(33))
    }

    @Test
    fun `chain id and curve are correct`() {
        assertEquals(wex.wallet.chain.api.ChainId.TON, deriver.chainId)
        assertEquals(wex.wallet.chain.api.KeyCurve.ED25519, deriver.curve)
    }

    @Test
    fun `address has valid base64url characters`() {
        val pubKey = ByteArray(32) { (it + 1).toByte() }
        val address = deriver.deriveAddress(pubKey)
        val validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        assertTrue(address.all { it in validChars })
    }
}
