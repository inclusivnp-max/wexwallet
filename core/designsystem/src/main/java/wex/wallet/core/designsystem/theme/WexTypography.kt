@file:Suppress("MagicNumber")

package wex.wallet.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import wex.wallet.core.designsystem.R

/**
 * Manrope — интерфейс и суммы, IBM Plex Mono — адреса и хеши.
 *
 * Manrope подключается одним переменным файлом: пять статических начертаний
 * весили бы вчетверо больше, а в кошельке лишний вес пакета оплачивает
 * пользователь трафиком при каждом обновлении.
 */
private fun manropeWeight(weight: Int) = Font(
    resId = R.font.manrope_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(
    manropeWeight(400),
    manropeWeight(500),
    manropeWeight(600),
    manropeWeight(700),
    manropeWeight(800),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

/**
 * Табличные цифры: ширина знака постоянна.
 *
 * Баланс перерисовывается каждые несколько секунд. С пропорциональными цифрами
 * число дёргается по горизонтали при смене разряда, и это читается как сбой
 * отрисовки, а не как свежие данные.
 */
private const val TABULAR = "tnum"

@Immutable
@Suppress("LongParameterList")
class WexTypography(
    /** Баланс на главном и сумма на подтверждении. */
    val display: TextStyle,
    /** Заголовок экрана. */
    val title: TextStyle,
    /** Заголовок блока внутри экрана. */
    val headline: TextStyle,
    /** Основной текст в карточках. */
    val bodyLarge: TextStyle,
    /** Основной текст. */
    val body: TextStyle,
    /** Подписи полей, названия строк. */
    val label: TextStyle,
    /** Метка раздела. Моноширинная, верхний регистр. */
    val caption: TextStyle,
    /** Адреса, хеши, слова seed-фразы. */
    val mono: TextStyle,
    /** Сумма внутри строки списка. */
    val amount: TextStyle,
    /** Второстепенное значение под суммой. */
    val amountSecondary: TextStyle,
)

internal val WexTypographyDefaults = WexTypography(
    display = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.4).sp,
        fontFeatureSettings = TABULAR,
    ),
    title = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.53).sp,
    ),
    headline = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.27).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    body = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    label = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    caption = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.38.sp,
    ),
    mono = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    amount = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.12).sp,
        fontFeatureSettings = TABULAR,
    ),
    amountSecondary = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        fontFeatureSettings = TABULAR,
    ),
)
