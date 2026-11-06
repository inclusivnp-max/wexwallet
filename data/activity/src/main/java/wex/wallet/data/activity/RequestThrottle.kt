package wex.wallet.data.activity

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Ограничитель обращений к Etherscan.
 *
 * Бесплатный тариф допускает 5 запросов в секунду, а лента по шести сетям — это
 * двенадцать запросов залпом. Без ограничения часть из них вернулась бы
 * «rate limit», и сети выглядели бы пустыми.
 */
internal class RequestThrottle(
    permits: Int,
    private val minIntervalMillis: Long,
) {
    private val semaphore = Semaphore(permits.coerceAtLeast(1))
    private val gate = Mutex()
    private var lastStartMillis = 0L

    suspend fun <T> withPermit(block: suspend () -> T): T = semaphore.withPermit {
        awaitTurn()
        block()
    }

    private suspend fun awaitTurn() {
        if (minIntervalMillis <= 0) return
        val waitMillis = gate.withLock {
            val now = System.currentTimeMillis()
            val earliest = lastStartMillis + minIntervalMillis
            val wait = (earliest - now).coerceAtLeast(0)
            lastStartMillis = now + wait
            wait
        }
        if (waitMillis > 0) delay(waitMillis)
    }
}
