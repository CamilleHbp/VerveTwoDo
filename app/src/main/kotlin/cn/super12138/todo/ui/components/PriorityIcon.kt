package cn.super12138.todo.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.utils.containerColor

@Composable
fun PriorityIcon(
    priority: Priority,
    modifier: Modifier = Modifier,
    tint: Color = priority.containerColor(),
    contentDescription: String? = stringResource(priority.nameRes)
) {
    Icon(
        painter = painterResource(priority.iconRes),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(20.dp)
    )
}
