package cn.super12138.todo.logic

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

const val DEFAULT_DUE_TIME_MINUTES = 10 * 60

fun formatDueTime(minutes: Int): String = LocalTime.of(minutes / 60, minutes % 60)
    .format(DateTimeFormatter.ofPattern("HH:mm"))

fun dueTimestamp(dateMillis: Long, minutes: Int?, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        .atTime(LocalTime.ofSecondOfDay((minutes ?: 0) * 60L)).atZone(zone).toInstant().toEpochMilli()

fun pickerDateMillis(dateMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun localDateFromPicker(pickerMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(pickerMillis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

fun isDueOn(dateMillis: Long?, date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
    dateMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == date } ?: false
