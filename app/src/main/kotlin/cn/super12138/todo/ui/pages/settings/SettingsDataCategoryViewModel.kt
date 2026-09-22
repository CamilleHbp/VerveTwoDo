package cn.super12138.todo.ui.pages.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.super12138.todo.logic.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsDataCategoryViewModel(
    private val tagRepository: TagRepository,
    application: android.app.Application,
    private val refreshWidgets: suspend (String?, String?) -> Unit = { old, replacement ->
        cn.super12138.todo.utils.updateTagWidgets(application, old, replacement)
    }
) : ViewModel() {
    private val localUiState = MutableStateFlow(SettingsDataCategoryUiState())
    val uiState: StateFlow<SettingsDataCategoryUiState> = combine(
        tagRepository.tags, tagRepository.colors, tagRepository.usageCounts, localUiState
    ) { tags, colors, counts, local ->
        local.copy(categories = tags, tagColors = colors, usageCounts = counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataCategoryUiState())

    fun clearError() = localUiState.update { it.copy(error = null) }

    fun saveTag(old: String?, name: String, color: Int, onSaved: () -> Unit) =
        mutate(old, onSaved) { tagRepository.save(old, name, color) }

    fun deleteTag(tag: String, onDeleted: () -> Unit) =
        mutate(tag, onDeleted) { tagRepository.delete(tag); null }

    private fun mutate(old: String?, onSuccess: () -> Unit, operation: suspend () -> String?) {
        if (localUiState.value.isSaving) return
        localUiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val replacement = operation()
                localUiState.update { it.copy(isSaving = false) }
                onSuccess()
                try {
                    refreshWidgets(old, replacement)
                } catch (exception: kotlinx.coroutines.CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    android.util.Log.w("Tags", "Could not refresh tag widgets", exception)
                }
            } catch (exception: kotlinx.coroutines.CancellationException) {
                throw exception
            } catch (_: cn.super12138.todo.logic.DuplicateTagException) {
                localUiState.update { it.copy(isSaving = false, error = cn.super12138.todo.R.string.tag_name_exists) }
            } catch (_: Exception) {
                localUiState.update { it.copy(isSaving = false, error = cn.super12138.todo.R.string.tag_change_failed) }
            }
        }
    }
}
