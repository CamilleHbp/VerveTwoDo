package cn.super12138.todo.ui.pages.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.super12138.todo.R
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.database.taskTags
import cn.super12138.todo.logic.formatDueTime
import cn.super12138.todo.logic.assignTagColors
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.ui.components.PriorityIcon
import cn.super12138.todo.ui.components.TopAppBarScaffold
import cn.super12138.todo.utils.toLocalDateString
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TaskDetailPage(
    taskId: Int,
    onBack: () -> Unit,
    onEdit: (TaskEntity) -> Unit,
    viewModel: TaskDetailViewModel = koinViewModel { parametersOf(taskId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val error = stringResource(R.string.task_update_failed)
    LaunchedEffect(viewModel, error) {
        viewModel.saveErrors.collect { snackbar.showSnackbar(error) }
    }
    TaskDetailScreen(state, onBack, onEdit, viewModel::setCompleted,
        viewModel::setSubtaskCompleted, viewModel::reload, snackbar)
}

@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    onBack: () -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onCompleted: (Boolean) -> Unit,
    onSubtaskCompleted: (String, Boolean) -> Unit,
    onRetry: () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() }
) {
    val task = state.task
    TopAppBarScaffold(
        title = { Text(stringResource(R.string.task_view_title)) },
        navigationIcon = {
            FilledIconButton(onClick = onBack, colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
            }
        },
        actions = {
            if (task != null && !state.loadFailed) TextButton(onClick = { onEdit(task) }, enabled = !state.isSaving) {
                Icon(painterResource(R.drawable.ic_edit_task), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.task_edit))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                state.isLoading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.loadFailed || task == null -> Column(Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(if (state.loadFailed) R.string.task_load_failed else R.string.task_not_found))
                    if (state.loadFailed) TextButton(onClick = onRetry) { Text(stringResource(R.string.overview_retry)) }
                    else TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                }
                else -> TaskDetailContent(task, state.tagColors, !state.isSaving, onEdit, onCompleted, onSubtaskCompleted)
            }
        }
    }
}

@Composable
private fun TaskDetailContent(
    task: TaskEntity,
    tagColors: Map<String, Int>,
    enabled: Boolean,
    onEdit: (TaskEntity) -> Unit,
    onCompleted: (Boolean) -> Unit,
    onSubtaskCompleted: (String, Boolean) -> Unit
) {
    val resolvedTagColors = remember(task.taskTags, tagColors) { assignTagColors(task.taskTags, tagColors) }
    LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SelectionContainer {
                Text(task.content, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
            }
            TextButton(onClick = { onCompleted(!task.isCompleted) }, enabled = enabled) {
                Icon(painterResource(if (task.isCompleted) R.drawable.ic_check_circle else R.drawable.ic_check), null,
                    Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (task.isCompleted) R.string.task_mark_incomplete else R.string.tip_mark_completed))
            }
            if (task.isCompleted) Text(stringResource(R.string.title_completed_task),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityIcon(Priority.fromFloat(task.priority), modifier = Modifier.align(Alignment.CenterVertically))
                    task.dueDateMillis?.let { date ->
                        DetailMetadata(stringResource(R.string.label_due_date),
                            date.toLocalDateString() + (task.dueTimeMinutes?.let { " · ${formatDueTime(it)}" } ?: ""))
                    }
                }
                if (task.taskTags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.taskTags.forEach { tag ->
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                            cn.super12138.todo.ui.components.EditableTag(tag, resolvedTagColors.getValue(tag), enabled = enabled)
                        }
                    }
                }
            }
        }
        if (task.details.isNotBlank()) item {
            DetailHeading(stringResource(R.string.task_details_heading))
            SelectionContainer { Text(task.details, style = MaterialTheme.typography.bodyLarge) }
        }
        item {
            DetailHeading(stringResource(R.string.task_subtasks))
            if (task.subtasks.isEmpty()) {
                Text(stringResource(R.string.task_subtasks_empty), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onEdit(task) }, enabled = enabled) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.task_subtasks_add))
                }
            } else {
                val completed = task.subtasks.count { it.isCompleted }
                Text(stringResource(R.string.task_subtasks_progress, completed, task.subtasks.size),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { completed.toFloat() / task.subtasks.size },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp))
            }
        }
        items(task.subtasks, key = { it.id }) { subtask ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .toggleable(value = subtask.isCompleted, enabled = enabled, role = Role.Checkbox,
                    onValueChange = { onSubtaskCompleted(subtask.id, it) })
                .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = subtask.isCompleted, onCheckedChange = null, enabled = enabled,
                    modifier = Modifier.size(48.dp))
                Text(subtask.content, style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                    color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(end = 8.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DetailHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp).semantics { heading() })
}

@Composable
private fun DetailMetadata(label: String, value: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
