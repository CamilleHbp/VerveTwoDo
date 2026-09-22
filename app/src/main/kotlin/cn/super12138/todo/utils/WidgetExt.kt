package cn.super12138.todo.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.updateAll
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.ui.widget.all.AllIncompleteWidget
import cn.super12138.todo.ui.widget.today.TodayTaskWidget
import cn.super12138.todo.ui.widget.upcoming.UpcomingTaskWidget

suspend fun updateTaskWidgets(context: Context) {
    AllIncompleteWidget().updateAll(context)
    TodayTaskWidget().updateAll(context)
    UpcomingTaskWidget().updateAll(context)
}

suspend fun updateTagWidgets(context: Context, old: String?, replacement: String?) {
    if (old != null && old != replacement) {
        val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
        manager.getGlanceIds(UpcomingTaskWidget::class.java).forEach { id ->
            androidx.glance.appwidget.state.updateAppWidgetState(context, id) { preferences ->
                cn.super12138.todo.ui.widget.upcoming.UpcomingWidgetPreferences.replaceTag(preferences, old, replacement)
            }
        }
    }
    updateTaskWidgets(context)
}

object GlanceTypography {
    val defaultColor: ColorProvider
        @Composable get() = GlanceTheme.colors.onSurface
    val titleLarge: TextStyle
        @Composable get() = TextStyle(
            color = defaultColor,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp
        )
    val titleMedium: TextStyle
        @Composable get() = TextStyle(
            color = defaultColor,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
    val labelLarge: TextStyle
        @Composable get() = TextStyle(
            color = defaultColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    val labelMedium: TextStyle
        @Composable get() = TextStyle(
            color = defaultColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
}

data class FixedColorProvider(val color: Color) : ColorProvider {
    override fun getColor(context: Context): Color = color
}

fun Color.toColorProvider() = FixedColorProvider(this)

@Stable
@Composable
fun Priority.glanceContainerColor(): ColorProvider =
    when (this) {
        Priority.NotUrgent -> GlanceTheme.colors.onSurfaceVariant
        Priority.NotImportant -> GlanceTheme.colors.onSurfaceVariant
        Priority.Default -> GlanceTheme.colors.secondary
        Priority.Important -> GlanceTheme.colors.tertiary
        Priority.Urgent -> GlanceTheme.colors.error
    }
