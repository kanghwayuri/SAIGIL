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
    val BackgroundPrimary = Color(0xFFF7F8FA)
    val BackgroundSecondary = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF131417)
    val TextSecondary = Color(0xD6131417)
    val TextTertiary = Color(0xA3131417)
    val TextQuaternary = Color(0x52131417)
    val TextOnFilled = Color(0xFFFFFFFF)
    val Divider = Color(0x1A131417)
    val FilledEnabled = Color(0xFF44464E)
    val FilledPressed = Color(0xFF313236)
    val FilledDisabled = Color(0x0D131417)
    val BasicEnabled = Color(0x0D131417)
    val BasicPressed = Color(0x1A131417)
    val SwitchOn = Color(0xFF02C265)
    val SwitchOff = Color(0x4D131417)
    val Field = Color(0xFFFFFFFF)
    val Error = Color(0xFFFE3D16)
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
