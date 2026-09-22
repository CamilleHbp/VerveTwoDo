package cn.super12138.todo.logic.database

import kotlinx.serialization.Serializable
import java.util.UUID

/** A checklist item belongs to its parent, never to a task collection or widget. */
@Serializable
data class Subtask(
    val content: String,
    val isCompleted: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)
