@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Шкала отступов с базой 4 dp.
 *
 * Значений вне шкалы в интерфейсе быть не должно: произвольные 13 или 18
 * не читаются как ошибка поодиночке, но именно с них вёрстка начинает разъезжаться.
 */
@Immutable
object WexSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 48.dp
    val giant: Dp = 64.dp

    /** Боковые поля экрана. */
    val screen: Dp = 16.dp

    /** Между карточками в вертикальном списке. */
    val cardGap: Dp = 12.dp

    /** Внутренний отступ карточки. */
    val cardPadding: Dp = 20.dp

    /** Между строками списка активов и операций. */
    val rowGap: Dp = 8.dp

    /** Между смысловыми разделами экрана. */
    val section: Dp = 32.dp

    /** От содержимого до главной кнопки внизу. */
    val beforeCta: Dp = 24.dp
}

/**
 * Минимальные размеры целей касания.
 *
 * Промах по кнопке отправки стоит дороже промаха в ленте новостей: подписанную
 * транзакцию не отменить, поэтому цели крупнее рекомендованного минимума.
 */
@Immutable
object WexTouch {
    /** Рекомендация Android для любой цели касания. */
    val minimum: Dp = 48.dp

    /** Кнопка цифры в PIN-клавиатуре. */
    val keypadKey: Dp = 64.dp

    /** Высота строки списка. */
    val row: Dp = 64.dp

    /** Высота основной кнопки. */
    val button: Dp = 52.dp

    /** Компактная кнопка внутри карточки. */
    val buttonSmall: Dp = 40.dp

    /** Минимальный зазор между соседними целями. */
    val gap: Dp = 8.dp
}
