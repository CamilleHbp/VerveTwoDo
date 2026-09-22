package cn.super12138.todo.ui.pages.overview

import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.model.OverviewCard
import cn.super12138.todo.logic.model.OverviewCardConfig
import cn.super12138.todo.logic.model.OverviewCardSize
import cn.super12138.todo.logic.model.OverviewLayout
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class OverviewStateTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val today = LocalDate.of(2026, 3, 29) // A 23-hour day.

    @Test fun calendarGroupsHandleTimedTasksDstAndTheSevenDayBoundary() {
        val tasks = listOf(task(1, -1), task(2, 0), task(3, 1), task(4, 7), task(5, 8),
            task(6, -2).copy(isCompleted = true), task(7, 0).copy(isCompleted = true),
            task(8, null))
        assertEquals(listOf(1), ids(tasks, OverviewCard.Overdue))
        assertEquals(listOf(2, 7), ids(tasks, OverviewCard.Today))
        assertEquals(listOf(3, 4), ids(tasks, OverviewCard.Upcoming))
        assertEquals(setOf(1, 2, 3, 4, 5, 8), ids(tasks, OverviewCard.Pending).toSet())
        assertEquals(setOf(6, 7), ids(tasks, OverviewCard.Completed).toSet())
        assertEquals(8, ids(tasks, OverviewCard.All).size)
    }

    @Test fun midnightMovesTasksBetweenGroupsWithoutADataMutation() {
        val tasks = listOf(task(1, 0), task(2, 1))
        val nextDay = today.plusDays(1)
        assertEquals(listOf(1), overviewTasks(tasks, OverviewCard.Overdue, nextDay, zone).map { it.id })
        assertEquals(listOf(2), overviewTasks(tasks, OverviewCard.Today, nextDay, zone).map { it.id })
    }

    @Test fun locksKeepTheirAbsoluteSlotsWhenOtherCardsCrossThem() {
        val layout = OverviewLayout().configure(OverviewCard.Today) { it.copy(locked = true) }
        val moved = layout.move(OverviewCard.Overdue, OverviewCard.Completed)
        assertEquals(OverviewCard.Today, moved.cards[1].card)
        assertEquals(OverviewCard.Overdue, moved.cards.last().card)
        assertEquals(layout, layout.move(OverviewCard.Today, OverviewCard.All))
        assertEquals(layout, layout.move(OverviewCard.All, OverviewCard.Today))
        assertNull(layout.neighbor(OverviewCard.Today, 1))
    }

    @Test fun layoutRoundTripKeepsOrderVisibilitySizeAndLocks() {
        val layout = OverviewLayout().move(OverviewCard.Upcoming, OverviewCard.Overdue)
            .configure(OverviewCard.All) { it.copy(visible = false) }
            .configure(OverviewCard.Today) { it.copy(locked = true, size = OverviewCardSize.Compact) }
        assertEquals(layout, OverviewLayout.decode(layout.encode()))
    }

    @Test fun incompleteOrMalformedSavedLayoutsNeverLoseAvailableCards() {
        val duplicate = OverviewLayout(listOf(OverviewCardConfig(OverviewCard.Today, visible = false),
            OverviewCardConfig(OverviewCard.Today)))
        val normalized = OverviewLayout.decode(duplicate.encode())
        assertEquals(OverviewCard.entries.size, normalized.cards.size)
        assertEquals(OverviewCard.Today, normalized.cards.first().card)
        assertFalse(normalized.cards.first().visible)
        assertEquals(OverviewLayout(), OverviewLayout.decode("invalid"))
        assertEquals(OverviewLayout(), OverviewLayout.decode(null))
    }

    private fun ids(tasks: List<TaskEntity>, card: OverviewCard) =
        overviewTasks(tasks, card, today, zone).map { it.id }

    private fun task(id: Int, days: Long?) = TaskEntity(id = id, content = "Task $id", priority = 0f,
        dueDateMillis = days?.let { today.plusDays(it).atTime(8, 30).atZone(zone).toInstant().toEpochMilli() })
}
