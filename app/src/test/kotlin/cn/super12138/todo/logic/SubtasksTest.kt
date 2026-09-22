package cn.super12138.todo.logic

import cn.super12138.todo.logic.database.Subtask
import cn.super12138.todo.logic.database.TaskConverters
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.ui.widget.upcoming.UpcomingTaskSort
import cn.super12138.todo.ui.widget.upcoming.upcomingTaskGroups
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SubtasksTest {
    @Test fun roundTripPreservesIdsOrderCompletionAndSpecialCharacters() {
        val items = listOf(Subtask("Quotes \" and commas,\nÉté", true), Subtask("同じ名前"), Subtask("同じ名前"))
        val converter = TaskConverters()
        assertEquals(items, converter.decodeSubtasks(converter.encodeSubtasks(items)))
        assertNotEquals(items[1].id, items[2].id)
        val task = TaskEntity("Parent", priority = 0f, subtasks = items)
        assertEquals(task, Json.decodeFromString<TaskEntity>(Json.encodeToString(task)))
        assertTrue(Json.decodeFromString<TaskEntity>("""{"content":"Legacy","priority":0.0}""").subtasks.isEmpty())
    }

    @Test fun widgetGroupsContainOnlyParentsRegardlessOfChecklistCompletion() {
        val parent = TaskEntity("Parent", priority = 0f, dueDateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            subtasks = listOf(Subtask("Hidden step"), Subtask("Done step", true)))
        for (sort in UpcomingTaskSort.entries) {
            assertEquals(listOf(parent), upcomingTaskGroups(listOf(parent), sort).flatMap { it.tasks })
        }
        assertFalse(parent.copy(subtasks = parent.subtasks.map { it.copy(isCompleted = true) }).isCompleted)
    }
}
