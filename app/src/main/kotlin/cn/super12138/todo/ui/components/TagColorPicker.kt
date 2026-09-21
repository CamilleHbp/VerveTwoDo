package cn.super12138.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.super12138.todo.R
import cn.super12138.todo.logic.tagColorPalette

fun parseTagColor(hex: String): Int? = hex.removePrefix("#").takeIf { it.length == 6 }
    ?.toIntOrNull(16)?.let { it or 0xFF000000.toInt() }

fun tagColorHex(color: Int): String = "#%06X".format(color and 0xFFFFFF)

@Composable
fun TagColorPicker(hex: String, onChange: (String) -> Unit) {
    val selectedColor = parseTagColor(hex)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tagColorPalette.forEach { value ->
            val color = Color(value)
            val label = stringResource(R.string.tag_color_value, tagColorHex(value))
            IconToggleButton(checked = value == selectedColor,
                onCheckedChange = { onChange(tagColorHex(value)) },
                modifier = Modifier.size(48.dp).semantics { contentDescription = label }) {
                Box(Modifier.size(32.dp).background(color, CircleShape)) {
                    if (value == selectedColor) Icon(painterResource(R.drawable.ic_check), null,
                        tint = if (color.luminance() > 0.45f) Color.Black else Color.White,
                        modifier = Modifier.size(32.dp))
                }
            }
        }
    }
    OutlinedTextField(value = hex, onValueChange = onChange,
        label = { Text(stringResource(R.string.tag_color)) }, singleLine = true,
        isError = selectedColor == null,
        supportingText = { Text(stringResource(R.string.tag_color_hint)) })
}
