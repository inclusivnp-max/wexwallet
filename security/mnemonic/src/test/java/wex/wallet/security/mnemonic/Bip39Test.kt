package wex.wallet.security.mnemonic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class Bip39Test {

    @Test
    fun `generate 12 words`() {
        val words = Mnemonic.generate(wordCount = 12)
        assertEquals(12, words.size)
        assertTrue(Mnemonic.isValid(words))
    }

    @Test
    fun `generate 24 words`() {
        val words = Mnemonic.generate(wordCount = 24)
        assertEquals(24, words.size)
        assertTrue(Mnemonic.isValid(words))
    }

    @Test
    fun `validate known mnemonic`() {
        val words = ABANDON_ABOUT.split(" ")
        assertTrue(Mnemonic.isValid(words))
    }

    @Test
    fun `invalid mnemonic detected`() {
        val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon"
            .split(" ")
        assertFalse(Mnemonic.isValid(words))
    }

    @Test
    fun `vector 1 - seed with TREZOR passphrase`() {
        val seed = Mnemonic.toSeed(ABANDON_ABOUT.split(" "), "TREZOR")
        seed.use { bytes ->
            assertEquals(64, bytes.size)
            assertEquals(
                "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531" +
                    "f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04",
                bytes.toHexString(),
            )
        }
        seed.close()
    }

    @Test
    fun `vector 1 - seed with empty passphrase`() {
        val seed = Mnemonic.toSeed(ABANDON_ABOUT.split(" "), "")
        seed.use { bytes ->
            assertEquals(64, bytes.size)
            assertEquals(
                "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1" +
                    "9a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4",
                bytes.toHexString(),
            )
        }
        seed.close()
    }

    @Test
    fun `vector 2 - zoo mnemonic`() {
        val words = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong".split(" ")
        assertTrue(Mnemonic.isValid(words))
        val seed = Mnemonic.toSeed(words, "TREZOR")
        seed.use { bytes ->
            assertEquals(64, bytes.size)
            assertEquals(
                "ac27495480225222079d7be181583751e86f571027b0497b5b5d11218e0a8a13" +
                    "332572917f0f8e5a589620c6f15b11c61dee327651a14c34e18231052e48c069",
                bytes.toHexString(),
            )
        }
        seed.close()
    }

    @Test
    fun `toSeed returns SensitiveBytes that can be closed`() {
        val words = Mnemonic.generate(12)
        val seed = Mnemonic.toSeed(words)
        seed.use { bytes -> assertEquals(64, bytes.size) }
        seed.close()
        assertTrue(seed.isClosed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid word count rejected`() {
        Mnemonic.generate(wordCount = 13)
    }

    @Test
    fun `different passphrases produce different seeds`() {
        val words = ABANDON_ABOUT.split(" ")
        val seed1 = Mnemonic.toSeed(words, "")
        val seed2 = Mnemonic.toSeed(words, "test")
        val bytes1 = seed1.copyBytes()
        val bytes2 = seed2.copyBytes()
        seed1.close()
        seed2.close()
        assertFalse(bytes1.contentEquals(bytes2))
    }

    companion object {
        private const val ABANDON_ABOUT =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    }
}
