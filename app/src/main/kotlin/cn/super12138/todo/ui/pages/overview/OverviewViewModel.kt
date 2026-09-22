package cn.super12138.todo.ui.pages.overview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.super12138.todo.R
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.datastore.DataStoreManager
import cn.super12138.todo.logic.model.OverviewLayout
import cn.super12138.todo.utils.updateTaskWidgets
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OverviewEvent {
    data class CompletionChanged(val id: Int, val title: String, val completed: Boolean) : OverviewEvent
    data class Error(val message: Int) : OverviewEvent
    data object LayoutSaved : OverviewEvent
}

private data class OverviewOperations(val busy: Set<Int> = emptySet(), val saving: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModel(
    private val taskRepository: TaskRepository,
    private val dataStoreManager: DataStoreManager,
    tagRepository: TagRepository,
    private val application: Application
) : ViewModel() {
    private val retry = MutableStateFlow(0)
    private val operations = MutableStateFlow(OverviewOperations())
    private val eventChannel = Channel<OverviewEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private val calendar = flow {
        while (true) {
            val zone = ZoneId.systemDefault()
            emit(LocalDate.now(zone) to zone)
            delay(30_000)
        }
    }.distinctUntilChanged()

    val uiState = retry.flatMapLatest {
        combine(taskRepository.getAllTasks(), dataStoreManager.overviewLayoutFlow,
            tagRepository.colors, calendar, operations) { tasks, layout, colors, day, action ->
            OverviewUiState(tasks, layout, day.first, day.second, colors,
                isLoading = false, busyTaskIds = action.busy, isSavingLayout = action.saving)
        }.catch { emit(OverviewUiState(isLoading = false, loadFailed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), OverviewUiState())

    fun reload() { retry.update { it + 1 } }

    fun setCompleted(id: Int, title: String, completed: Boolean, allowUndo: Boolean = true) {
        if (id in operations.value.busy) return
        operations.update { it.copy(busy = it.busy + id) }
        viewModelScope.launch {
            try {
                // Update only completion so editing, tags, and future sync cannot be overwritten.
                if (completed) taskRepository.completeTask(id) else taskRepository.restoreTask(id)
                if (allowUndo) eventChannel.send(OverviewEvent.CompletionChanged(id, title, completed))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                eventChannel.send(OverviewEvent.Error(R.string.overview_task_save_error))
            } finally {
                operations.update { it.copy(busy = it.busy - id) }
            }
            try {
                updateTaskWidgets(application)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Widget refresh also runs when the activity stops.
            }
        }
    }

    fun saveLayout(layout: OverviewLayout) {
        if (operations.value.saving) return
        operations.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                dataStoreManager.setOverviewLayout(layout)
                eventChannel.send(OverviewEvent.LayoutSaved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                eventChannel.send(OverviewEvent.Error(R.string.overview_layout_save_error))
            } finally {
                operations.update { it.copy(saving = false) }
            }
        }
    }
}
