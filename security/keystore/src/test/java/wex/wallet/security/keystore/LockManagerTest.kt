package wex.wallet.security.keystore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockManagerTest {

    @Test
    fun `starts locked`() {
        val manager = LockManager(elapsedRealtimeMs = { 0L })
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun `unlock changes state`() {
        val manager = LockManager(elapsedRealtimeMs = { 0L })
        manager.unlock()
        assertFalse(manager.isLocked.value)
    }

    @Test
    fun `lock after unlock`() {
        val manager = LockManager(elapsedRealtimeMs = { 0L })
        manager.unlock()
        manager.lock()
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun `auto-lock after delay`() {
        var now = 0L
        val manager = LockManager(
            elapsedRealtimeMs = { now },
            autoLockDelayMs = 1000L,
        )

        manager.unlock()
        assertFalse(manager.isLocked.value)

        now = 500L
        assertFalse(manager.checkAutoLock())
        assertFalse(manager.isLocked.value)

        now = 1001L
        assertTrue(manager.checkAutoLock())
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun `touch resets auto-lock timer`() {
        var now = 0L
        val manager = LockManager(
            elapsedRealtimeMs = { now },
            autoLockDelayMs = 1000L,
        )

        manager.unlock()

        now = 900L
        manager.touch()

        now = 1500L
        assertFalse(manager.checkAutoLock())

        now = 1901L
        assertTrue(manager.checkAutoLock())
    }

    @Test
    fun `no auto-lock when already locked`() {
        var now = 0L
        val manager = LockManager(
            elapsedRealtimeMs = { now },
            autoLockDelayMs = 100L,
        )

        now = 200L
        assertFalse(manager.checkAutoLock())
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun `custom auto-lock delay`() {
        var now = 0L
        val manager = LockManager(
            elapsedRealtimeMs = { now },
            autoLockDelayMs = 1000L,
        )
        manager.unlock()

        manager.setAutoLockDelay(500L)

        now = 501L
        assertTrue(manager.checkAutoLock())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative delay rejected`() {
        val manager = LockManager(elapsedRealtimeMs = { 0L })
        manager.setAutoLockDelay(-1)
    }
}
