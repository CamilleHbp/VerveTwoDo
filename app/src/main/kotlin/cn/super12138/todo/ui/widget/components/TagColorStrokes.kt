package cn.super12138.todo.ui.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import cn.super12138.todo.logic.assignTagColors

@Composable
fun TagColorStrokes(tags: List<String>, colors: Map<String, Int>) {
    if (tags.isEmpty()) return
    val resolved = assignTagColors(tags, colors)
    Column(GlanceModifier.padding(end = 8.dp).semantics { contentDescription = tags.joinToString(", ") }) {
        tags.chunked(6).forEach { group ->
            Row(GlanceModifier.padding(vertical = 1.dp)) {
                group.forEach { tag ->
                    Spacer(GlanceModifier.width(3.dp).height(20.dp).background(Color(resolved.getValue(tag))))
                    Spacer(GlanceModifier.width(2.dp))
                }
            }
        }
    }
}
