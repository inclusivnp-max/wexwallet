@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.background

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.exp
import kotlin.math.hypot

internal class TrailPoint(val position: Offset, val bornMillis: Long)

internal class TouchPulse(val position: Offset, val bornMillis: Long)

/**
 * Состояние касания: свечение под пальцем, шлейф, круги от нажатий и отталкивание
 * пятен. Живёт отдельно от отрисовки, чтобы кадр только читал готовые значения.
 */
internal class ShaderTouchState {

    var position by mutableStateOf(Offset.Zero)
        private set

    var isPressed by mutableStateOf(false)
        private set

    private var strengthState = mutableFloatStateOf(0f)
    val strength: Float get() = strengthState.floatValue

    val trail = mutableStateListOf<TrailPoint>()
    val pulses = mutableStateListOf<TouchPulse>()

    private var lastFrameMillis = 0L

    fun press(at: Offset, nowMillis: Long) {
        position = at
        isPressed = true
        addPulse(at, nowMillis)
        addTrail(at, nowMillis, force = true)
    }

    fun move(to: Offset, nowMillis: Long) {
        if (!isPressed) return
        position = to
        addTrail(to, nowMillis, force = false)
    }

    fun release() {
        isPressed = false
    }

    /** Плавно подтягивает силу к цели: резкое включение выглядело бы дёрганым. */
    fun advance(nowMillis: Long) {
        val delta = if (lastFrameMillis == 0L) 16f else (nowMillis - lastFrameMillis).coerceIn(0L, 50L).toFloat()
        lastFrameMillis = nowMillis

        val target = if (isPressed) 1f else 0f
        val rate = if (isPressed) 0.016f else 0.006f
        var value = strengthState.floatValue
        value += (target - value) * (1f - exp(-rate * delta))
        if (!isPressed && value < 0.01f) value = 0f
        strengthState.floatValue = value

        trail.removeAll { nowMillis - it.bornMillis >= TRAIL_LIFE_MILLIS }
        pulses.removeAll { nowMillis - it.bornMillis >= PULSE_LIFE_MILLIS }
    }

    val isIdle: Boolean get() = strength <= 0.02f && trail.isEmpty() && pulses.isEmpty()

    /**
     * Отталкивание от пальца с квадратичным затуханием: рядом с пальцем сдвиг
     * заметный, на краю радиуса сходит на нет.
     */
    @Suppress("ReturnCount")
    fun push(at: Offset, reach: Float, maxShift: Float, factor: Float): Offset {
        if (strength <= 0.02f || reach <= 1f) return Offset.Zero
        val delta = at - position
        val distance = hypot(delta.x, delta.y)
        if (distance >= reach || distance < 1f) return Offset.Zero

        val falloff = 1f - distance / reach
        val shift = maxShift * falloff * falloff * strength * factor
        return Offset(delta.x / distance * shift, delta.y / distance * shift)
    }

    private fun addTrail(at: Offset, nowMillis: Long, force: Boolean) {
        val last = trail.lastOrNull()
        if (!force && last != null) {
            val moved = hypot(at.x - last.position.x, at.y - last.position.y)
            if (moved < MIN_TRAIL_STEP_PX && nowMillis - last.bornMillis < 20L) return
        }
        trail.add(TrailPoint(at, nowMillis))
        while (trail.size > TRAIL_MAX) trail.removeAt(0)
    }

    private fun addPulse(at: Offset, nowMillis: Long) {
        if (pulses.size >= PULSE_MAX) pulses.removeAt(0)
        pulses.add(TouchPulse(at, nowMillis))
    }

    companion object {
        const val TRAIL_MAX = 26
        const val TRAIL_LIFE_MILLIS = 520f
        const val PULSE_MAX = 6
        const val PULSE_LIFE_MILLIS = 700f
        private const val MIN_TRAIL_STEP_PX = 16f
    }
}
