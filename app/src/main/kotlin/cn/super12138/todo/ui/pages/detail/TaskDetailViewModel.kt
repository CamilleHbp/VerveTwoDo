package cn.super12138.todo.ui.pages.detail

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.utils.updateTaskWidgets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: TaskEntity? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val tagColors: Map<String, Int> = emptyMap()
)

class TaskDetailViewModel(
    private val taskId: Int,
    private val repository: TaskRepository,
    private val tags: TagRepository,
    private val application: Application
) : ViewModel() {
    private val state = MutableStateFlow(TaskDetailUiState())
    val uiState = state.asStateFlow()
    private val errors = Channel<Unit>(Channel.BUFFERED)
    val saveErrors = errors.receiveAsFlow()
    private var observation: Job? = null

    init { reload() }

    fun reload() {
        observation?.cancel()
        state.update { it.copy(isLoading = true, loadFailed = false) }
        observation = viewModelScope.launch {
            try {
                combine(repository.observeTask(taskId), tags.colors) { task, colors -> task to colors }
                    .collect { (task, colors) ->
                    state.update { it.copy(task = task, tagColors = colors, isLoading = false, loadFailed = false) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                state.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun setCompleted(completed: Boolean) = save {
        if (completed) repository.completeTask(taskId) else repository.restoreTask(taskId)
    }

    fun setSubtaskCompleted(id: String, completed: Boolean) = save {
        repository.setSubtaskCompleted(taskId, id, completed)
    }

    private fun save(write: suspend () -> Unit) {
        if (state.value.isSaving) return
        state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                write()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                errors.send(Unit)
            } finally {
                state.update { it.copy(isSaving = false) }
            }
            // A launcher refresh failure must not report a successfully saved task as unsaved.
            try { updateTaskWidgets(application) }
            catch (exception: CancellationException) { throw exception }
            catch (_: Exception) { /* Widgets also refresh when the app leaves the foreground. */ }
        }
    }
}
