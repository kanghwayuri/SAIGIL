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
    val BackgroundPrimary = Color(0xFFF2F6F1)
    val BackgroundSecondary = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF202C27)
    val TextSecondary = Color(0xFF40524A)
    val TextTertiary = Color(0xFF52665A)
    val TextQuaternary = Color(0xFF69786E)
    val TextOnFilled = Color(0xFFFFFFFF)
    val Divider = Color(0xFFD3E3D6)
    val FilledEnabled = Color(0xFF236947)
    val FilledPressed = Color(0xFF185437)
    val FilledDisabled = Color(0xFFDCE7DF)
    val BasicEnabled = Color(0xFFE3F0E5)
    val BasicPressed = Color(0xFFCEE5D3)
    val SwitchOn = Color(0xFF238052)
    val SwitchOff = Color(0xFF82958A)
    val Field = Color(0xFFFFFFFF)
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
    val HeadlineSmall = style(FontWeight.ExtraBold, 40, 52)
    val TitleLarge = style(FontWeight.ExtraBold, 34, 44)
    val TitleMedium = style(FontWeight.ExtraBold, 30, 40)
    val TitleSmall = style(FontWeight.Bold, 26, 34)
    val BodyLarge = style(FontWeight.Bold, 24, 32)
    val BodyMedium = style(FontWeight.Bold, 22, 30)
    val BodySmall = style(FontWeight.Bold, 20, 28)
    val LabelMedium = style(FontWeight.Normal, 20, 26)
    val LabelSmall = style(FontWeight.Normal, 18, 24)
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
