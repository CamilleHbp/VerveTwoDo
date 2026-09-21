package cn.super12138.todo.ui.widget.upcoming

import androidx.datastore.preferences.core.mutablePreferencesOf
import cn.super12138.todo.logic.database.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UpcomingTasksTest {
    private val today = LocalDate.of(2026, 9, 21)
    private val zone = ZoneId.of("Europe/Paris")

    @Test fun includesUnfinishedOverdueButExcludesUndatedAndCompletedPastTasksInEverySort() {
        val tasks = listOf(task(1, day = today.minusDays(1)), task(2), task(3, day = today.plusYears(1)),
            task(4).copy(dueDateMillis = null), task(5).copy(isCompleted = true),
            task(6, day = today.minusDays(1)).copy(isCompleted = true))
        UpcomingTaskSort.entries.forEach {
            assertEquals(setOf(1, 2, 3, 5), ids(tasks, it).toSet())
        }
    }

    @Test fun overdueBoundaryUsesLocalMidnight() {
        val midnight = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val tasks = listOf(task(1).copy(dueDateMillis = midnight - 1), task(2).copy(dueDateMillis = midnight))
        val result = groups(tasks)
        assertEquals(listOf(1), result.first().tasks.map { it.id })
        assertTrue(result.first().isOverdue)
        assertEquals(listOf(2), result.last().tasks.map { it.id })
        assertEquals(today, result.last().date)
        assertFalse(result.last().isOverdue)
    }

    @Test fun overdueSectionIsFirstAndRespectsEverySelectedSort() {
        val tasks = listOf(
            task(3, day = today.minusDays(2), priority = 1f).copy(content = "Zebra", createdAtMillis = 200),
            task(1, day = today.minusDays(1), priority = 2f).copy(content = "Alpha", createdAtMillis = 100),
            task(2, priority = 3f).copy(content = "Aardvark", createdAtMillis = 300))
        val expected = mapOf(
            UpcomingTaskSort.DueDate to listOf(3, 1),
            UpcomingTaskSort.DueDateLatest to listOf(1, 3),
            UpcomingTaskSort.CreatedNewest to listOf(3, 1),
            UpcomingTaskSort.CreatedOldest to listOf(1, 3),
            UpcomingTaskSort.Priority to listOf(1, 3),
            UpcomingTaskSort.Alphabetical to listOf(1, 3))
        UpcomingTaskSort.entries.forEach { sort ->
            val result = groups(tasks, sort)
            assertEquals(sort.name, 2, result.size)
            assertTrue(result.first().isOverdue)
            assertEquals(sort.name, expected[sort], result.first().tasks.map { it.id })
            assertFalse(result.last().isOverdue)
            assertEquals(listOf(2), result.last().tasks.map { it.id })
        }
    }

    @Test fun overdueSectionHonorsTagsAndDisappearsWhenFilteredOut() {
        val tasks = listOf(task(1, "Work", today.minusDays(1)), task(2, "Home", today.minusDays(2)),
            task(3, day = today.minusDays(1)), task(4, "Future"))
        UpcomingTaskSort.entries.forEach { sort ->
            assertEquals(setOf(1, 3), ids(tasks, sort, setOf("Work", "")).toSet())
            assertEquals(listOf(2), ids(tasks, sort, setOf("Home")))
            val filtered = groups(tasks, sort, setOf("Future"))
            assertEquals(1, filtered.size)
            assertFalse(filtered.single().isOverdue)
            assertTrue(groups(tasks, sort, setOf("Missing")).isEmpty())
        }
    }

    @Test fun completingRestoringAndReschedulingAnOverdueTaskUpdatesItsSection() {
        val missed = task(1, day = today.minusDays(1))
        UpcomingTaskSort.entries.forEach { sort ->
            assertTrue(groups(listOf(missed), sort).single().isOverdue)
            val completed = missed.copy(isCompleted = true)
            assertTrue(groups(listOf(completed), sort).isEmpty())
            assertTrue(groups(listOf(completed.copy(isCompleted = false)), sort).single().isOverdue)
            val rescheduled = missed.copy(dueDateMillis = task(1).dueDateMillis)
            assertFalse(groups(listOf(rescheduled), sort).single().isOverdue)
            assertEquals(listOf(1), ids(listOf(rescheduled.copy(isCompleted = true)), sort))
        }
    }

    @Test fun overdueOnlyInputHasNoEmptyUpcomingGroup() {
        val tasks = listOf(task(1, day = today.minusYears(1)), task(2, day = today.minusDays(1)))
        UpcomingTaskSort.entries.forEach { sort ->
            val result = groups(tasks, sort)
            assertEquals(1, result.size)
            assertTrue(result.single().isOverdue)
            assertEquals(2, result.single().tasks.size)
        }
    }

    @Test fun dateSortGroupsByDayInEitherDirection() {
        val tasks = listOf(task(1), task(2, day = today.plusDays(1)))
        assertEquals(listOf(today, today.plusDays(1)), groups(tasks).map { it.date })
        assertEquals(listOf(today.plusDays(1), today), groups(tasks, UpcomingTaskSort.DueDateLatest).map { it.date })
    }

    @Test fun dateTiesUsePriorityThenStableId() {
        val tasks = listOf(task(3, priority = 2f), task(2), task(1, priority = 2f))
        assertEquals(listOf(1, 3, 2), ids(tasks))
    }

    @Test fun completedTasksStayAtTheBottomOfTheirDayInEitherDateDirection() {
        val tasks = listOf(task(1, priority = 2f).copy(isCompleted = true), task(2),
            task(3, priority = 1f).copy(isCompleted = true), task(4, priority = 2f),
            task(5, day = today.plusDays(1)), task(6, day = today.plusDays(1)).copy(isCompleted = true))
        assertEquals(listOf(4, 2, 1, 3, 5, 6), ids(tasks))
        assertEquals(listOf(5, 6, 4, 2, 1, 3), ids(tasks, UpcomingTaskSort.DueDateLatest))
        val completedToday = tasks.map { if (it.id <= 4) it.copy(isCompleted = true) else it }
        assertEquals(listOf(today, today.plusDays(1)), groups(completedToday).map { it.date })
        assertEquals(listOf(today.plusDays(1), today), groups(completedToday, UpcomingTaskSort.DueDateLatest).map { it.date })
    }

    @Test fun selectedSortOrdersPendingAndCompletedTasksIndependently() {
        val hour = 3_600_000L
        val tasks = listOf(
            task(1, priority = 1f).copy(content = "Zebra", createdAtMillis = 100, isCompleted = true),
            task(2).copy(content = "apple", createdAtMillis = 400, dueDateMillis = task(2).dueDateMillis!! + 3 * hour),
            task(3, priority = 2f).copy(content = "Books", createdAtMillis = 300, isCompleted = true,
                dueDateMillis = task(3).dueDateMillis!! + 2 * hour),
            task(4, priority = 1f).copy(content = "Delta", createdAtMillis = 200, dueDateMillis = task(4).dueDateMillis!! + hour))
        val expected = mapOf(
            UpcomingTaskSort.DueDate to listOf(4, 2, 1, 3),
            UpcomingTaskSort.DueDateLatest to listOf(2, 4, 3, 1),
            UpcomingTaskSort.CreatedNewest to listOf(2, 4, 3, 1),
            UpcomingTaskSort.CreatedOldest to listOf(4, 2, 1, 3),
            UpcomingTaskSort.Priority to listOf(4, 2, 3, 1),
            UpcomingTaskSort.Alphabetical to listOf(2, 4, 3, 1))
        UpcomingTaskSort.entries.forEach { sort ->
            assertEquals(sort.name, expected[sort], ids(tasks, sort))
            val restored = tasks.map { if (it.id == 1) it.copy(isCompleted = false) else it }
            assertEquals(sort.name, 3, ids(restored, sort).last())
        }
    }

    @Test fun tagFiltersApplyToCompletedTasksToo() {
        val tasks = listOf(task(1, "Work"), task(2, "Home").copy(isCompleted = true),
            task(3, "Work").copy(isCompleted = true), task(4).copy(isCompleted = true))
        UpcomingTaskSort.entries.forEach { sort ->
            assertEquals(listOf(1, 3), ids(tasks, sort, setOf("Work")))
            assertEquals(setOf(1, 3, 4), ids(tasks, sort, setOf("Work", "")).toSet())
        }
    }

    @Test fun filteringMultipleTagsDoesNotChangeSortOrder() {
        val tasks = listOf(task(1, "Work", today.plusDays(2)), task(2, "Home"), task(3, "Other"), task(4, "Work"))
        for (sort in UpcomingTaskSort.entries) {
            val unfiltered = ids(tasks, sort)
            assertEquals(unfiltered.filter { it != 3 }, ids(tasks, sort, setOf("Home", "Work")))
            assertEquals(listOf(2), ids(tasks, sort, setOf("Home")))
            assertTrue(ids(tasks, sort, setOf("Deleted tag")).isEmpty())
        }
    }

    @Test fun noTagCanBeCombinedWithNamedTags() {
        val tasks = listOf(task(1, "Work"), task(2), task(3, " "), task(4, "Home"))
        assertEquals(listOf(1, 2, 3, 4), ids(tasks))
        assertEquals(listOf(2, 3), ids(tasks, tags = setOf("")))
        assertEquals(listOf(1, 2, 3), ids(tasks, tags = setOf("", "Work")))
    }

    @Test fun tagMatchingIsExact() {
        assertEquals(listOf(1), ids(listOf(task(1, "Work"), task(2, "work")), tags = setOf("Work")))
    }

    @Test fun creationSortUsesTimestampsNotDueDatesOrIds() {
        val tasks = listOf(task(1).copy(createdAtMillis = 300), task(3).copy(createdAtMillis = 100),
            task(2, day = today.plusDays(5)).copy(createdAtMillis = 200))
        assertEquals(listOf(3, 2, 1), ids(tasks, UpcomingTaskSort.CreatedOldest))
        assertEquals(listOf(1, 2, 3), ids(tasks, UpcomingTaskSort.CreatedNewest))
        assertEquals(1, groups(tasks, UpcomingTaskSort.CreatedNewest).size)
        assertEquals(null, groups(tasks, UpcomingTaskSort.CreatedNewest).single().date)
    }

    @Test fun legacyCreationOrderIsPreservedWithNewTasks() {
        val tasks = listOf(task(2), task(1), task(3).copy(createdAtMillis = 100))
        assertEquals(listOf(1, 2, 3), ids(tasks, UpcomingTaskSort.CreatedOldest))
        assertEquals(listOf(3, 2, 1), ids(tasks, UpcomingTaskSort.CreatedNewest))
    }

    @Test fun prioritySortPutsHighestFirstThenUsesDueDate() {
        val tasks = listOf(task(1), task(2, day = today.plusDays(1), priority = 2f), task(3, priority = 2f))
        assertEquals(listOf(3, 2, 1), ids(tasks, UpcomingTaskSort.Priority))
    }

    @Test fun alphabeticalSortIgnoresCase() {
        val tasks = listOf(task(1).copy(content = "Zebra"), task(2).copy(content = "apple"), task(3).copy(content = "Books"))
        assertEquals(listOf(2, 3, 1), ids(tasks, UpcomingTaskSort.Alphabetical))
    }

    @Test fun daylightSavingUsesCalendarDays() {
        val day = LocalDate.of(2026, 3, 29)
        val tasks = listOf(task(1, day = day), task(2).copy(
            dueDateMillis = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()))
        assertEquals(listOf(day, day.plusDays(1)),
            upcomingTaskGroups(tasks, UpcomingTaskSort.DueDate, today = day, zone = zone).map { it.date })
        val nextDay = upcomingTaskGroups(tasks, UpcomingTaskSort.DueDate,
            today = day.plusDays(1), zone = zone)
        assertTrue(nextDay.first().isOverdue)
        assertEquals(listOf(1), nextDay.first().tasks.map { it.id })
        assertEquals(day.plusDays(1), nextDay.last().date)
        assertEquals(listOf(2), nextDay.last().tasks.map { it.id })
    }

    @Test fun changingTagsPreservesSortAndCollapsedControls() {
        val prefs = mutablePreferencesOf(UpcomingWidgetPreferences.sortKey to UpcomingTaskSort.CreatedNewest.name,
            UpcomingWidgetPreferences.controlsKey to false)
        UpcomingWidgetPreferences.setCategories(prefs, setOf("Work", "Home"))
        assertEquals(setOf("Work", "Home"), UpcomingWidgetPreferences.categories(prefs))
        assertEquals(UpcomingTaskSort.CreatedNewest, UpcomingWidgetPreferences.sort(prefs))
        assertFalse(prefs[UpcomingWidgetPreferences.controlsKey]!!)
        UpcomingWidgetPreferences.setCategories(prefs, emptySet())
        assertTrue(UpcomingWidgetPreferences.categories(prefs).isEmpty())
    }

    @Test fun oldWidgetPreferencesMigrateWithoutLosingFilter() {
        val prefs = mutablePreferencesOf(UpcomingWidgetPreferences.categoryKey to "Work",
            UpcomingWidgetPreferences.sortKey to "Tag")
        assertEquals(setOf("Work"), UpcomingWidgetPreferences.categories(prefs))
        assertEquals(UpcomingTaskSort.DueDate, UpcomingWidgetPreferences.sort(prefs))
        UpcomingWidgetPreferences.setCategories(prefs, emptySet())
        assertTrue(UpcomingWidgetPreferences.categories(prefs).isEmpty())
        assertEquals(null, prefs[UpcomingWidgetPreferences.categoryKey])
    }

    @Test fun emptyInputHasNoGroups() {
        UpcomingTaskSort.entries.forEach { assertTrue(groups(emptyList(), it).isEmpty()) }
    }

    @Test fun choosingSameSortFieldPreservesDateDirection() {
        UpcomingTaskSort.entries.forEach { sort ->
            assertEquals(sort, sort.withField(sort.field()))
            assertEquals(sort, sort.reverseDateOrder().reverseDateOrder())
        }
        assertEquals(UpcomingTaskSort.CreatedNewest, UpcomingTaskSort.Priority.withField(UpcomingSortField.CreationDate))
        assertEquals(UpcomingTaskSort.DueDate, UpcomingTaskSort.CreatedOldest.withField(UpcomingSortField.DueDate))
    }

    @Test fun everyPickerMethodSortsWithoutLosingTagFiltering() {
        val tasks = listOf(task(1, "Work"), task(2, "Home"), task(3, "Work", today.plusDays(1)))
        UpcomingSortField.entries.forEach { field ->
            val sort = UpcomingTaskSort.DueDate.withField(field)
            assertEquals(field, sort.field())
            assertEquals(setOf(1, 3), ids(tasks, sort, setOf("Work")).toSet())
            assertEquals(setOf(1, 3), ids(tasks, sort.reverseDateOrder(), setOf("Work")).toSet())
        }
    }

    @Test fun undoExpiresWithoutChangingFilterOrSort() {
        val prefs = mutablePreferencesOf(UpcomingWidgetPreferences.undoIdKey to 42,
            UpcomingWidgetPreferences.undoUntilKey to 10_000L,
            UpcomingWidgetPreferences.undoTitleKey to "Task",
            UpcomingWidgetPreferences.sortKey to UpcomingTaskSort.CreatedOldest.name)
        UpcomingWidgetPreferences.setCategories(prefs, setOf("Work"))
        assertEquals(42, UpcomingWidgetPreferences.undoTaskId(prefs, 9_999L))
        assertEquals(null, UpcomingWidgetPreferences.undoTaskId(prefs, 10_000L))
        UpcomingWidgetPreferences.clearUndo(prefs)
        assertEquals(null, UpcomingWidgetPreferences.undoTaskId(prefs, 0L))
        assertEquals(null, prefs[UpcomingWidgetPreferences.undoTitleKey])
        assertEquals(setOf("Work"), UpcomingWidgetPreferences.categories(prefs))
        assertEquals(UpcomingTaskSort.CreatedOldest, UpcomingWidgetPreferences.sort(prefs))
    }

    private fun groups(tasks: List<TaskEntity>, sort: UpcomingTaskSort = UpcomingTaskSort.DueDate,
        tags: Set<String> = emptySet()) = upcomingTaskGroups(tasks, sort, tags, today, zone)
    private fun ids(tasks: List<TaskEntity>, sort: UpcomingTaskSort = UpcomingTaskSort.DueDate,
        tags: Set<String> = emptySet()) = groups(tasks, sort, tags).flatMap { it.tasks }.map { it.id }
    private fun task(id: Int, tag: String = "", day: LocalDate = today, priority: Float = 0f) = TaskEntity(
        id = id, content = "Task $id", category = tag,
        dueDateMillis = day.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), priority = priority)
}
