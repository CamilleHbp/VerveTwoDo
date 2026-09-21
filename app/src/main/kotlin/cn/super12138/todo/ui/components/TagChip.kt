package cn.super12138.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.InputChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.super12138.todo.R

@Composable
fun TagChip(tag: String, color: Int, onRemove: () -> Unit, enabled: Boolean = true) {
    val description = stringResource(R.string.tag_remove, tag)
    InputChip(
        selected = true, onClick = onRemove, enabled = enabled,
        label = { Text(tag) },
        leadingIcon = { Box(Modifier.size(10.dp).background(Color(color), CircleShape)) },
        trailingIcon = { Icon(painterResource(R.drawable.ic_close), null, Modifier.size(16.dp)) },
        modifier = Modifier.semantics { contentDescription = description }
    )
}
