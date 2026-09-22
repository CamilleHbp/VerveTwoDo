package cn.super12138.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.super12138.todo.R
import cn.super12138.todo.utils.VibrationUtils

@Composable
fun EditableTag(
    tag: String,
    color: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onEdit: (() -> Unit)? = null
) {
    var editing by rememberSaveable(tag) { mutableStateOf(false) }
    val view = LocalView.current
    val edit = stringResource(R.string.tag_edit_named, tag)
    val openEditor = onEdit ?: { editing = true }
    Row(
        modifier.clip(MaterialTheme.shapes.small)
            .combinedClickable(
                enabled = enabled, role = Role.Button, onClickLabel = edit, onLongClickLabel = edit,
                hapticFeedbackEnabled = false,
                onClick = openEditor,
                onLongClick = { VibrationUtils.performHapticFeedback(view); openEditor() }
            )
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).background(Color(color), CircleShape))
        Text(tag, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (editing) TagManagementDialog(tag, onDismiss = { editing = false }, initialColor = color)
}
