package cn.super12138.todo.ui.pages.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.resolveTag
import cn.super12138.todo.logic.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsDataCategoryViewModel(
    private val settingsRepository: SettingsRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    private val localUiState = MutableStateFlow(SettingsDataCategoryUiState())
    val uiState: StateFlow<SettingsDataCategoryUiState> = combine(
        settingsRepository.categoriesFlow,
        tagRepository.tags,
        tagRepository.colors,
        localUiState
    ) { categories, tags, colors, localState ->
        localState.copy(categories = tags, presetCategories = categories, suggestedTags = tags, tagColors = colors)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsDataCategoryUiState()
    )

    fun setEditingCategory(value: String) = localUiState.update { it.copy(editingCategory = value) }

    fun addCategory(new: String, color: Int? = null) {
        if (new.isBlank()) return
        val old = localUiState.value.editingCategory
        viewModelScope.launch {
            val presets = settingsRepository.categoriesFlow.first()
            val category = resolveTag(new, tagRepository.tags.first())
            val list = when {
                old == category -> presets
                category in presets -> presets.filterNot { it == old }
                old in presets -> presets.map { if (it == old) category else it }
                else -> presets + category
            }
            settingsRepository.setCategories(list)
            settingsRepository.ensureTagColors(tagRepository.tags.first() + category)
            if (color != null) settingsRepository.setTagColor(category, color)
        }
    }

    fun removeCategory(category: String) {
        viewModelScope.launch {
            settingsRepository.setCategories(settingsRepository.categoriesFlow.first() - category)
        }
    }

    fun showAddDialog() = localUiState.update { it.copy(showAddDialog = true) }
    fun hideAddDialog() = localUiState.update { it.copy(showAddDialog = false) }
}
