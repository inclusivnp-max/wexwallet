package wex.wallet.security.keystore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LockManager(
    private val elapsedRealtimeMs: () -> Long,
    autoLockDelayMs: Long = DEFAULT_LOCK_DELAY,
) {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastActivityTime = 0L
    var autoLockDelayMs: Long = autoLockDelayMs
        private set

    fun unlock() {
        _isLocked.value = false
        touch()
    }

    fun lock() {
        _isLocked.value = true
    }

    fun touch() {
        lastActivityTime = elapsedRealtimeMs()
    }

    fun checkAutoLock(): Boolean {
        if (!_isLocked.value && elapsedRealtimeMs() - lastActivityTime > autoLockDelayMs) {
            lock()
            return true
        }
        return false
    }

    fun setAutoLockDelay(delayMs: Long) {
        require(delayMs > 0) { "Delay must be positive" }
        this.autoLockDelayMs = delayMs
    }

    companion object {
        const val DEFAULT_LOCK_DELAY = 5 * 60 * 1000L
    }
}
