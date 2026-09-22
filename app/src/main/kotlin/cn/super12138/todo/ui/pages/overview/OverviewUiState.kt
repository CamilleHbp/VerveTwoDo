package cn.super12138.todo.ui.pages.overview

import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.model.OverviewCard
import cn.super12138.todo.logic.model.OverviewLayout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class OverviewUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val layout: OverviewLayout = OverviewLayout(),
    val today: LocalDate = LocalDate.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val tagColors: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val busyTaskIds: Set<Int> = emptySet(),
    val isSavingLayout: Boolean = false
) {
    fun tasksFor(card: OverviewCard): List<TaskEntity> = overviewTasks(tasks, card, today, zone)
}

/** Calendar boundaries keep grouping correct across daylight-saving changes and timed tasks. */
fun overviewTasks(
    tasks: List<TaskEntity>,
    card: OverviewCard,
    today: LocalDate,
    zone: ZoneId
): List<TaskEntity> = tasks.filter { task ->
    val date = task.dueDateMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    when (card) {
        OverviewCard.All -> true
        OverviewCard.Pending -> !task.isCompleted
        OverviewCard.Completed -> task.isCompleted
        OverviewCard.Overdue -> !task.isCompleted && date != null && date < today
        OverviewCard.Today -> date == today
        OverviewCard.Upcoming -> !task.isCompleted && date != null && date > today && date <= today.plusDays(7)
    }
}.sortedWith(compareBy<TaskEntity> { it.isCompleted }
    .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
    .thenBy { it.dueTimeMinutes ?: Int.MAX_VALUE }
    .thenByDescending { it.priority }
    .thenBy { it.id })
