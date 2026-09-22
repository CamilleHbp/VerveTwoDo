package cn.super12138.todo.ui.pages.overview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cn.super12138.todo.R
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.model.OverviewCard
import cn.super12138.todo.logic.model.OverviewCardSize
import cn.super12138.todo.logic.model.OverviewLayout
import cn.super12138.todo.ui.VerveDoDefaults
import cn.super12138.todo.ui.components.TopAppBarScaffold
import cn.super12138.todo.ui.pages.overview.components.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OverviewPage(
    toTaskAddPage: () -> Unit,
    toTaskEditPage: (TaskEntity) -> Unit,
    toTaskViewPage: (TaskEntity) -> Unit = toTaskEditPage,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources by rememberUpdatedState(LocalResources.current)
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    var filter by rememberSaveable { mutableStateOf<OverviewCard?>(null) }
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
    var discardDialog by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val editing = draft != null
    val editLayout = {
        snackbar.currentSnackbarData?.dismiss()
        draft = uiState.layout.encode()
    }
    val leave: () -> Unit = {
        if (!uiState.isSavingLayout) {
            if (editing && draft != uiState.layout.encode()) discardDialog = true
            else if (editing) draft = null
            else filter = null
        }
    }

    BackHandler(enabled = editing || filter != null, onBack = leave)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is OverviewEvent.CompletionChanged -> {
                        val message = resources.getString(if (event.completed) R.string.widget_completed_task
                            else R.string.overview_restored_task, event.title)
                        val result = snackbar.showSnackbar(message,
                            actionLabel = resources.getString(R.string.widget_undo),
                            withDismissAction = true, duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.setCompleted(event.id, event.title, !event.completed, allowUndo = false)
                        }
                    }
                    is OverviewEvent.Error -> snackbar.showSnackbar(resources.getString(event.message))
                    OverviewEvent.LayoutSaved -> {
                        draft = null
                        snackbar.showSnackbar(resources.getString(R.string.overview_layout_saved))
                    }
                }
            }
        }
    }

    val toggle: (TaskEntity) -> Unit = {
        viewModel.setCompleted(it.id, it.content, !it.isCompleted)
    }
    TopAppBarScaffold(
        modifier = modifier,
        title = { Text(if (editing) stringResource(R.string.overview_edit)
            else filter?.let { overviewFilterTitle(it) } ?: stringResource(R.string.page_overview)) },
        navigationIcon = {
            if (editing || filter != null) {
                IconButton(onClick = leave, enabled = !uiState.isSavingLayout) {
                    Icon(painterResource(R.drawable.ic_arrow_back),
                        stringResource(R.string.overview_back))
                }
            }
        },
        actions = {
            if (editing) {
                TextButton(onClick = { viewModel.saveLayout(OverviewLayout.decode(draft)) },
                    enabled = !uiState.isSavingLayout) {
                    Text(stringResource(if (uiState.isSavingLayout) R.string.overview_saving else R.string.action_save))
                }
            } else if (filter == null) {
                TextButton(onClick = editLayout, enabled = !uiState.isLoading && !uiState.loadFailed) {
                    Text(stringResource(R.string.overview_edit))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!editing) {
                val addLabel = stringResource(R.string.action_add_task)
                ExtendedFloatingActionButton(onClick = toTaskAddPage,
                    modifier = Modifier.semantics { contentDescription = addLabel },
                    icon = { Icon(painterResource(R.drawable.ic_add), null) },
                    text = { Text(stringResource(R.string.action_add_task)) })
            }
        }
    ) {
        when {
            editing -> OverviewLayoutEditor(OverviewLayout.decode(draft),
                enabled = !uiState.isSavingLayout, onChange = { draft = it.encode() })
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.loadFailed -> Column(Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.overview_load_error))
                TextButton(onClick = viewModel::reload) { Text(stringResource(R.string.overview_retry)) }
            }
            filter != null -> {
                val selected = requireNotNull(filter)
                key(selected) {
                    OverviewFilteredTasks(selected, uiState, toTaskAddPage, toTaskViewPage, toTaskEditPage, toggle) {
                        filter = OverviewCard.All
                    }
                }
            }
            else -> OverviewDashboard(uiState, gridState, toTaskAddPage, toTaskViewPage, toTaskEditPage, toggle,
                onFilter = { filter = it }, onEdit = editLayout)
        }
    }
    if (discardDialog) {
        AlertDialog(onDismissRequest = { discardDialog = false },
            title = { Text(stringResource(R.string.overview_discard_title)) },
            text = { Text(stringResource(R.string.overview_discard_message)) },
            confirmButton = { TextButton(onClick = { draft = null; discardDialog = false }) {
                Text(stringResource(R.string.overview_discard))
            } },
            dismissButton = { TextButton(onClick = { discardDialog = false }) {
                Text(stringResource(R.string.overview_keep_editing))
            } })
    }
}

@Composable
private fun OverviewDashboard(
    state: OverviewUiState,
    gridState: LazyGridState,
    onAdd: () -> Unit,
    onOpen: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onToggle: (TaskEntity) -> Unit,
    onFilter: (OverviewCard) -> Unit,
    onEdit: () -> Unit
) {
    val groups = remember(state.tasks, state.today, state.zone) {
        OverviewCard.entries.associateWith(state::tasksFor)
    }
    val cards = state.layout.cards.filter { it.visible &&
        (it.card != OverviewCard.Overdue || groups.getValue(it.card).isNotEmpty()) }
    val largeText = LocalDensity.current.fontScale >= 1.3f
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        LazyVerticalGrid(columns = GridCells.Fixed(6), state = gridState,
            modifier = Modifier.fillMaxSize().testTag("overview-dashboard"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)) {
            if (cards.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val onlyOverdue = state.layout.cards.any { it.visible }
                        Text(stringResource(if (onlyOverdue) R.string.overview_empty_overdue else R.string.overview_empty_layout),
                            style = MaterialTheme.typography.titleLarge)
                        if (onlyOverdue) {
                            Button(onClick = { onFilter(OverviewCard.All) }) {
                                Text(stringResource(R.string.overview_show_all_tasks))
                            }
                        } else {
                            Text(stringResource(R.string.overview_empty_layout_help))
                            Button(onClick = onEdit) { Text(stringResource(R.string.overview_edit)) }
                        }
                    }
                }
            }
            items(cards, key = { it.card.name }, span = { config ->
                val summary = config.card in setOf(OverviewCard.All, OverviewCard.Pending, OverviewCard.Completed)
                GridItemSpan(when {
                    config.size == OverviewCardSize.Expanded -> maxLineSpan
                    summary -> if (largeText && !wide) 3 else 2
                    wide -> 3
                    else -> maxLineSpan
                })
            }) { config ->
                val tasks = groups.getValue(config.card)
                when (config.card) {
                    OverviewCard.All, OverviewCard.Pending, OverviewCard.Completed ->
                        OverviewStatusCard(overviewCardTitle(config.card), tasks.size,
                            onClick = { onFilter(config.card) }, modifier = Modifier.animateItem())
                    else -> OverviewTaskSection(config.card, tasks, config.size, state.tagColors,
                        state.busyTaskIds, onOpen, onToggle, onViewAll = { onFilter(config.card) },
                        onAddTask = onAdd, modifier = Modifier.animateItem(), onEditTask = onEditTask)
                }
            }
        }
    }
}

@Composable
private fun OverviewFilteredTasks(
    filter: OverviewCard,
    state: OverviewUiState,
    onAdd: () -> Unit,
    onOpen: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onToggle: (TaskEntity) -> Unit,
    onAll: () -> Unit
) {
    val tasks = remember(state.tasks, filter, state.today, state.zone) { state.tasksFor(filter) }
    Surface(shape = VerveDoDefaults.defaultShape, color = VerveDoDefaults.Colors.Container) {
        LazyColumn(Modifier.fillMaxSize().testTag("overview-filtered-list"),
            contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(pluralStringResource(R.plurals.overview_task_count, tasks.size, tasks.size),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (filter != OverviewCard.All) {
                        TextButton(onClick = onAll) { Text(stringResource(R.string.overview_show_all_tasks)) }
                    }
                }
            }
            if (tasks.isEmpty()) {
                item { OverviewEmptyState(overviewEmptyMessage(filter), onAdd, Modifier.padding(16.dp)) }
            }
            items(tasks, key = { it.id }) { task ->
                OverviewTaskRow(task, state.tagColors, task.id in state.busyTaskIds, onOpen, onToggle, onEdit = onEditTask)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
