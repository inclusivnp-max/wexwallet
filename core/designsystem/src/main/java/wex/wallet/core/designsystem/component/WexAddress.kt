@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.component

import android.content.ClipData
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.theme.WexSpacing
import wex.wallet.core.designsystem.theme.WexTheme

/** Поле рисунка иконки копирования. */
private val CopyIconSize: Dp = WexSpacing.xl

/** Толщина штриха иконки. Оптика значка, к шкале отступов отношения не имеет. */
private val CopyIconStroke: Dp = 1.5.dp

/**
 * Сколько держится галочка.
 *
 * Двух секунд хватает, чтобы заметить подтверждение, и мало, чтобы принять его
 * за состояние кнопки: адрес копируют по нескольку раз подряд, и застрявшая
 * галочка заставила бы гадать, сработало ли повторное нажатие.
 */
private const val CopiedFeedbackMillis = 2_000L

/**
 * Сокращённый адрес.
 *
 * Прячется середина, края остаются на виду: подменяющая буфер обмена зараза
 * подбирает адрес с совпадающим началом и хвостом, а расходится как раз середина.
 * Поэтому короткий вид годится для опознания «свой — чужой», но не для сверки
 * перед отправкой — для неё есть [WexAddressFull].
 */
@Composable
fun WexAddressShort(
    address: String,
    modifier: Modifier = Modifier,
    copyable: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = WexFormat.shortAddress(address),
            style = WexTheme.type.mono,
            color = WexTheme.colors.ink2,
            maxLines = 1,
        )
        if (copyable) {
            Spacer(Modifier.width(WexTheme.spacing.xs))
            CopyButton(value = address)
        }
    }
}

/**
 * Полный адрес группами по четыре знака.
 *
 * Сплошную строку из сорока знаков глазом не сверяют вообще — её пролистывают
 * взглядом и подтверждают наугад. Группы дают опору: человек сравнивает четыре
 * знака за раз и замечает подменённую середину.
 *
 * Перенос идёт по границам групп, поэтому одна и та же группа не разрывается
 * между строками и её не приходится собирать глазами заново.
 */
@Composable
fun WexAddressFull(
    address: String,
    modifier: Modifier = Modifier,
    copyable: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = WexFormat.groupedAddress(address),
            style = WexTheme.type.mono,
            color = WexTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (copyable) {
            Spacer(Modifier.width(WexTheme.spacing.sm))
            CopyButton(value = address)
        }
    }
}

/**
 * Кнопка копирования с подтверждением на месте иконки.
 *
 * Отклик появляется там, куда смотрит палец, а не всплывашкой внизу экрана:
 * адрес чаще всего копируют, чтобы тут же вставить его в другое приложение,
 * и подтверждение должно попасться на глаза до ухода с экрана.
 */
/** Метка записи в буфере: её видит системный диалог вставки. */
private const val ClipLabel = "Адрес кошелька"

@Composable
private fun CopyButton(
    value: String,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(CopiedFeedbackMillis)
            copied = false
        }
    }

    Box(
        modifier = modifier
            .size(WexTheme.touch.buttonSmall)
            .pressable {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText(ClipLabel, value)),
                    )
                }
                copied = true
            },
        contentAlignment = Alignment.Center,
    ) {
        if (copied) {
            SuccessCheck(
                size = CopyIconSize,
                color = WexTheme.colors.positive,
                strokeWidth = CopyIconStroke,
            )
        } else {
            CopyGlyph(color = WexTheme.colors.ink3)
        }
    }
}

@Composable
private fun CopyGlyph(color: Color) {
    Canvas(modifier = Modifier.size(CopyIconSize)) {
        val stroke = Stroke(
            width = CopyIconStroke.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawCopyGlyph(color, stroke)
    }
}

/**
 * Два листа: задний виден только уголком, поэтому рисуется незамкнутой линией —
 * так штрихи не пересекаются и значок не мутнеет на маленьком размере.
 */
private fun DrawScope.drawCopyGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val back = Path().apply {
        moveTo(s * 0.3f, s * 0.3f)
        lineTo(s * 0.3f, s * 0.06f)
        lineTo(s * 0.94f, s * 0.06f)
        lineTo(s * 0.94f, s * 0.7f)
        lineTo(s * 0.7f, s * 0.7f)
    }
    drawPath(path = back, color = color, style = stroke)

    drawRoundRect(
        color = color,
        topLeft = Offset(s * 0.06f, s * 0.3f),
        size = Size(s * 0.64f, s * 0.64f),
        cornerRadius = CornerRadius(s * 0.14f),
        style = stroke,
    )
}

@PreviewLightDark
@Composable
private fun WexAddressPreview() {
    WexTheme {
        Column(
            modifier = Modifier
                .background(WexTheme.colors.ground)
                .padding(WexTheme.spacing.screen),
        ) {
            WexAddressShort(
                address = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F",
                copyable = true,
            )
            Spacer(Modifier.height(WexTheme.spacing.md))
            WexAddressFull(
                address = "bc1qcr8te4kryg7uzsd8mgdvtqr5hmg2n0sh4t6ryw",
                copyable = true,
            )
        }
    }
}
