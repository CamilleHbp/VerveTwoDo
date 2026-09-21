package cn.super12138.todo.logic.database

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

class TaskConverters {
    @ColumnTypeConverter fun encodeTags(tags: List<String>): String = Json.encodeToString(tags)
    @ColumnTypeConverter fun decodeTags(value: String): List<String> = Json.decodeFromString(value)
}
