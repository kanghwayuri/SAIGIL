package com.gomsoo.saigil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gomsoo.saigil.R

// Pleos Connect design assets 215, light mode.
object PleosColor {
    val BackgroundPrimary = Color(0xFFF5EBDD)
    val BackgroundSecondary = Color(0xFFFFFBF4)
    val TextPrimary = Color(0xFF40382F)
    val TextSecondary = Color(0xFF5E5347)
    val TextTertiary = Color(0xFF6B5E50)
    val TextQuaternary = Color(0xFF8B8174)
    val TextOnFilled = Color(0xFFFFFBF4)
    val Divider = Color(0xFFD8C8B5)
    val FilledEnabled = Color(0xFF6B5945)
    val FilledPressed = Color(0xFF554433)
    val FilledDisabled = Color(0xFFF0E2D1)
    val BasicEnabled = Color(0xFFF0E2D1)
    val BasicPressed = Color(0xFFD8C8B5)
    val SwitchOn = Color(0xFF648368)
    val SwitchOff = Color(0xFFAF9F8B)
    val Field = Color(0xFFFFFBF4)
    val Error = Color(0xFFB44134)
}

@OptIn(ExperimentalTextApi::class)
private fun astaSans(weight: FontWeight) = Font(
    R.font.asta_sans,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val AstaSans = FontFamily(
    astaSans(FontWeight.Normal),
    astaSans(FontWeight.Bold),
    astaSans(FontWeight.ExtraBold),
)

private fun style(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    color = PleosColor.TextPrimary,
)

object PleosType {
    val HeadlineSmall = style(FontWeight.ExtraBold, 48, 64)
    val TitleLarge = style(FontWeight.ExtraBold, 40, 52)
    val TitleMedium = style(FontWeight.ExtraBold, 36, 44)
    val TitleSmall = style(FontWeight.Bold, 32, 40)
    val BodyLarge = style(FontWeight.Bold, 30, 38)
    val BodyMedium = style(FontWeight.Bold, 28, 36)
    val BodySmall = style(FontWeight.Bold, 26, 34)
    val LabelMedium = style(FontWeight.Normal, 24, 28)
    val LabelSmall = style(FontWeight.Normal, 20, 24)
}

@Composable
fun SaigilTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PleosColor.FilledEnabled,
            onPrimary = PleosColor.TextOnFilled,
            background = PleosColor.BackgroundPrimary,
            onBackground = PleosColor.TextPrimary,
            surface = PleosColor.BackgroundSecondary,
            onSurface = PleosColor.TextPrimary,
            onSurfaceVariant = PleosColor.TextSecondary,
            outline = PleosColor.Divider,
            error = PleosColor.Error,
        ),
        typography = Typography(
            headlineSmall = PleosType.HeadlineSmall,
            titleLarge = PleosType.TitleLarge,
            titleMedium = PleosType.TitleMedium,
            titleSmall = PleosType.TitleSmall,
            bodyLarge = PleosType.BodyLarge,
            bodyMedium = PleosType.BodyMedium,
            bodySmall = PleosType.BodySmall,
            labelMedium = PleosType.LabelMedium,
            labelSmall = PleosType.LabelSmall,
        ),
        content = content,
    )
}
