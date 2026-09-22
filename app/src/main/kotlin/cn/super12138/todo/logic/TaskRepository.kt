package cn.super12138.todo.logic

import cn.super12138.todo.logic.database.TaskDao
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.database.taskTags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao) {
    suspend fun insertTask(task: TaskEntity) {
        taskDao.insert(
            if (task.id == 0 && task.createdAtMillis == null) {
                task.copy(createdAtMillis = System.currentTimeMillis())
            } else task
        )
    }

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAll()

    fun observeTask(taskId: Int): Flow<TaskEntity?> = taskDao.observeTask(taskId)

    suspend fun setSubtaskCompleted(taskId: Int, subtaskId: String, completed: Boolean) =
        taskDao.setSubtaskCompleted(taskId, subtaskId, completed)

    fun getCategories(): Flow<List<String>> = taskDao.getAll().map { tasks -> tasks.flatMap { it.taskTags }.distinct() }

    suspend fun replaceTag(old: String, replacement: String?) = taskDao.replaceTag(old, replacement)

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
    }

    suspend fun completeTask(taskId: Int) {
        taskDao.completeTask(taskId)
    }

    suspend fun restoreTask(taskId: Int) {
        taskDao.restoreTask(taskId)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
    }

    suspend fun deleteTaskFromIds(tasks: Set<Int>) {
        taskDao.deleteFromIds(tasks)
    }

    /*suspend fun deleteAllTodo() {
        toDoDao.deleteAllTodo()
    }*/
}
