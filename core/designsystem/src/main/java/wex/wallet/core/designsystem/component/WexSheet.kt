package wex.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme
import java.math.BigDecimal

/** Держатель: 36 × 4. Ширина набрана из шкалы, чтобы полоска не выпадала из сетки. */
private val HandleWidth: Dp = WexTheme.spacing.xxxl + WexTheme.spacing.xs
private val HandleHeight: Dp = WexTheme.spacing.xs

/** Тень шторки. Крупнее карточной: шторка перекрывает экран, а не лежит на нём. */
private val SheetElevation: Dp = WexTheme.spacing.xxl

private val SheetPadding = PaddingValues(
    start = WexTheme.spacing.xl,
    top = WexTheme.spacing.md,
    end = WexTheme.spacing.xl,
    bottom = WexTheme.spacing.xxl,
)

/**
 * Оформление содержимого нижней шторки.
 *
 * Показом управляет вызывающая сторона: подтверждение перевода нельзя закрывать
 * свайпом так же легко, как список фильтров, а выбор сети — можно. Если бы
 * компонент сам был `ModalBottomSheet`, эти правила пришлось бы протаскивать
 * через него параметрами; вместо этого он отвечает только за вид.
 *
 * Кромка сверху светлее, чем по бокам: шторка выезжает поверх экрана, и блик по
 * верхнему краю — единственное, что отделяет её от содержимого под ней, когда
 * тень уходит за границу экрана.
 *
 * @param title слот заголовка; пустой, когда шторка показывает только действия
 * @param content содержимое шторки
 */
@Composable
fun WexSheet(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = SheetPadding,
    title: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WexTheme.colors
    val shape = WexTheme.shapes.sheet
    // Блик гаснет на длине скругления: дальше кромка обычная, иначе бок светится.
    val litEndPx = with(LocalDensity.current) { WexTheme.radius.sheet.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = SheetElevation, shape = shape, clip = false)
            .clip(shape)
            .background(colors.surface2)
            .border(
                width = WexBorderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(colors.lit, colors.edge),
                    startY = 0f,
                    endY = litEndPx,
                ),
                shape = shape,
            )
            .padding(contentPadding),
    ) {
        SheetHandle()
        if (title != null) {
            Spacer(Modifier.height(WexTheme.spacing.lg))
            title()
        }
        Spacer(Modifier.height(WexTheme.spacing.lg))
        content()
    }
}

/**
 * Полоска-держатель.
 *
 * Ничего не делает по нажатию и намеренно: это подсказка, что шторку можно
 * утянуть вниз, а не кнопка закрытия.
 */
@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = HandleWidth, height = HandleHeight)
                .clip(RoundedCornerShape(HandleHeight / 2))
                .background(WexTheme.colors.edgeStrong),
        )
    }
}

@PreviewLightDark
@Composable
private fun WexSheetPreview() {
    WexTheme {
        Box(Modifier.background(WexTheme.colors.ground)) {
            WexSheet(
                title = {
                    Text(
                        text = "Подтвердите отправку",
                        style = WexTheme.type.title,
                        color = WexTheme.colors.ink,
                    )
                },
            ) {
                Text(
                    text = WexFormat.amount(BigDecimal("0.482"), "ETH"),
                    style = WexTheme.type.amount,
                    color = WexTheme.colors.ink,
                )
                Spacer(Modifier.height(WexTheme.spacing.sm))
                Text(
                    text = WexFormat.shortAddress("0x8f2A55949038A501F8a5E1E2d2eF6bB0b5D0F9a1"),
                    style = WexTheme.type.mono,
                    color = WexTheme.colors.ink3,
                )
                Spacer(Modifier.height(WexTheme.spacing.beforeCta))
                WexPrimaryButton(text = "Отправить", onClick = {})
            }
        }
    }
}
