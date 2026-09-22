package cn.super12138.todo.ui.pages.editor

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import cn.super12138.todo.R
import cn.super12138.todo.constants.Constants
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.ui.VerveDoDefaults
import cn.super12138.todo.ui.components.CheckboxWithLabel
import cn.super12138.todo.ui.components.ConfirmDialog
import cn.super12138.todo.ui.components.PriorityIcon
import cn.super12138.todo.ui.components.TagTextField
import cn.super12138.todo.ui.components.TagChip
import cn.super12138.todo.ui.components.DueTimeDialog
import cn.super12138.todo.logic.assignTagColors
import cn.super12138.todo.logic.formatDueTime
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import cn.super12138.todo.ui.components.TodoFloatingActionButton
import cn.super12138.todo.ui.components.TopAppBarScaffold
import cn.super12138.todo.ui.pages.editor.components.DueDateChooser
import cn.super12138.todo.utils.VibrationUtils
import cn.super12138.todo.utils.toggleButtonShapesIn
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SharedTransitionScope.TaskAddPage(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) = TaskEditorPage(
    task = null,
    modifier = modifier
        .sharedBounds(
            sharedContentState = rememberSharedContentState(key = Constants.KEY_TODO_FAB_TRANSITION),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
        )
        .skipToLookaheadSize(), // 这个修饰符必须放后面
    onNavigateUp = onNavigateUp
)

@Composable
fun SharedTransitionScope.TaskEditPage(
    modifier: Modifier = Modifier,
    task: TaskEntity,
    onNavigateUp: () -> Unit
) = TaskEditorPage(
    task = task,
    modifier = modifier
        .sharedBounds(
            sharedContentState = rememberSharedContentState(key = "${Constants.KEY_TODO_ITEM_TRANSITION}_${task.id}"),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
        )
        .skipToLookaheadSize(),
    onNavigateUp = onNavigateUp
)

@Composable
fun TaskEditorPage(
    modifier: Modifier = Modifier,
    task: TaskEntity? = null,
    onNavigateUp: () -> Unit = {},
    onSaved: () -> Unit = onNavigateUp,
    quickAdd: Boolean = false,
    initialCategory: String = "",
    initialDueDateMillis: Long? = null,
    viewModel: EditorViewModel = koinViewModel { parametersOf(task) }
) {
    val view = LocalView.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val contentField = rememberTextFieldState(initialText = task?.content ?: "")

    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.error_task_save)
    val saveLabel = stringResource(R.string.action_save)
    var showTimePicker by remember { mutableStateOf(false) }
    var validate by remember { mutableStateOf(false) } // @ChatGPT，用于判断用户是否按下了保存按钮。按下了开始进行错误检测
    val isContentError by remember { derivedStateOf { validate && uiState.content.isBlank() } }

    fun navigateUpIfUnchanged() {
        if (uiState.isSaving) return
        if (viewModel.isModified()) {
            viewModel.showExitConfirmDialog()
        } else {
            onNavigateUp()
        }
    }

    LaunchedEffect(quickAdd) {
        if (quickAdd) viewModel.setCreationDefaults(initialCategory, initialDueDateMillis)
    }

    LaunchedEffect(quickAdd || uiState.shouldAutoFocusContent) {
        if (quickAdd || uiState.shouldAutoFocusContent) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            if (task != null && !task.isCompleted && uiState.isCompleted) viewModel.setConfettiVisibility(true)
            onSaved()
        }
    }
    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) snackbarHostState.showSnackbar(saveError)
    }

    LaunchedEffect(contentField) {
        snapshotFlow { contentField.text.trim().toString() }
            .collect { viewModel.setContentText(it) }
    }

    BackHandler(onBack = ::navigateUpIfUnchanged)

    TopAppBarScaffold(
        title = stringResource(if (task == null) R.string.action_add_task else R.string.title_edit_task),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        navigationIcon = {
            FilledIconButton(
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    VibrationUtils.performHapticFeedback(view)
                    navigateUpIfUnchanged()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VerveDoDefaults.contentPadding),
                modifier = Modifier.imePadding()
            ) {
                if (task != null) {
                    TodoFloatingActionButton(
                        text = stringResource(R.string.action_delete),
                        iconRes = R.drawable.ic_delete,
                        expanded = true,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = viewModel::showDeleteConfirmDialog
                    )
                }
                TodoFloatingActionButton(
                    text = if (uiState.isSaving) stringResource(R.string.action_saving) else saveLabel,
                    iconRes = R.drawable.ic_save,
                    expanded = true,
                    modifier = Modifier.semantics { contentDescription = saveLabel },
                    onClick = {
                        validate = true

                        if (uiState.content.isBlank()) {
                            return@TodoFloatingActionButton
                        }

                        viewModel.saveNewTask()
                    }
                )
            }
        },
        modifier = modifier
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(VerveDoDefaults.contentPadding * 2),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = VerveDoDefaults.screenVerticalPadding)
        ) {
            item {
                Subtitle(R.string.label_basic_information)
                TextField(
                    state = contentField,
                    label = { Text(stringResource(R.string.placeholder_add_todo)) },
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 3),
                    isError = isContentError,
                    supportingText = {
                        AnimatedVisibility(
                            visible = isContentError,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = stringResource(R.string.error_no_content_entered),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.details,
                    onValueChange = viewModel::setDetailsText,
                    label = { Text(stringResource(R.string.task_details)) },
                    minLines = 2,
                    maxLines = 6,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Subtitle(R.string.task_subtasks)
                if (uiState.subtasks.isEmpty()) Text(stringResource(R.string.task_subtasks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            itemsIndexed(uiState.subtasks, key = { _, item -> item.id }) { index, item ->
                val subtaskFocus = remember { FocusRequester() }
                LaunchedEffect(item.id) { if (item.content.isEmpty()) subtaskFocus.requestFocus() }
                OutlinedTextField(
                    value = item.content,
                    onValueChange = { viewModel.updateSubtask(item.id, it) },
                    label = { Text(stringResource(R.string.task_subtask_label, index + 1)) },
                    leadingIcon = {
                        val checkLabel = stringResource(R.string.task_subtask_check, index + 1)
                        Checkbox(checked = item.isCompleted,
                            onCheckedChange = { viewModel.setSubtaskCompleted(item.id, it) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.semantics { contentDescription = checkLabel })
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.removeSubtask(item.id) }, enabled = !uiState.isSaving) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.task_subtask_remove, index + 1))
                        }
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().focusRequester(subtaskFocus)
                )
            }
            item {
                TextButton(onClick = viewModel::addSubtask, enabled = !uiState.isSaving) {
                    Icon(painterResource(R.drawable.ic_add), null)
                    Text(stringResource(R.string.task_subtask_add), Modifier.padding(start = 8.dp))
                }
            }
            item {
                val colors = assignTagColors(uiState.tags, uiState.tagColors)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.tags.forEach { tag ->
                        TagChip(tag, colors.getValue(tag), { viewModel.removeTag(tag) }, !uiState.isSaving)
                    }
                }
                TagTextField(
                    value = uiState.category,
                    onValueChange = viewModel::setCategoryText,
                    tags = uiState.categoryList - uiState.tags.toSet(),
                    label = stringResource(R.string.tag_optional),
                    enabled = !uiState.isSaving,
                    onDone = { viewModel.addTag() },
                    onTagSelected = { viewModel.addTag(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { viewModel.addTag() },
                    enabled = uiState.category.isNotBlank() && !uiState.isSaving) {
                    Text(stringResource(R.string.tag_add))
                }
            }
            item {
                val priorityList = Priority.entries

                Subtitle(R.string.label_priority)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalArrangement = Arrangement.spacedBy(VerveDoDefaults.contentPadding / 4)
                ) {
                    priorityList.forEachIndexed { index, priority ->
                        ToggleButton(
                            content = {
                                PriorityIcon(priority, tint = LocalContentColor.current, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(priority.nameRes))
                            },
                            checked = uiState.priority == priority,
                            onCheckedChange = {
                                viewModel.setPriority(priority)
                                VibrationUtils.performHapticFeedback(view)
                            },
                            shapes = index toggleButtonShapesIn priorityList,
                            colors = VerveDoDefaults.toggleButtonColors,
                            modifier = Modifier.semantics { role = Role.RadioButton }
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.label_more),
                    style = MaterialTheme.typography.titleMedium
                )
                DueDateChooser(
                    dateMillis = uiState.dueDateMillis,
                    onDateChange = { viewModel.setDueDate(it) }
                )
                if (uiState.dueDateMillis != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showTimePicker = true }) {
                            Text(uiState.dueTimeMinutes?.let { stringResource(R.string.due_time_value, formatDueTime(it)) }
                                ?: stringResource(R.string.add_due_time))
                        }
                        if (uiState.dueTimeMinutes != null) TextButton(onClick = { viewModel.setDueTime(null) }) {
                            Text(stringResource(R.string.remove_due_time))
                        }
                    }
                }
                if (task != null) {
                    CheckboxWithLabel(
                        label = stringResource(R.string.tip_mark_completed),
                        checked = uiState.isCompleted,
                        onCheckedChange = { viewModel.setCompleted(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Spacer(Modifier.size(56.dp))
            }
            item {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
            }
        }
    }

    if (showTimePicker) DueTimeDialog(
        initialMinutes = uiState.dueTimeMinutes ?: uiState.defaultDueTimeMinutes,
        onConfirm = { viewModel.setDueTime(it); showTimePicker = false },
        onDismiss = { showTimePicker = false }
    )

    ConfirmDialog(
        visible = uiState.showExitConfirmDialog,
        iconRes = R.drawable.ic_undo,
        text = stringResource(R.string.tip_discard_changes),
        onConfirm = {
            viewModel.hideExitConfirmDialog()
            onNavigateUp()
        },
        onDismiss = { viewModel.hideExitConfirmDialog() }
    )

    ConfirmDialog(
        visible = uiState.showDeleteConfirmDialog,
        iconRes = R.drawable.ic_delete,
        text = stringResource(R.string.tip_delete_task, 1),
        onConfirm = {
            viewModel.deleteTask()
            onNavigateUp()
        },
        onDismiss = viewModel::hideDeleteConfirmDialog
    )
}

@Composable
private fun LazyItemScope.Subtitle(@StringRes titleRes: Int) =
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium
    )
