@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Радиусы.
 *
 * Вложенный радиус равен внешнему минус отступ: карточка 24 с полем 6 содержит
 * строку 18. При равных радиусах вложенный угол выглядит толще внешнего.
 */
@Immutable
object WexRadius {
    val chip: Dp = 10.dp
    val button: Dp = 14.dp
    val row: Dp = 18.dp
    val card: Dp = 24.dp
    val sheet: Dp = 32.dp

    /** Радиус вложенного элемента при заданном отступе от края родителя. */
    fun nested(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(0.dp)
}

@Immutable
object WexShapes {
    val chip = RoundedCornerShape(WexRadius.chip)
    val button = RoundedCornerShape(WexRadius.button)
    val row = RoundedCornerShape(WexRadius.row)
    val card = RoundedCornerShape(WexRadius.card)
    val sheet = RoundedCornerShape(topStart = WexRadius.sheet, topEnd = WexRadius.sheet)

    /** Капсула. Только для статусов — кнопкой такой формы её не делаем. */
    val pill = RoundedCornerShape(percent = 50)
}

/** Толщина кромки. Всегда 1: при 2 покой уже выглядит как активное состояние. */
val WexBorderWidth: Dp = 1.dp
