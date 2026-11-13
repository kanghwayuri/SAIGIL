package com.gomsoo.saigil.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gomsoo.saigil.R
import com.gomsoo.saigil.ui.theme.PleosColor
import com.gomsoo.saigil.ui.theme.PleosType

enum class ButtonStyle { Primary, Secondary }

/** ivi_basic_button sizes. */
enum class ButtonSize(val height: Dp, val radius: Dp, val paddingH: Dp, val minWidth: Dp, val text: TextStyle) {
    Large(88.dp, 28.dp, 36.dp, 140.dp, PleosType.BodyLarge),
    Medium(72.dp, 22.dp, 28.dp, 120.dp, PleosType.BodySmall),
    Small(60.dp, 16.dp, 22.dp, 100.dp, PleosType.LabelMedium),
}

@Composable
private fun Modifier.shrinkWhenPressed(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun PleosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Secondary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container = when {
        !enabled -> PleosColor.FilledDisabled
        style == ButtonStyle.Primary -> if (pressed) PleosColor.FilledPressed else PleosColor.FilledEnabled
        else -> if (pressed) PleosColor.BasicPressed else PleosColor.BasicEnabled
    }
    val content = when {
        !enabled -> PleosColor.TextQuaternary
        style == ButtonStyle.Primary -> PleosColor.TextOnFilled
        else -> PleosColor.TextPrimary
    }
    Box(
        modifier = modifier
            .shrinkWhenPressed(interaction)
            .defaultMinSize(minWidth = size.minWidth)
            .height(size.height)
            .clip(RoundedCornerShape(size.radius))
            .background(container)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = size.paddingH),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = size.text, color = content, textAlign = TextAlign.Center)
    }
}

@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PleosType.LabelMedium,
        color = PleosColor.Error,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
fun ChoiceOption(text: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = modifier
            .shrinkWhenPressed(interaction)
            .heightIn(min = 100.dp)
            .clip(shape)
            .background(if (selected) PleosColor.BasicEnabled else PleosColor.BackgroundSecondary)
            .selectable(selected, interaction, indication = null, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = PleosColor.FilledEnabled,
                unselectedColor = PleosColor.TextTertiary,
            ),
        )
        Text(text, style = PleosType.BodyLarge)
    }
}

@Composable
fun CheckOption(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .shrinkWhenPressed(interaction)
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (checked) PleosColor.BasicEnabled else PleosColor.BackgroundSecondary)
            .toggleable(checked, interaction, indication = null, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = PleosColor.FilledEnabled,
                uncheckedColor = PleosColor.TextTertiary,
                checkmarkColor = PleosColor.TextOnFilled,
            ),
        )
        Text(text, style = PleosType.BodyMedium)
    }
}

@Composable
fun LabeledField(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    limit: Int,
    error: String?,
    modifier: Modifier = Modifier,
    minLines: Int = 2,
) {
    val marker = stringResource(if (required) R.string.label_required else R.string.label_optional)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = PleosType.BodyMedium.copy(fontWeight = FontWeight.Normal),
        label = { Text("$label ($marker)", style = PleosType.LabelMedium) },
        supportingText = {
            Row(Modifier.fillMaxWidth()) {
                if (error != null) ErrorText(error, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                Text(
                    stringResource(R.string.action_char_count, value.length, limit),
                    style = PleosType.LabelSmall,
                    color = PleosColor.TextTertiary,
                )
            }
        },
        isError = error != null,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PleosColor.Field,
            unfocusedContainerColor = PleosColor.Field,
            errorContainerColor = PleosColor.Field,
            focusedBorderColor = PleosColor.Field,
            unfocusedBorderColor = PleosColor.Field,
            focusedLabelColor = PleosColor.TextSecondary,
            unfocusedLabelColor = PleosColor.TextTertiary,
            cursorColor = PleosColor.TextPrimary,
        ),
    )
}
