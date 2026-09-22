package cn.super12138.todo.logic.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cn.super12138.todo.constants.Constants
import cn.super12138.todo.logic.DEFAULT_DUE_TIME_MINUTES
import cn.super12138.todo.logic.assignTagColors
import cn.super12138.todo.logic.model.OverviewLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class DataStoreManager(val dataStore: DataStore<Preferences>) {
    // Keys
    // 外观与个性化
    private val DYNAMIC_COLOR = booleanPreferencesKey(Constants.PREF_DYNAMIC_COLOR)
    private val PALETTE_STYLE = intPreferencesKey(Constants.PREF_PALETTE_STYLE)
    private val DARK_MODE = intPreferencesKey(Constants.PREF_DARK_MODE)
    private val PURE_BLACK_MODE = booleanPreferencesKey(Constants.PREF_PURE_BLACK_MODE)
    private val CONTRAST_LEVEL = floatPreferencesKey(Constants.PREF_CONTRAST_LEVEL)
    private val PREVIEW_COLOR_SYSTEM = booleanPreferencesKey(Constants.PREF_PREVIEW_COLOR_SYSTEM)

    // 界面与交互
    // private val SHOW_COMPLETED = booleanPreferencesKey(Constants.PREF_SHOW_COMPLETED)
    private val SORTING_METHOD = intPreferencesKey(Constants.PREF_SORTING_METHOD)
    private val TEXT_FIELD_AUTO_FOCUS = booleanPreferencesKey(Constants.PREF_TEXT_FIELD_AUTO_FOCUS)
    private val SECURE_MODE = booleanPreferencesKey(Constants.PREF_SECURE_MODE)
    private val HAPTIC_FEEDBACK = booleanPreferencesKey(Constants.PREF_HAPTIC_FEEDBACK)

    // 数据
    private val CATEGORIES = stringPreferencesKey(Constants.PREF_CATEGORIES)
    private val TAG_COLORS = stringPreferencesKey("tag_colors")
    private val DEFAULT_DUE_TIME = intPreferencesKey("default_due_time_minutes")
    private val OVERVIEW_LAYOUT = stringPreferencesKey("overview_layout_v1")

    val overviewLayoutFlow = dataStore.data.map { OverviewLayout.decode(it[OVERVIEW_LAYOUT]) }

    suspend fun setOverviewLayout(layout: OverviewLayout) {
        dataStore.edit { it[OVERVIEW_LAYOUT] = layout.normalized().encode() }
    }

    val tagColorsFlow = dataStore.data.map { preferences ->
        Json.decodeFromString<Map<String, Int>>(preferences[TAG_COLORS] ?: "{}")
    }
    val defaultDueTimeFlow = dataStore.data.map { preferences ->
        (preferences[DEFAULT_DUE_TIME] ?: DEFAULT_DUE_TIME_MINUTES).coerceIn(0, 1439)
    }

    suspend fun ensureTagColors(tags: List<String>) {
        dataStore.edit { preferences ->
            val existing = Json.decodeFromString<Map<String, Int>>(preferences[TAG_COLORS] ?: "{}")
            val colors = assignTagColors(tags, existing)
            if (colors != existing) preferences[TAG_COLORS] = Json.encodeToString(colors)
        }
    }

    suspend fun setTagColor(tag: String, color: Int) {
        dataStore.edit { preferences ->
            val existing = Json.decodeFromString<Map<String, Int>>(preferences[TAG_COLORS] ?: "{}")
            preferences[TAG_COLORS] = Json.encodeToString(existing + (tag to color))
        }
    }

    suspend fun updateTag(old: String?, replacement: String?, color: Int?) {
        dataStore.edit { preferences ->
            val presets = Json.decodeFromString<List<String>>(preferences[CATEGORIES] ?: Constants.PREF_CATEGORIES_DEFAULT)
            val colors = Json.decodeFromString<Map<String, Int>>(preferences[TAG_COLORS] ?: "{}")
            val updated = if (old == null) presets else cn.super12138.todo.logic.replaceTag(presets, old, replacement)
            preferences[CATEGORIES] = Json.encodeToString((updated + listOfNotNull(replacement)).distinct())
            val newColors = if (old == null) colors else colors - old
            preferences[TAG_COLORS] = Json.encodeToString(
                if (replacement != null && color != null) newColors + (replacement to color) else newColors
            )
        }
    }

    suspend fun setDefaultDueTime(minutes: Int) {
        require(minutes in 0..1439)
        dataStore.edit { it[DEFAULT_DUE_TIME] = minutes }
    }

    // Getters
    val dynamicColorFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR] ?: Constants.PREF_DYNAMIC_COLOR_DEFAULT
    }

    val paletteStyleFlow = dataStore.data.map { preferences ->
        preferences[PALETTE_STYLE] ?: Constants.PREF_PALETTE_STYLE_DEFAULT
    }

    val darkModeFlow = dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: Constants.PREF_DARK_MODE_DEFAULT
    }

    val pureBlackFlow = dataStore.data.map { preferences ->
        preferences[PURE_BLACK_MODE] ?: Constants.PREF_PURE_BLACK_MODE_DEFAULT
    }

    val contrastLevelFlow = dataStore.data.map { preferences ->
        preferences[CONTRAST_LEVEL] ?: Constants.PREF_CONTRAST_LEVEL_DEFAULT
    }

    val previewColorSystemFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PREVIEW_COLOR_SYSTEM] ?: Constants.PREF_PREVIEW_COLOR_SYSTEM_DEFAULT
    }

    /*val showCompletedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHOW_COMPLETED] ?: Constants.PREF_SHOW_COMPLETED_DEFAULT
    }*/

    val sortingMethodFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SORTING_METHOD] ?: Constants.PREF_SORTING_METHOD_DEFAULT
    }

    val textFieldAutoFocusFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[TEXT_FIELD_AUTO_FOCUS] ?: Constants.PREF_TEXT_FIELD_AUTO_FOCUS_DEFAULT
    }

    val secureModeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SECURE_MODE] ?: Constants.PREF_SECURE_MODE_DEFAULT
    }

    val hapticFeedbackFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK] ?: Constants.PREF_HAPTIC_FEEDBACK_DEFAULT
    }

    val categoriesFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        Json.decodeFromString(preferences[CATEGORIES] ?: Constants.PREF_CATEGORIES_DEFAULT)
    }

    // Setters
    suspend fun setDynamicColor(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = value
        }
    }

    suspend fun setPaletteStyle(value: Int) {
        dataStore.edit { preferences ->
            preferences[PALETTE_STYLE] = value
        }
    }

    suspend fun setDarkMode(value: Int) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE] = value
        }
    }

    suspend fun setPureBlackMode(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PURE_BLACK_MODE] = value
        }
    }

    suspend fun setContrastLevel(value: Float) {
        dataStore.edit { preferences ->
            preferences[CONTRAST_LEVEL] = value
        }
    }

    suspend fun setPreviewColorSystem(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREVIEW_COLOR_SYSTEM] = value
        }
    }

    /*suspend fun setShowCompleted(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_COMPLETED] = value
        }
    }*/

    suspend fun setSortingMethod(value: Int) {
        dataStore.edit { preferences ->
            preferences[SORTING_METHOD] = value
        }
    }

    suspend fun setTextFieldAutoFocus(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[TEXT_FIELD_AUTO_FOCUS] = value
        }
    }

    suspend fun setSecureMode(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[SECURE_MODE] = value
        }
    }

    suspend fun setHapticFeedback(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK] = value
        }
    }

    suspend fun setCategories(value: List<String>) {
        dataStore.edit { preferences ->
            preferences[CATEGORIES] = Json.encodeToString(value)
        }
    }
}
