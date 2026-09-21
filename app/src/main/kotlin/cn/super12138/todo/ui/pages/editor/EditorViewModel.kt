package cn.super12138.todo.ui.pages.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.super12138.todo.logic.SettingsRepository
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.resolveTag
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
    private val localUiState = MutableStateFlow(TaskEditorUiState())
    val uiState: StateFlow<TaskEditorUiState> = combine(
        settingsRepository.textFieldAutoFocusFlow,
        tagRepository.tags,
        localUiState
    ) { textFieldAutoFocus, categories, localState ->
        localState.copy(
            shouldAutoFocusContent = textFieldAutoFocus,
            categoryList = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskEditorUiState()
    )

    private var initialCategory = ""
    private var initialDueDate = initialTask?.dueDateMillis
    private var creationDefaultsApplied = false

    fun setCreationDefaults(category: String, dueDateMillis: Long?) {
        if (initialTask != null || creationDefaultsApplied) return
        creationDefaultsApplied = true
        initialCategory = category
        initialDueDate = dueDateMillis
        localUiState.update { it.copy(category = category, dueDateMillis = dueDateMillis) }
    }

    init {
        if (initialTask != null) {
            with(initialTask) {
                localUiState.update {
                    it.copy(
                        content = content,
                        category = category,
                        priority = Priority.fromFloat(priority),
                        dueDateMillis = dueDateMillis,
                        isCompleted = isCompleted
                    )
                }
            }
        }
    }

    fun setContentText(content: String) = localUiState.update { it.copy(content = content) }
    fun setCategoryText(category: String) = localUiState.update { it.copy(category = category) }
    fun setPriority(priority: Priority) = localUiState.update { it.copy(priority = priority) }
    fun setDueDate(dueDate: Long?) = localUiState.update { it.copy(dueDateMillis = dueDate) }
    fun setCompleted(completed: Boolean) = localUiState.update { it.copy(isCompleted = completed) }
    fun isModified(): Boolean {
        var isModified = false

        with(uiState.value) {
            if ((initialTask?.content ?: "") != content.trim()) isModified = true
            if ((initialTask?.category ?: initialCategory) != category.trim()) isModified = true
            if ((initialTask?.priority ?: 0f) != priority.value) isModified = true
            if ((initialTask?.isCompleted == true) != isCompleted) isModified = true
            if (initialDueDate != dueDateMillis) isModified = true
        }

        return isModified
    }


    fun showDeleteConfirmDialog() = localUiState.update { it.copy(showDeleteConfirmDialog = true) }
    fun showExitConfirmDialog() = localUiState.update { it.copy(showExitConfirmDialog = true) }
    fun hideDeleteConfirmDialog() = localUiState.update { it.copy(showDeleteConfirmDialog = false) }
    fun hideExitConfirmDialog() = localUiState.update { it.copy(showExitConfirmDialog = false) }
    fun saveNewTask() {
        if (localUiState.value.isSaving || localUiState.value.isSaved) return
        if (!isModified()) {
            localUiState.update { it.copy(isSaved = true) }
            return
        }
        val task = with(localUiState.value) {
            TaskEntity(
                content = content,
                category = category,
                isCompleted = isCompleted,
                priority = priority.value,
                dueDateMillis = dueDateMillis,
                id = initialTask?.id ?: 0,
                createdAtMillis = initialTask?.createdAtMillis
            )
        }
        localUiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val category = resolveTag(task.category, tagRepository.tags.first())
                taskRepository.insertTask(task.copy(category = category))
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
