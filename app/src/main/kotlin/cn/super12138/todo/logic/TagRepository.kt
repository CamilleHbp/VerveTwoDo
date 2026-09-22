package cn.super12138.todo.logic

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import cn.super12138.todo.logic.database.taskTags

data class TagChange(val old: String, val replacement: String?)
class DuplicateTagException : IllegalArgumentException()

internal fun replaceTag(tags: List<String>, old: String, replacement: String?): List<String> =
    tags.mapNotNull { if (it == old) replacement else it }.distinct()

/** Owns global tag identities across presets, tasks, and editor drafts. */
class TagRepository(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository
) {
    private val mutations = Mutex()
    private val _changes = MutableSharedFlow<TagChange>(extraBufferCapacity = 16)
    val changes = _changes.asSharedFlow()

    val tags = combine(settingsRepository.categoriesFlow, taskRepository.getCategories()) { presets, used ->
        tagCatalog(presets, used)
    }.distinctUntilChanged()

    val usageCounts = taskRepository.getAllTasks().map { tasks ->
        tasks.flatMap { it.taskTags }.groupingBy { it }.eachCount()
    }

    val colors = combine(tags, settingsRepository.tagColorsFlow) { tags, colors ->
        assignTagColors(tags, colors)
    }.distinctUntilChanged().onEach {
        mutations.withLock { settingsRepository.ensureTagColors(tags.first()) }
    }

    suspend fun save(old: String?, input: String, color: Int): String = mutations.withLock {
        val name = if (input == old) input else input.trim()
        require(name.isNotBlank())
        if (name != old && tags.first().any { it != old && it.trim().equals(name, ignoreCase = true) }) {
            throw DuplicateTagException()
        }
        if (old != null && old != name) taskRepository.replaceTag(old, name)
        settingsRepository.updateTag(old, name, color)
        if (old != null && old != name) _changes.emit(TagChange(old, name))
        name
    }

    suspend fun delete(tag: String) = mutations.withLock {
        taskRepository.replaceTag(tag, null)
        settingsRepository.updateTag(tag, null, null)
        _changes.emit(TagChange(tag, null))
    }
}

internal fun tagCatalog(presets: List<String>, used: List<String>): List<String> =
    (presets + used).filter { it.isNotBlank() }.distinct()
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.trim() })

/** Prefer an exact existing identity, then reuse its spelling instead of creating a case variant. */
internal fun resolveTag(input: String, existing: List<String>): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    return existing.firstOrNull { it == input }
        ?: existing.firstOrNull { it == trimmed }
        ?: existing.firstOrNull { it.trim().equals(trimmed, ignoreCase = true) }
        ?: trimmed
}

internal fun matchingTags(input: String, existing: List<String>): List<String> {
    val query = input.trim()
    return existing.filter { it.contains(query, ignoreCase = true) }
        .sortedBy { !it.trim().startsWith(query, ignoreCase = true) }
}
