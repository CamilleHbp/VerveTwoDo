package cn.super12138.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.*
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.super12138.todo.R
import cn.super12138.todo.logic.tagColorPalette

fun parseTagColor(hex: String): Int? = hex.removePrefix("#")
    .takeIf { value -> value.length == 6 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } }
    ?.toIntOrNull(16)?.let { it or 0xFF000000.toInt() }

fun tagColorHex(color: Int): String = "#%06X".format(color and 0xFFFFFF)

@Composable
fun TagColorPicker(hex: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    val selectedColor = parseTagColor(hex)
    val initialColor = remember { Color(selectedColor ?: tagColorPalette.first()) }
    val controller = rememberColorPickerController()
    val latestOnChange by rememberUpdatedState(onChange)
    val brightness = controller.selectedColor.value.let { maxOf(it.red, it.green, it.blue) }
    val wheelLabel = stringResource(R.string.tag_color_wheel)
    val brightnessLabel = stringResource(R.string.tag_color_brightness)

    SideEffect { controller.enabled = enabled }
    LaunchedEffect(selectedColor) {
        selectedColor?.let {
            if (controller.selectedColor.value.toArgb() != it) controller.selectByColor(Color(it), fromUser = false)
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HsvColorPicker(
            modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally)
                .semantics { contentDescription = wheelLabel; stateDescription = hex },
            controller = controller,
            initialColor = initialColor,
            onColorChanged = { envelope ->
                if (envelope.fromUser && enabled) latestOnChange(tagColorHex(envelope.color.toArgb()))
            }
        )
        BrightnessSlider(
            modifier = Modifier.fillMaxWidth().height(48.dp).semantics {
                contentDescription = brightnessLabel
                progressBarRangeInfo = ProgressBarRangeInfo(brightness, 0f..1f)
                if (!enabled) disabled()
                setProgress { value ->
                    if (enabled) controller.setBrightness(value.coerceIn(0f, 1f), fromUser = true)
                    enabled
                }
            },
            controller = controller,
            initialColor = initialColor,
            borderRadius = 24.dp,
            borderSize = 8.dp,
            borderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            wheelRadius = 10.dp
        )
        OutlinedTextField(
            value = hex, onValueChange = onChange, enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.tag_color)) },
            leadingIcon = {
                Box(Modifier.size(24.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .background(Color(selectedColor ?: initialColor.toArgb()), CircleShape))
            },
            singleLine = true,
            isError = selectedColor == null,
            supportingText = if (selectedColor == null) ({ Text(stringResource(R.string.tag_color_hint)) }) else null
        )
    }
}
