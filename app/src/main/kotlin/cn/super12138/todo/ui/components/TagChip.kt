package cn.super12138.todo.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.super12138.todo.R

@Composable
fun TagChip(tag: String, color: Int, onRemove: () -> Unit, enabled: Boolean = true) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.material3.MaterialTheme.shapes.small,
        color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            EditableTag(tag, color, Modifier.weight(1f, fill = false), enabled)
            androidx.compose.material3.IconButton(onClick = onRemove, enabled = enabled) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.tag_remove, tag), Modifier.size(18.dp))
            }
        }
    }
}
