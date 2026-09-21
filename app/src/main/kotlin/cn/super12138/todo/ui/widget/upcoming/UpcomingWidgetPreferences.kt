package cn.super12138.todo.ui.widget.upcoming

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import cn.super12138.todo.R

object UpcomingWidgetPreferences {
    val sortKey = stringPreferencesKey("upcoming_sort")
    val categoryKey = stringPreferencesKey("upcoming_category") // Legacy single-tag filter.
    val categoriesKey = stringSetPreferencesKey("upcoming_categories")
    val controlsKey = booleanPreferencesKey("upcoming_controls_visible")
    val panelKey = stringPreferencesKey("upcoming_panel")
    val undoIdKey = intPreferencesKey("upcoming_undo_id")
    val undoUntilKey = longPreferencesKey("upcoming_undo_until")
    val undoTitleKey = stringPreferencesKey("upcoming_undo_title")

    fun undoTaskId(preferences: Preferences, now: Long = System.currentTimeMillis()): Int? =
        preferences[undoIdKey]?.takeIf { now < (preferences[undoUntilKey] ?: 0L) }

    fun clearUndo(preferences: MutablePreferences) {
        preferences.remove(undoIdKey)
        preferences.remove(undoUntilKey)
        preferences.remove(undoTitleKey)
    }

    fun sort(preferences: Preferences): UpcomingTaskSort =
        UpcomingTaskSort.entries.find { it.name == preferences[sortKey] } ?: UpcomingTaskSort.DueDate

    fun categories(preferences: Preferences): Set<String> = preferences[categoriesKey]
        ?: preferences[categoryKey]?.let { setOf(it.ifBlank { "" }) } ?: emptySet()

    fun setCategories(preferences: MutablePreferences, categories: Set<String>) {
        preferences[categoriesKey] = categories
        preferences.remove(categoryKey)
    }
}

fun UpcomingTaskSort.labelRes(): Int = when (this) {
    UpcomingTaskSort.DueDate -> R.string.widget_due_soonest
    UpcomingTaskSort.DueDateLatest -> R.string.widget_due_latest
    UpcomingTaskSort.CreatedNewest -> R.string.widget_created_newest
    UpcomingTaskSort.CreatedOldest -> R.string.widget_created_oldest
    UpcomingTaskSort.Priority -> R.string.widget_priority_highest
    UpcomingTaskSort.Alphabetical -> R.string.widget_alphabetical
}
