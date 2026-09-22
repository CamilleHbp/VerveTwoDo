package cn.super12138.todo.ui.pages.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.super12138.todo.R
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.database.taskTags
import cn.super12138.todo.logic.formatDueTime
import cn.super12138.todo.logic.model.OverviewCard
import cn.super12138.todo.logic.model.OverviewCardSize
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.ui.VerveDoDefaults
import cn.super12138.todo.ui.components.PriorityIcon
import cn.super12138.todo.utils.containerColor
import cn.super12138.todo.utils.toRelativeTimeString

@Composable
fun overviewCardTitle(card: OverviewCard): String = stringResource(when (card) {
    OverviewCard.Overdue -> R.string.overview_overdue
    OverviewCard.Today -> R.string.time_today
    OverviewCard.Upcoming -> R.string.overview_next_seven_days
    OverviewCard.All -> R.string.title_all_task
    OverviewCard.Pending -> R.string.title_pending_task
    OverviewCard.Completed -> R.string.title_completed_task
})

@Composable
fun overviewFilterTitle(card: OverviewCard): String = stringResource(when (card) {
    OverviewCard.All -> R.string.overview_all_tasks
    OverviewCard.Pending -> R.string.overview_pending_tasks
    OverviewCard.Completed -> R.string.overview_completed_tasks
    else -> when (card) {
        OverviewCard.Overdue -> R.string.overview_overdue
        OverviewCard.Today -> R.string.time_today
        else -> R.string.overview_next_seven_days
    }
})

@Composable
fun overviewEmptyMessage(card: OverviewCard): String = stringResource(when (card) {
    OverviewCard.All -> R.string.overview_empty_all
    OverviewCard.Pending -> R.string.overview_empty_pending
    OverviewCard.Completed -> R.string.overview_empty_completed
    OverviewCard.Overdue -> R.string.overview_empty_overdue
    OverviewCard.Today -> R.string.overview_empty_today
    OverviewCard.Upcoming -> R.string.overview_empty_upcoming
})

@Composable
fun OverviewTaskSection(
    card: OverviewCard,
    tasks: List<TaskEntity>,
    size: OverviewCardSize,
    tagColors: Map<String, Int>,
    busyTaskIds: Set<Int>,
    onOpenTask: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onViewAll: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    onEditTask: (TaskEntity) -> Unit = onOpenTask
) {
    Surface(modifier, shape = VerveDoDefaults.defaultShape, color = VerveDoDefaults.Colors.Container) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(overviewCardTitle(card), style = MaterialTheme.typography.titleLarge,
                    color = if (card == OverviewCard.Overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).semantics { heading() })
                TextButton(onClick = onViewAll) { Text(stringResource(R.string.overview_view_all)) }
            }
            if (card == OverviewCard.Today && tasks.isNotEmpty()) {
                val complete = tasks.count { it.isCompleted }
                Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.overview_progress, complete, tasks.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { complete.toFloat() / tasks.size },
                        modifier = Modifier.fillMaxWidth())
                }
            }
            if (tasks.isEmpty()) {
                OverviewEmptyState(overviewEmptyMessage(card), onAddTask, Modifier.padding(horizontal = 16.dp))
            } else {
                val limit = if (size == OverviewCardSize.Compact) 3 else 6
                tasks.take(limit).forEachIndexed { index, task ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    OverviewTaskRow(task, tagColors, task.id in busyTaskIds, onOpenTask, onToggleTask,
                        expanded = size == OverviewCardSize.Expanded, onEdit = onEditTask)
                }
                if (tasks.size > limit) {
                    TextButton(onClick = onViewAll, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(pluralStringResource(R.plurals.overview_view_remaining, tasks.size - limit, tasks.size - limit))
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewEmptyState(message: String, onAddTask: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onAddTask) { Text(stringResource(R.string.action_add_task)) }
    }
}

@Composable
fun OverviewTaskRow(
    task: TaskEntity,
    tagColors: Map<String, Int>,
    busy: Boolean,
    onOpen: (TaskEntity) -> Unit,
    onToggle: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onEdit: (TaskEntity) -> Unit = onOpen
) {
    val context = LocalContext.current
    val toggleLabel = stringResource(if (task.isCompleted) R.string.overview_restore_task
        else R.string.widget_complete_task, task.content)
    val openLabel = stringResource(R.string.overview_open_task)
    Row(modifier.fillMaxWidth().combinedClickable(onClickLabel = openLabel,
        onClick = { onOpen(task) }, onLongClickLabel = stringResource(R.string.task_edit),
        onLongClick = { onEdit(task) })
        .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle(task) },
            enabled = !busy, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = toggleLabel })
        Column(Modifier.weight(1f).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(task.content, style = MaterialTheme.typography.titleMedium,
                maxLines = if (expanded) 3 else 2, overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface)
            if (expanded && task.details.isNotBlank()) {
                Text(task.details, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val priority = Priority.fromFloat(task.priority)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                task.dueDateMillis?.let {
                    Text(listOfNotNull(it.toRelativeTimeString(context),
                        task.dueTimeMinutes?.let(::formatDueTime)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically))
                }
                task.taskTags.forEach { tag ->
                    cn.super12138.todo.ui.components.EditableTag(tag,
                        tagColors[tag] ?: cn.super12138.todo.logic.nextTagColor(emptySet()),
                        enabled = !busy)
                }
                if (priority != Priority.Default) {
                    PriorityIcon(priority, modifier = Modifier.align(Alignment.CenterVertically),
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else priority.containerColor())
                }
            }
        }
    }
}
