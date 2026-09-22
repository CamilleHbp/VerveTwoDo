package cn.super12138.todo.ui.pages.editor

import cn.super12138.todo.logic.model.Priority

data class TaskEditorUiState(
    val content: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val details: String = "",
    val subtasks: List<cn.super12138.todo.logic.database.Subtask> = emptyList(),
    val tagColors: Map<String, Int> = emptyMap(),
    val defaultDueTimeMinutes: Int = cn.super12138.todo.logic.DEFAULT_DUE_TIME_MINUTES,
    val dueTimeMinutes: Int? = null,
    val priority: Priority = Priority.Default,
    val dueDateMillis: Long? = null,
    val isCompleted: Boolean = false,
    val categoryList: List<String> = emptyList(),
    val shouldAutoFocusContent: Boolean = false,
    val showExitConfirmDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false
)
