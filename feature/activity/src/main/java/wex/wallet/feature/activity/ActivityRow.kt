@file:Suppress("MagicNumber")

package wex.wallet.feature.activity

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import wex.wallet.core.designsystem.component.WexActivityKind
import wex.wallet.core.designsystem.component.WexActivityRow
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.theme.WexTheme
import wex.wallet.data.activity.ActivityDirection
import wex.wallet.data.activity.ActivityItem
import wex.wallet.data.activity.ActivityStatus

/** Задержка на строку и потолок каскада: дольше — и список начинает казаться медленным. */
private const val CascadeStepMs = 30
private const val CascadeMaxMs = 250

/**
 * Строка ленты.
 *
 * Вся отрисовка отдана [WexActivityRow]: направление, знак суммы и пометку
 * нерасшифрованного вызова история показывает ровно так же, как остальные
 * экраны кошелька. Здесь остаётся только перевод модели данных в термины
 * дизайн-системы и каскад появления.
 */
@Composable
internal fun ActivityRow(
    item: ActivityItem,
    nowSeconds: Long,
    index: Int,
    listShownAt: Long,
    onItemClick: (ActivityItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    WexActivityRow(
        label = item.methodLabel.ifBlank { ActivityFormat.directionLabel(item) },
        counterparty = WexFormat.shortAddress(item.counterparty),
        network = item.networkName,
        symbol = ActivityFormat.displaySymbol(item),
        amount = item.displayAmount,
        relativeTime = ActivityFormat.relativeTime(item.timestampSeconds, nowSeconds),
        kind = activityKind(item),
        modifier = modifier.cascadeAppearance(index, listShownAt),
        decrypted = item.isMethodDecoded,
        onClick = { onItemClick(item) },
    )
}

/**
 * Неудача перекрывает направление.
 *
 * У метки в углу бейджа одно место, и «перевод не прошёл» важнее того, куда он
 * шёл: баланс после отказа не изменился, а стрелка обещала бы обратное.
 * Перевод себе показываем исходящим — деньги ушли с адреса, пусть и вернулись.
 */
private fun activityKind(item: ActivityItem): WexActivityKind = when {
    item.status == ActivityStatus.FAILED -> WexActivityKind.Failed
    item.direction == ActivityDirection.INCOMING -> WexActivityKind.Incoming
    else -> WexActivityKind.Outgoing
}

/**
 * Строки въезжают по очереди: одновременное появление всего списка читается как
 * вспышка, а каскад показывает порядок сверху вниз.
 */
@Composable
private fun Modifier.cascadeAppearance(index: Int, listShownAt: Long): Modifier {
    val delayMillis = remember(index, listShownAt) { cascadeDelayMillis(index, listShownAt) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = Motion.Duration.QUICK,
            delayMillis = delayMillis,
            easing = Motion.Curve.Decelerate,
        ),
        label = "row-appear",
    )
    val shift = with(LocalDensity.current) { WexTheme.spacing.md.toPx() }
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * shift
    }
}

/**
 * Каскад отсчитывается от момента открытия списка, а не только от номера строки:
 * иначе строка, доскроленная через минуту, всё равно ждала бы свою задержку.
 */
private fun cascadeDelayMillis(index: Int, listShownAt: Long): Int {
    val elapsed = SystemClock.uptimeMillis() - listShownAt
    val delay = index.toLong() * CascadeStepMs - elapsed
    return delay.coerceIn(0L, CascadeMaxMs.toLong()).toInt()
}
