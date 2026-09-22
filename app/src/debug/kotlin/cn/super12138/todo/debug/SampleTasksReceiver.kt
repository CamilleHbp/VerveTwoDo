package cn.super12138.todo.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cn.super12138.todo.logic.database.TaskDao
import cn.super12138.todo.logic.database.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.ZoneId

/** Invoked by the development launcher; absent from release builds. */
class SampleTasksReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inserted = GlobalContext.get().get<TaskDao>().insertIfEmpty(sampleTasks())
                pending.setResult(0, if (inserted) "Sample tasks added" else "Existing tasks preserved", null)
            } catch (error: Exception) {
                Log.e("SampleTasks", "Could not seed tasks", error)
                pending.setResult(1, "Could not seed tasks: ${error.message}", null)
            } finally {
                pending.finish()
            }
        }
    }
}

private fun sampleTasks(): List<TaskEntity> {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val created = System.currentTimeMillis()
    fun task(title: String, tag: String, priority: Float, days: Long?, details: String = "", done: Boolean = false) =
        TaskEntity(
            content = title, category = tag, tags = listOf(tag), priority = priority,
            dueDateMillis = days?.let { today.plusDays(it).atStartOfDay(zone).toInstant().toEpochMilli() },
            details = details, isCompleted = done, createdAtMillis = created,
        )
    return listOf(
        task("Plan the week", "Personal", 2f, 0, "Choose three priorities and review upcoming appointments."),
        task("Review the website mockups", "Work", 1f, 0, "Check typography, spacing, and the mobile layout."),
        task("Buy groceries for dinner", "Shopping", 0f, 0, "Tomatoes, pasta, basil, parmesan, and fresh bread."),
        task("Book a dentist appointment", "Personal", 1f, -2),
        task("Send the project update", "Work", 2f, -1, "Summarize progress and next steps."),
        task("Go for a morning walk", "Health", -1f, 1),
        task("Read a chapter", "Personal", -2f, null),
        task("Plan a weekend getaway", "Travel", 0f, 5),
        task("Prepare the presentation", "Work", 1f, 3),
        task("Water the plants", "Home", -1f, 2),
        task("Try a new recipe", "Home", 0f, null),
        task("Organize the desk", "Home", -1f, -1, done = true),
        task("Back up holiday photos", "Personal", 0f, -3, done = true),
        task("Reserve the restaurant", "Travel", 1f, 4, "Table for two at 19:30.", done = true),
    )
}
