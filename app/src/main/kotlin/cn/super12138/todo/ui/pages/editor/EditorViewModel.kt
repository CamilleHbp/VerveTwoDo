package cn.super12138.todo.ui.pages.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.super12138.todo.logic.SettingsRepository
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.resolveTag
import cn.super12138.todo.logic.dueTimestamp
import cn.super12138.todo.logic.database.taskTags
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.model.Priority
import cn.super12138.todo.utils.ConfettiController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class EditorViewModel(
    val initialTask: TaskEntity?,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val tagRepository: TagRepository,
    private val confettiController: ConfettiController
) : ViewModel() {
    private var initialState = TaskEditorUiState(
        content = initialTask?.content.orEmpty(),
        details = initialTask?.details.orEmpty(),
        subtasks = initialTask?.subtasks.orEmpty(),
        tags = initialTask?.taskTags.orEmpty(),
        priority = Priority.fromFloat(initialTask?.priority ?: 0f),
        dueDateMillis = initialTask?.dueDateMillis,
        dueTimeMinutes = initialTask?.dueTimeMinutes,
        isCompleted = initialTask?.isCompleted ?: false
    )
    private val localUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<TaskEditorUiState> = combine(
        settingsRepository.textFieldAutoFocusFlow, tagRepository.tags, tagRepository.colors,
        settingsRepository.defaultDueTimeFlow, localUiState
    ) { autoFocus, tags, colors, defaultTime, local ->
        local.copy(shouldAutoFocusContent = autoFocus, categoryList = tags,
            tagColors = colors, defaultDueTimeMinutes = defaultTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialState)

    private var creationDefaultsApplied = false
    private var creationState = initialState

    init {
        viewModelScope.launch {
            tagRepository.changes.collect { change ->
                fun TaskEditorUiState.updatedTags() = copy(
                    tags = cn.super12138.todo.logic.replaceTag(tags, change.old, change.replacement),
                    category = if (category.trim() == change.old) change.replacement.orEmpty() else category
                )
                initialState = initialState.updatedTags()
                creationState = creationState.updatedTags()
                localUiState.update { it.updatedTags() }
            }
        }
    }

    fun setCreationDefaults(category: String, dueDateMillis: Long?) {
        if (initialTask != null || creationDefaultsApplied) return
        creationDefaultsApplied = true
        viewModelScope.launch {
            val minutes = if (dueDateMillis != null) settingsRepository.defaultDueTimeFlow.first() else null
            val state = localUiState.value.copy(tags = listOf(category).filter { it.isNotBlank() },
                dueDateMillis = dueDateMillis?.let { dueTimestamp(it, minutes) }, dueTimeMinutes = minutes)
            creationState = initialState.copy(tags = state.tags, dueDateMillis = state.dueDateMillis,
                dueTimeMinutes = state.dueTimeMinutes)
            localUiState.value = state
        }
    }

    fun setContentText(content: String) = localUiState.update { it.copy(content = content) }
    fun setDetailsText(details: String) = localUiState.update { it.copy(details = details) }
    fun addSubtask() = localUiState.update {
        it.copy(subtasks = it.subtasks + cn.super12138.todo.logic.database.Subtask(content = ""))
    }
    fun updateSubtask(id: String, content: String) = localUiState.update { state ->
        state.copy(subtasks = state.subtasks.map { if (it.id == id) it.copy(content = content) else it })
    }
    fun setSubtaskCompleted(id: String, completed: Boolean) = localUiState.update { state ->
        state.copy(subtasks = state.subtasks.map { if (it.id == id) it.copy(isCompleted = completed) else it })
    }
    fun removeSubtask(id: String) = localUiState.update { state ->
        state.copy(subtasks = state.subtasks.filterNot { it.id == id })
    }
    fun setCategoryText(category: String) = localUiState.update { it.copy(category = category) }
    fun addTag(input: String = localUiState.value.category) {
        val tag = resolveTag(input, uiState.value.categoryList + localUiState.value.tags)
        if (tag.isBlank()) return
        localUiState.update { it.copy(tags = (it.tags + tag).distinct(), category = "") }
    }
    fun removeTag(tag: String) = localUiState.update { it.copy(tags = it.tags - tag) }
    fun setPriority(priority: Priority) = localUiState.update { it.copy(priority = priority) }
    fun setDueDate(date: Long?) = localUiState.update {
        val time = if (date == null) null else if (it.dueDateMillis == null) uiState.value.defaultDueTimeMinutes else it.dueTimeMinutes
        it.copy(dueDateMillis = date?.let { day -> dueTimestamp(day, time) }, dueTimeMinutes = time)
    }
    fun setDueTime(minutes: Int?) = localUiState.update {
        require(minutes == null || minutes in 0..1439)
        it.copy(dueTimeMinutes = if (it.dueDateMillis == null) null else minutes,
            dueDateMillis = it.dueDateMillis?.let { date -> dueTimestamp(date, minutes) })
    }
    fun setCompleted(completed: Boolean) = localUiState.update { it.copy(isCompleted = completed) }

    fun isModified(): Boolean {
        val state = localUiState.value
        val initial = if (initialTask == null) creationState else initialState
        return state.content.trim() != initial.content || state.details != initial.details ||
            state.tags != initial.tags || state.category.isNotBlank() || state.priority != initial.priority ||
            state.isCompleted != initial.isCompleted || state.dueDateMillis != initial.dueDateMillis ||
            state.dueTimeMinutes != initial.dueTimeMinutes || state.subtasks != initial.subtasks
    }

    fun showDeleteConfirmDialog() = localUiState.update { it.copy(showDeleteConfirmDialog = true) }
    fun showExitConfirmDialog() = localUiState.update { it.copy(showExitConfirmDialog = true) }
    fun hideDeleteConfirmDialog() = localUiState.update { it.copy(showDeleteConfirmDialog = false) }
    fun hideExitConfirmDialog() = localUiState.update { it.copy(showExitConfirmDialog = false) }

    fun saveNewTask() {
        if (localUiState.value.isSaving || localUiState.value.isSaved || localUiState.value.content.isBlank()) return
        if (!isModified()) {
            localUiState.update { it.copy(isSaved = true) }
            return
        }
        val state = localUiState.value
        localUiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val catalog = tagRepository.tags.first()
                val tags = (state.tags + state.category).map { resolveTag(it, catalog + state.tags) }
                    .filter { it.isNotBlank() }.distinct()
                settingsRepository.ensureTagColors(catalog + tags)
                taskRepository.insertTask(TaskEntity(
                    content = state.content.trim(), details = state.details, tags = tags,
                    subtasks = state.subtasks.filter { it.content.isNotBlank() }.map { it.copy(content = it.content.trim()) },
                    category = tags.firstOrNull().orEmpty(), isCompleted = state.isCompleted,
                    priority = state.priority.value, dueDateMillis = state.dueDateMillis,
                    dueTimeMinutes = state.dueTimeMinutes, id = initialTask?.id ?: 0,
                    createdAtMillis = initialTask?.createdAtMillis
                ))
                localUiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                localUiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    fun deleteTask() {
        if (initialTask == null) return
        viewModelScope.launch { taskRepository.deleteTask(initialTask) }
    }

    fun setConfettiVisibility(visible: Boolean) = confettiController.setVisibility(visible)
}
