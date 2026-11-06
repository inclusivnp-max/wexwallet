@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Переходы между экранами.
 *
 * Направление несёт смысл: вперёд экран приезжает справа, назад — слева.
 * Пользователь считывает, где он находится в потоке, без надписей.
 */
object ScreenTransitions {

    /** Доля ширины, на которую экран сдвигается: полный сдвиг выглядит дёргано. */
    private const val SLIDE_FRACTION = 0.18f

    fun forward(): ContentTransform =
        (
            slideInHorizontally(animationSpec = Motion.enter()) { width ->
                (width * SLIDE_FRACTION).toInt()
            } + fadeIn(animationSpec = Motion.enter())
            ) togetherWith (
            slideOutHorizontally(animationSpec = Motion.exit()) { width ->
                -(width * SLIDE_FRACTION).toInt()
            } + fadeOut(animationSpec = Motion.exit())
            )

    fun backward(): ContentTransform =
        (
            slideInHorizontally(animationSpec = Motion.enter()) { width ->
                -(width * SLIDE_FRACTION).toInt()
            } + fadeIn(animationSpec = Motion.enter())
            ) togetherWith (
            slideOutHorizontally(animationSpec = Motion.exit()) { width ->
                (width * SLIDE_FRACTION).toInt()
            } + fadeOut(animationSpec = Motion.exit())
            )

    /** Смена контента без направления — например, шага загрузки на результат. */
    fun crossfade(): ContentTransform =
        fadeIn(animationSpec = Motion.enter()) togetherWith fadeOut(animationSpec = Motion.exit())
}
