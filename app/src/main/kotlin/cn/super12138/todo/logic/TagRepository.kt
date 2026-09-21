package cn.super12138.todo.logic

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

/** Names preserve existing category and widget-filter identities. */
class TagRepository(
    settingsRepository: SettingsRepository,
    taskRepository: TaskRepository
) {
    val tags = combine(settingsRepository.categoriesFlow, taskRepository.getCategories()) { presets, used ->
        tagCatalog(presets, used)
    }.distinctUntilChanged()

    val colors = combine(tags, settingsRepository.tagColorsFlow) { tags, colors ->
        assignTagColors(tags, colors)
    }.distinctUntilChanged().onEach { colors -> settingsRepository.ensureTagColors(colors.keys.toList()) }
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
