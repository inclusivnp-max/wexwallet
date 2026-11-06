@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Токены движения.
 *
 * Длительности и кривые заданы централизованно, иначе каждый экран заводит свои
 * и интерфейс начинает двигаться вразнобой. Правило: чем больше площадь
 * изменения, тем дольше анимация; обратное движение всегда быстрее прямого.
 */
object Motion {

    object Duration {
        /** Отклик на касание: мгновенно, но не резко. */
        const val INSTANT = 100

        /** Смена состояния внутри элемента. */
        const val QUICK = 180

        /** Переход между экранами. */
        const val STANDARD = 280

        /** Появление крупного блока или успеха операции. */
        const val SLOW = 420
    }

    object Curve {
        /** Material 3 emphasized: медленный старт, быстрый разгон, мягкая остановка. */
        val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** Для входящих элементов. */
        val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        /** Для уходящих элементов. */
        val Accelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }

    /** Пружина для интерактивных элементов: живая, но без назойливого отскока. */
    fun <T> springy() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Пружина для элементов, которые должны «щёлкнуть» — точки PIN, галочка успеха. */
    fun <T> bouncy() = spring<T>(
        dampingRatio = 0.5f,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> enter(durationMillis: Int = Duration.STANDARD) =
        tween<T>(durationMillis = durationMillis, easing = Curve.Decelerate)

    fun <T> exit(durationMillis: Int = Duration.QUICK) =
        tween<T>(durationMillis = durationMillis, easing = Curve.Accelerate)

    fun <T> standard(durationMillis: Int = Duration.STANDARD) =
        tween<T>(durationMillis = durationMillis, easing = Curve.Emphasized)
}
