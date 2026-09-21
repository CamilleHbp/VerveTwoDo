package cn.super12138.todo.ui.widget.upcoming

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.unit.ColorProvider
import cn.super12138.todo.R
import cn.super12138.todo.logic.SettingsRepository
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.formatDueTime
import cn.super12138.todo.logic.database.taskTags
import cn.super12138.todo.ui.widget.components.TagColorStrokes
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.ui.activities.MainActivity
import cn.super12138.todo.utils.GlanceTypography
import cn.super12138.todo.utils.glanceContainerColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class UpcomingTaskWidget : GlanceAppWidget(), KoinComponent {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = get<TaskRepository>().getAllTasks()
        val categories = get<SettingsRepository>().categoriesFlow
        val initialTasks = tasks.first()
        val initialCategories = categories.first()
        val colors = get<TagRepository>().colors
        val initialColors = colors.first()
        provideContent {
            val allTasks by tasks.collectAsState(initialTasks)
            val savedCategories by categories.collectAsState(initialCategories)
            val tagColors by colors.collectAsState(initialColors)
            val preferences = currentState<Preferences>()
            val sort = UpcomingWidgetPreferences.sort(preferences)
            val selected = UpcomingWidgetPreferences.categories(preferences)
            val tags = (savedCategories + allTasks.flatMap { it.taskTags } + selected)
                .filter { it.isNotBlank() }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
            val today = LocalDate.now()
            val undoUntil = preferences[UpcomingWidgetPreferences.undoUntilKey] ?: 0L
            LaunchedEffect(undoUntil) {
                if (undoUntil > 0L) {
                    delay((undoUntil - System.currentTimeMillis()).coerceAtLeast(0L))
                    updateAppWidgetState(context, id) {
                        if (it[UpcomingWidgetPreferences.undoUntilKey] == undoUntil) UpcomingWidgetPreferences.clearUndo(it)
                    }
                    this@UpcomingTaskWidget.update(context, id)
                }
            }
            val undoId = UpcomingWidgetPreferences.undoTaskId(preferences)
            val undoTitle = preferences[UpcomingWidgetPreferences.undoTitleKey]
                ?.takeIf { allTasks.any { task -> task.id == undoId && task.isCompleted } }
            GlanceTheme {
                UpcomingWidgetContent(upcomingTaskGroups(allTasks, sort, selected, today), sort, selected,
                    tags, today, preferences[UpcomingWidgetPreferences.controlsKey] ?: true,
                    preferences[UpcomingWidgetPreferences.panelKey].orEmpty(), undoTitle,
                    UpcomingWidgetPreferences.collapsedSections(preferences), tagColors)
            }
        }
    }
}

private fun controlAction(command: String, value: String = ""): Action =
    actionRunCallback<UpcomingControlsAction>(actionParametersOf(
        UpcomingControlsAction.commandKey to command, UpcomingControlsAction.valueKey to value))

@Composable
private fun UpcomingWidgetContent(
    groups: List<UpcomingTaskGroup>, sort: UpcomingTaskSort, selected: Set<String>,
    tags: List<String>, today: LocalDate, controlsVisible: Boolean, panel: String, undoTitle: String?,
    collapsedSections: Set<String>, tagColors: Map<String, Int>
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val narrow = size.width < 280.dp
    val count = groups.sumOf { it.tasks.size }
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    val addTask = actionStartActivity(Intent(context, WidgetTaskCreationActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra(WidgetTaskCreationActivity.EXTRA_CATEGORY, selected.singleOrNull().orEmpty())
    })
    val showingPicker = controlsVisible && panel in setOf("tags", "sort")
    Box(contentAlignment = Alignment.BottomEnd, modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
        .background(GlanceTheme.colors.widgetBackground).cornerRadius(24.dp)
        .padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            if (showingPicker) {
                PickerHeader(context.getString(if (panel == "tags") R.string.widget_filter_tag else R.string.pref_sorting_method))
                if (panel == "tags") {
                    Text(context.getString(R.string.widget_tags_hint),
                        style = GlanceTypography.labelMedium.copy(color = GlanceTheme.colors.onSurfaceVariant),
                        modifier = GlanceModifier.padding(start = 8.dp, bottom = 4.dp))
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        item { TagOption(context.getString(R.string.widget_all_tags), selected.isEmpty(), controlAction("all_tags")) }
                        item { TagOption(context.getString(R.string.widget_no_tag), "" in selected, controlAction("tag", "")) }
                        items(tags) { tag -> TagOption(tag.trim(), tag in selected, controlAction("tag", tag)) }
                    }
                } else SortPicker(sort, narrow, GlanceModifier.fillMaxWidth().defaultWeight())
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().height(48.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.defaultWeight().height(48.dp).clickable(openApp)) {
                        Text(context.getString(R.string.title_upcoming_task),
                            style = GlanceTypography.titleMedium, maxLines = 1,
                            modifier = GlanceModifier.defaultWeight())
                        if (size.width >= 340.dp) Text(context.resources.getQuantityString(R.plurals.widget_task_count, count, count),
                            style = GlanceTypography.labelMedium.copy(color = GlanceTheme.colors.onSurfaceVariant), maxLines = 1)
                    }
                    WidgetIcon(if (controlsVisible) R.drawable.ic_widget_collapse else R.drawable.ic_widget_expand,
                        context.getString(if (controlsVisible) R.string.widget_hide_controls else R.string.widget_show_controls),
                        controlAction("controls"))
                }
                if (controlsVisible) {
                    val tagLabel = when {
                        selected.isEmpty() -> context.getString(R.string.widget_tags_all_short)
                        selected.size == 1 && !narrow -> context.getString(R.string.widget_filter_value,
                            selected.first().trim().ifEmpty { context.getString(R.string.widget_no_tag) })
                        else -> context.getString(R.string.widget_tags_count_short, selected.size)
                    }
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Selector(tagLabel, controlAction("panel", "tags"), GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(8.dp))
                        Selector(if (narrow) context.getString(R.string.widget_sort) else context.getString(R.string.widget_sort_value,
                            context.getString(sort.shortLabelRes())), controlAction("panel", "sort"), GlanceModifier.defaultWeight())
                    }
                } else {
                    val tagLabel = if (selected.isEmpty()) context.getString(R.string.widget_all_tags)
                        else if (selected.size <= 2) selected.sorted().joinToString(" + ") {
                            it.trim().ifEmpty { context.getString(R.string.widget_no_tag) }
                        } else context.resources.getQuantityString(R.plurals.widget_tag_count, selected.size, selected.size)
                    Text(context.getString(R.string.widget_task_summary, tagLabel, context.getString(sort.shortLabelRes())),
                        style = GlanceTypography.labelMedium.copy(color = GlanceTheme.colors.onSurfaceVariant), maxLines = 1,
                        modifier = GlanceModifier.padding(bottom = 4.dp))
                }
                if (groups.isEmpty() && undoTitle != null) {
                    Spacer(GlanceModifier.defaultWeight())
                } else if (groups.isEmpty()) {
                    Column(verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(start = 8.dp, end = 72.dp)) {
                        Text(context.getString(if (selected.isEmpty()) R.string.widget_empty_unfiltered else R.string.widget_empty_filtered),
                            style = GlanceTypography.labelLarge.copy(color = GlanceTheme.colors.onSurfaceVariant))
                        TextAction(context.getString(if (selected.isEmpty()) R.string.widget_open_tasks else R.string.widget_clear_tags),
                            if (selected.isEmpty()) openApp else controlAction("all_tags"))
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        groups.forEach { group ->
                            val collapsed = group.section.name in collapsedSections
                            val showDate = group.section !in setOf(UpcomingTaskSection.Today, UpcomingTaskSection.Tomorrow)
                            item(itemId = -100L - group.section.ordinal) { Spacer(GlanceModifier.height(8.dp)) }
                            item(itemId = -1L - group.section.ordinal) {
                                SectionHeader(group, collapsed)
                            }
                            if (!collapsed) {
                                items(group.tasks, itemId = { it.id.toLong() }) { task ->
                                    UpcomingTaskRow(task, today, showDate, tagColors,
                                        if (size.height >= 300.dp && !narrow) 2 else 1, group.isOverdue,
                                        GlanceModifier.fillMaxWidth().sectionTint(group.section.color(), alpha = 0.04f,
                                            roundBottom = task.id == group.tasks.last().id))
                                }
                            }
                        }
                        // Let the final task scroll completely above the floating add button.
                        item { Spacer(GlanceModifier.height(80.dp)) }
                    }
                }
                if (undoTitle != null) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().height(48.dp).padding(end = 72.dp)) {
                        Text(context.getString(R.string.widget_completed_task, undoTitle),
                            style = GlanceTypography.labelMedium.copy(color = GlanceTheme.colors.onSurfaceVariant),
                            maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        TextAction(context.getString(R.string.widget_undo), actionRunCallback<UndoUpcomingTaskAction>())
                    }
                }
            }
        }
        if (!showingPicker) {
            Box(modifier = GlanceModifier.padding(end = 4.dp, bottom = 12.dp)) {
                SquareIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_add),
                    contentDescription = context.getString(R.string.action_add_task),
                    onClick = addTask,
                    modifier = GlanceModifier.size(56.dp),
                    backgroundColor = GlanceTheme.colors.primary,
                    contentColor = GlanceTheme.colors.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(group: UpcomingTaskGroup, collapsed: Boolean) {
    val context = LocalContext.current
    val title = context.getString(group.section.labelRes())
    val color = group.section.color()
    val count = context.resources.getQuantityString(R.plurals.widget_task_count, group.tasks.size, group.tasks.size)
    val description = context.getString(
        if (collapsed) R.string.widget_expand_section else R.string.widget_collapse_section, title, count)
    Box(contentAlignment = Alignment.CenterStart, modifier = GlanceModifier.fillMaxWidth()
        .sectionTint(color, roundTop = true, roundBottom = collapsed)
        .clickable(controlAction("section", group.section.name))
        .semantics { contentDescription = description }) {
        Spacer(GlanceModifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(title, style = GlanceTypography.labelLarge.copy(color = color, fontWeight = FontWeight.Bold),
                maxLines = 2, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            Text(group.tasks.size.toString(), style = GlanceTypography.labelMedium.copy(color = color), maxLines = 1)
            Spacer(GlanceModifier.width(12.dp))
            Image(ImageProvider(if (collapsed) R.drawable.ic_widget_expand else R.drawable.ic_widget_collapse),
                null, colorFilter = ColorFilter.tint(color), modifier = GlanceModifier.size(16.dp))
        }
    }
}

private fun GlanceModifier.sectionTint(
    color: ColorProvider, alpha: Float = 0.08f, roundTop: Boolean = false, roundBottom: Boolean = false
): GlanceModifier {
    val shape = when {
        roundTop && !roundBottom -> R.drawable.widget_section_top_background
        roundBottom && !roundTop -> R.drawable.widget_section_bottom_background
        else -> R.drawable.widget_section_background
    }
    val tinted = background(ImageProvider(shape), colorFilter = ColorFilter.tint(color), alpha = alpha)
    return if (roundTop && roundBottom) tinted.cornerRadius(12.dp) else tinted
}

@Composable
private fun UpcomingTaskSection.color(): ColorProvider = when (this) {
    UpcomingTaskSection.Overdue -> GlanceTheme.colors.error
    UpcomingTaskSection.Today, UpcomingTaskSection.Upcoming -> GlanceTheme.colors.primary
    UpcomingTaskSection.Tomorrow, UpcomingTaskSection.ThisWeek -> GlanceTheme.colors.secondary
    UpcomingTaskSection.NextWeek -> GlanceTheme.colors.tertiary
    UpcomingTaskSection.Later -> GlanceTheme.colors.onSurfaceVariant
}

private fun UpcomingTaskSection.labelRes(): Int = when (this) {
    UpcomingTaskSection.Overdue -> R.string.widget_reschedule
    UpcomingTaskSection.Today -> R.string.time_today
    UpcomingTaskSection.Tomorrow -> R.string.time_tomorrow
    UpcomingTaskSection.ThisWeek -> R.string.widget_this_week
    UpcomingTaskSection.NextWeek -> R.string.widget_next_week
    UpcomingTaskSection.Later -> R.string.widget_later
    UpcomingTaskSection.Upcoming -> R.string.title_upcoming_task
}

@Composable
private fun WidgetIcon(icon: Int, description: String, action: Action) {
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(48.dp).clickable(action)) {
        Image(ImageProvider(icon), description, colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(22.dp))
    }
}

@Composable
private fun Selector(label: String, action: Action, modifier: GlanceModifier) {
    val narrow = LocalSize.current.width < 280.dp
    Box(contentAlignment = Alignment.Center, modifier = modifier.height(48.dp).clickable(action)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().height(32.dp)
            .background(GlanceTheme.colors.secondaryContainer).cornerRadius(16.dp).padding(horizontal = if (narrow) 8.dp else 12.dp)) {
            Text(label, style = GlanceTypography.labelMedium.copy(color = GlanceTheme.colors.onSecondaryContainer),
                maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Image(ImageProvider(R.drawable.ic_widget_expand), null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer), modifier = GlanceModifier.size(if (narrow) 12.dp else 16.dp))
        }
    }
}

@Composable
private fun PickerHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().height(48.dp)) {
        Text(title, style = GlanceTypography.titleMedium, maxLines = 1, modifier = GlanceModifier.defaultWeight())
        TextAction(LocalContext.current.getString(R.string.widget_done), controlAction("panel"))
    }
}

@Composable
private fun TextAction(label: String, action: Action) {
    Box(contentAlignment = Alignment.Center,
        modifier = GlanceModifier.height(48.dp).clickable(action).padding(horizontal = 12.dp)) {
        Spacer(GlanceModifier.width(24.dp))
        Text(label, style = GlanceTypography.labelLarge.copy(color = GlanceTheme.colors.primary), maxLines = 1)
    }
}

@Composable
private fun TagOption(label: String, selected: Boolean, action: Action) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().height(48.dp).clickable(action).padding(horizontal = 8.dp)) {
        Image(ImageProvider(if (selected) R.drawable.ic_widget_checkbox_checked else R.drawable.ic_widget_checkbox),
            if (selected) LocalContext.current.getString(R.string.tip_selected) else null,
            colorFilter = ColorFilter.tint(if (selected) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(20.dp))
        Spacer(GlanceModifier.width(12.dp))
        Text(label, style = GlanceTypography.labelLarge, maxLines = 1, modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun SortPicker(sort: UpcomingTaskSort, narrow: Boolean, modifier: GlanceModifier) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier) {
        if (narrow) {
            items(UpcomingSortField.entries) { SortOption(it, sort.field() == it, GlanceModifier.fillMaxWidth()) }
        } else UpcomingSortField.entries.chunked(2).forEach { fields ->
            item {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    SortOption(fields[0], sort.field() == fields[0], GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    SortOption(fields[1], sort.field() == fields[1], GlanceModifier.defaultWeight())
                }
            }
        }
        sort.directionLabelRes()?.let { label ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth().height(48.dp).clickable(controlAction("sort_direction")).padding(horizontal = 8.dp)) {
                    Image(ImageProvider(R.drawable.ic_sort), context.getString(R.string.widget_change_order, context.getString(label)),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary), modifier = GlanceModifier.size(20.dp))
                    Spacer(GlanceModifier.width(12.dp))
                    Text(context.getString(label), style = GlanceTypography.labelLarge.copy(color = GlanceTheme.colors.primary), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SortOption(field: UpcomingSortField, selected: Boolean, modifier: GlanceModifier) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(48.dp).clickable(controlAction("sort_field", field.name)).padding(horizontal = 8.dp)) {
        Image(ImageProvider(if (selected) R.drawable.ic_check_circle else R.drawable.ic_widget_circle),
            if (selected) LocalContext.current.getString(R.string.tip_selected) else null,
            colorFilter = ColorFilter.tint(if (selected) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(20.dp))
        Spacer(GlanceModifier.width(8.dp))
        Text(LocalContext.current.getString(field.labelRes()),
            style = GlanceTypography.labelLarge.copy(color = if (selected) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface),
            maxLines = 2, modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun UpcomingTaskRow(task: TaskEntity, today: LocalDate, showDate: Boolean, tagColors: Map<String, Int>, titleLines: Int, overdue: Boolean,
    modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    val priority = Priority.fromFloat(task.priority)
    val taskParameters = actionParametersOf(CompleteUpcomingTaskAction.taskIdKey to task.id)
    val completionAction = if (task.isCompleted) actionRunCallback<RestoreUpcomingTaskAction>(taskParameters)
        else actionRunCallback<CompleteUpcomingTaskAction>(taskParameters)
    val open = actionStartActivity(Intent(context, MainActivity::class.java).apply {
        data = ("vervetwodo://widget/task/" + task.id).toUri()
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(MainActivity.EXTRA_WIDGET_TASK_ID, task.id)
    })
    val schedule = buildList {
        if (showDate) task.dueDateMillis?.let {
            add(dateLabel(context, Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(), today))
        }
        task.dueTimeMinutes?.let { add(formatDueTime(it)) }
    }.joinToString("\n")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(end = 12.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(48.dp).clickable(completionAction)) {
            Image(ImageProvider(if (task.isCompleted) R.drawable.ic_check_circle else R.drawable.ic_widget_circle),
                context.getString(if (task.isCompleted) R.string.widget_restore_task else R.string.widget_complete_task, task.content),
                colorFilter = ColorFilter.tint(if (task.isCompleted || priority == Priority.Default) GlanceTheme.colors.onSurfaceVariant else priority.glanceContainerColor()),
                modifier = GlanceModifier.size(22.dp))
        }
        Spacer(GlanceModifier.width(4.dp))
        TagColorStrokes(task.taskTags, tagColors)
        if (schedule.isNotEmpty()) Text(schedule,
            style = GlanceTypography.labelMedium.copy(
                color = if (overdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                fontWeight = FontWeight.Normal), maxLines = 3,
            modifier = GlanceModifier.width(64.dp).padding(end = 8.dp).clickable(open))
        Box(contentAlignment = Alignment.CenterStart, modifier = GlanceModifier.defaultWeight().clickable(open)) {
            Spacer(GlanceModifier.height(48.dp))
            Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(task.content, style = GlanceTypography.titleMedium.copy(fontWeight = FontWeight.Normal,
                    color = if (task.isCompleted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None), maxLines = titleLines)
                if (priority != Priority.Default) Text(context.getString(priority.nameRes),
                    style = GlanceTypography.labelMedium.copy(
                        color = if (overdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                        fontWeight = FontWeight.Normal), maxLines = 1)
            }
        }
    }
}

private fun dateLabel(context: Context, date: LocalDate, today: LocalDate): String = when (date) {
    today -> context.getString(R.string.time_today)
    today.plusDays(1) -> context.getString(R.string.time_tomorrow)
    else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "d MMM" else "d MMM yy"))
}

private fun UpcomingTaskSort.shortLabelRes(): Int = when (this) {
    UpcomingTaskSort.DueDate -> R.string.widget_sort_soonest_short
    UpcomingTaskSort.DueDateLatest -> R.string.widget_sort_latest_short
    UpcomingTaskSort.CreatedNewest -> R.string.widget_sort_newest_short
    UpcomingTaskSort.CreatedOldest -> R.string.widget_sort_oldest_short
    UpcomingTaskSort.Priority -> R.string.widget_sort_priority
    UpcomingTaskSort.Alphabetical -> R.string.widget_sort_name_short
}

private fun UpcomingTaskSort.directionLabelRes(): Int? = when (this) {
    UpcomingTaskSort.DueDate -> R.string.widget_order_soonest
    UpcomingTaskSort.DueDateLatest -> R.string.widget_order_latest
    UpcomingTaskSort.CreatedNewest -> R.string.widget_order_newest
    UpcomingTaskSort.CreatedOldest -> R.string.widget_order_oldest
    else -> null
}

fun UpcomingSortField.labelRes(): Int = when (this) {
    UpcomingSortField.DueDate -> R.string.widget_sort_due
    UpcomingSortField.CreationDate -> R.string.widget_sort_creation
    UpcomingSortField.Priority -> R.string.widget_sort_priority
    UpcomingSortField.Alphabetical -> R.string.widget_sort_name
}
