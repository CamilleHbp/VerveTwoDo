package cn.super12138.todo.logic

import cn.super12138.todo.logic.database.TaskConverters
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.database.taskTags
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TaskMetadataTest {
    @Test fun legacyTagsAndJsonRoundTripPreserveNames() {
        assertEquals(listOf("Work"), TaskEntity("Old", "Work", priority = 0f).taskTags)
        val tags = listOf("Work, home", "quotes \" and \\", "Été", "line\nbreak")
        val converter = TaskConverters()
        assertEquals(tags, converter.decodeTags(converter.encodeTags(tags)))
        assertEquals(tags, TaskEntity("New", "Old", priority = 0f, tags = tags).taskTags)
    }

    @Test fun colorsStayStableAndUnusedDefaultsContinueBeyondPalette() {
        val names = (1..100).map { "Tag $it" }
        val colors = assignTagColors(names, emptyMap())
        assertEquals(100, colors.values.distinct().size)
        assertEquals(colors, assignTagColors(names.reversed(), colors))
        val changed = colors + ("Tag 1" to 0xFFABCDEF.toInt())
        assertEquals(changed, assignTagColors(names, changed))
        assertEquals(101, assignTagColors(names + "New", changed).size)
    }

    @Test fun pickerAndTimesUseLocalCalendarDaysAcrossZonesAndDst() {
        listOf("America/Los_Angeles", "Europe/Paris", "Pacific/Auckland").forEach { id ->
            val zone = ZoneId.of(id)
            listOf(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 10, 25)).forEach { date ->
                val day = date.atStartOfDay(zone).toInstant().toEpochMilli()
                assertEquals(day, localDateFromPicker(pickerDateMillis(day, zone), zone))
                val timed = dueTimestamp(day, 23 * 60 + 45, zone)
                assertTrue(isDueOn(timed, date, zone))
                assertEquals(23, Instant.ofEpochMilli(timed).atZone(zone).hour)
                assertFalse(isDueOn(timed, date.plusDays(1), zone))
                assertEquals(day, dueTimestamp(timed, null, zone))
            }
        }
        assertEquals("00:00", formatDueTime(0))
        assertEquals("10:00", formatDueTime(DEFAULT_DUE_TIME_MINUTES))
        assertEquals("23:59", formatDueTime(1439))
    }
}
