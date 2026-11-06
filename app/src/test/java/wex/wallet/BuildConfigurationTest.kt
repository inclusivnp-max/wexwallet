package wex.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяет договорённость открытого репозитория: сборка не требует ключей,
 * а те, что не заданы, честно перечислены и доступны коду.
 */
class BuildConfigurationTest {

    @Test
    fun `список ключей конфигурации не пуст`() {
        assertTrue(
            "secrets.defaults.properties должен объявлять хотя бы один ключ",
            BuildConfig.ALL_CONFIG_KEYS.isNotEmpty(),
        )
    }

    @Test
    fun `незаданные ключи являются подмножеством объявленных`() {
        val unknown = BuildConfig.MISSING_CONFIG_KEYS - BuildConfig.ALL_CONFIG_KEYS.toSet()
        assertEquals(
            "MISSING_CONFIG_KEYS не может содержать ключи, которых нет в ALL_CONFIG_KEYS",
            emptyList<String>(),
            unknown,
        )
    }

    @Test
    fun `сетевые эндпоинты по умолчанию заданы и не требуют ключа`() {
        val endpoints = mapOf(
            "RPC_ETHEREUM" to BuildConfig.RPC_ETHEREUM,
            "RPC_BASE" to BuildConfig.RPC_BASE,
            "RPC_SOLANA" to BuildConfig.RPC_SOLANA,
            "BITCOIN_ESPLORA_URL" to BuildConfig.BITCOIN_ESPLORA_URL,
            "TON_API_URL" to BuildConfig.TON_API_URL,
        )
        endpoints.forEach { (name, value) ->
            assertTrue(
                "$name должен иметь работающее значение по умолчанию, иначе чистый клон бесполезен",
                value.startsWith("https://"),
            )
        }
    }
}
