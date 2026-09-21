package cn.super12138.todo.logic.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import cn.super12138.todo.constants.Constants
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = Constants.DB_TABLE_NAME)
data class TaskEntity(
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "priority") val priority: Float,
    @ColumnInfo(name = "due_date") val dueDateMillis: Long? = null,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "created_at") val createdAtMillis: Long? = null,
    @ColumnInfo(name = "details", defaultValue = "''") val details: String = "",
    @ColumnInfo(name = "tags", defaultValue = "'[]'") val tags: List<String> = emptyList(),
    @ColumnInfo(name = "due_time") val dueTimeMinutes: Int? = null,
)

/** Old backups and navigation entries still carry their single category. */
val TaskEntity.taskTags: List<String>
    get() = tags.ifEmpty { listOf(category) }.filter { it.isNotBlank() }.distinct()
