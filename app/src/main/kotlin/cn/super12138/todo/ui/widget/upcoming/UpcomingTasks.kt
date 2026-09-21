package cn.super12138.todo.ui.widget.upcoming

import cn.super12138.todo.logic.database.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class UpcomingTaskSort {
    DueDate, DueDateLatest, CreatedNewest, CreatedOldest, Priority, Alphabetical
}

enum class UpcomingSortField { DueDate, CreationDate, Priority, Alphabetical }

fun UpcomingTaskSort.field(): UpcomingSortField = when (this) {
    UpcomingTaskSort.DueDate, UpcomingTaskSort.DueDateLatest -> UpcomingSortField.DueDate
    UpcomingTaskSort.CreatedNewest, UpcomingTaskSort.CreatedOldest -> UpcomingSortField.CreationDate
    UpcomingTaskSort.Priority -> UpcomingSortField.Priority
    UpcomingTaskSort.Alphabetical -> UpcomingSortField.Alphabetical
}

fun UpcomingTaskSort.withField(field: UpcomingSortField): UpcomingTaskSort =
    if (field == this.field()) this else when (field) {
        UpcomingSortField.DueDate -> UpcomingTaskSort.DueDate
        UpcomingSortField.CreationDate -> UpcomingTaskSort.CreatedNewest
        UpcomingSortField.Priority -> UpcomingTaskSort.Priority
        UpcomingSortField.Alphabetical -> UpcomingTaskSort.Alphabetical
    }

fun UpcomingTaskSort.reverseDateOrder(): UpcomingTaskSort = when (this) {
    UpcomingTaskSort.DueDate -> UpcomingTaskSort.DueDateLatest
    UpcomingTaskSort.DueDateLatest -> UpcomingTaskSort.DueDate
    UpcomingTaskSort.CreatedNewest -> UpcomingTaskSort.CreatedOldest
    UpcomingTaskSort.CreatedOldest -> UpcomingTaskSort.CreatedNewest
    else -> this
}

data class UpcomingTaskGroup(
    val date: LocalDate? = null,
    val tasks: List<TaskEntity>,
    val isOverdue: Boolean = false
)

/** An empty set includes all tags; an empty string selects untagged tasks. */
fun upcomingTaskGroups(
    tasks: List<TaskEntity>,
    sort: UpcomingTaskSort,
    categories: Set<String> = emptySet(),
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): List<UpcomingTaskGroup> {
    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val dated = tasks.filter {
        it.dueDateMillis != null && (it.dueDateMillis >= start || !it.isCompleted) &&
            (categories.isEmpty() || it.category.ifBlank { "" } in categories)
    }
    val byDate = compareBy<TaskEntity> { it.dueDateMillis }
        .thenByDescending { it.priority }.thenBy { it.id }
    // Legacy tasks have no timestamp. Keep their insertion order before timestamped tasks.
    val byCreation = compareBy<TaskEntity> { it.createdAtMillis != null }
        .thenBy { it.createdAtMillis }.thenBy { it.id }
    val comparator = when (sort) {
        UpcomingTaskSort.DueDate -> byDate
        UpcomingTaskSort.DueDateLatest -> compareByDescending<TaskEntity> { it.dueDateMillis }
            .thenByDescending { it.priority }.thenBy { it.id }
        UpcomingTaskSort.CreatedNewest -> byCreation.reversed()
        UpcomingTaskSort.CreatedOldest -> byCreation
        UpcomingTaskSort.Priority -> compareByDescending<TaskEntity> { it.priority }.then(byDate)
        UpcomingTaskSort.Alphabetical -> compareBy<TaskEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.content }
            .then(byDate)
    }
    val (overdue, upcoming) = dated.sortedWith(comparator).partition {
        requireNotNull(it.dueDateMillis) < start
    }
    val groups = if (upcoming.isEmpty()) emptyList()
    else if (sort == UpcomingTaskSort.DueDate || sort == UpcomingTaskSort.DueDateLatest) {
        upcoming.groupBy { Instant.ofEpochMilli(requireNotNull(it.dueDateMillis)).atZone(zone).toLocalDate() }
            .map { (date, group) -> UpcomingTaskGroup(date, group) }
    } else listOf(UpcomingTaskGroup(tasks = upcoming))
    // Keep the date groups in place, then apply the chosen order within each completion state.
    val pendingFirst = compareBy<TaskEntity> { it.isCompleted }.then(comparator)
    return buildList {
        if (overdue.isNotEmpty()) add(UpcomingTaskGroup(tasks = overdue, isOverdue = true))
        addAll(groups.map { it.copy(tasks = it.tasks.sortedWith(pendingFirst)) })
    }
}
