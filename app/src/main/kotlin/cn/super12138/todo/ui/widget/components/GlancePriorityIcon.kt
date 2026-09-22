package cn.super12138.todo.ui.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.utils.glanceContainerColor

@Composable
fun GlancePriorityIcon(
    priority: Priority,
    modifier: GlanceModifier = GlanceModifier,
    tint: ColorProvider = priority.glanceContainerColor()
) {
    Image(
        provider = ImageProvider(priority.iconRes),
        contentDescription = LocalContext.current.getString(priority.nameRes),
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(20.dp)
    )
}
