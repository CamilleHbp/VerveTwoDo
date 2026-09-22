package cn.super12138.todo.logic.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Transaction
import cn.super12138.todo.constants.Constants
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT COUNT(*) FROM ${Constants.DB_TABLE_NAME}")
    suspend fun count(): Int

    @Transaction
    suspend fun insertIfEmpty(tasks: List<TaskEntity>): Boolean {
        if (count() != 0) return false
        tasks.forEach { insert(it) }
        return true
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Query("SELECT * FROM ${Constants.DB_TABLE_NAME}")
    fun getAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM ${Constants.DB_TABLE_NAME} WHERE id = :taskId")
    fun observeTask(taskId: Int): Flow<TaskEntity?>

    @Query("SELECT * FROM ${Constants.DB_TABLE_NAME} WHERE id = :taskId")
    suspend fun getTask(taskId: Int): TaskEntity?

    @Query("SELECT * FROM ${Constants.DB_TABLE_NAME}")
    suspend fun getTasksForTagUpdate(): List<TaskEntity>

    @Query("UPDATE ${Constants.DB_TABLE_NAME} SET tags = :tags, category = :category WHERE id = :taskId")
    suspend fun updateTags(taskId: Int, tags: List<String>, category: String)

    @Transaction
    suspend fun replaceTag(old: String, replacement: String?) {
        getTasksForTagUpdate().forEach { task ->
            if (old in task.taskTags) {
                val tags = cn.super12138.todo.logic.replaceTag(task.taskTags, old, replacement)
                updateTags(task.id, tags, tags.firstOrNull().orEmpty())
            }
        }
    }

    @Query("UPDATE ${Constants.DB_TABLE_NAME} SET subtasks = :subtasks WHERE id = :taskId")
    suspend fun updateSubtasks(taskId: Int, subtasks: List<Subtask>)

    @Transaction
    suspend fun setSubtaskCompleted(taskId: Int, subtaskId: String, completed: Boolean) {
        val task = getTask(taskId) ?: return
        updateSubtasks(taskId, task.subtasks.map {
            if (it.id == subtaskId) it.copy(isCompleted = completed) else it
        })
    }

    @Query("SELECT DISTINCT category FROM ${Constants.DB_TABLE_NAME} WHERE TRIM(category) != ''")
    fun getCategories(): Flow<List<String>>

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE ${Constants.DB_TABLE_NAME} SET completed = 1 WHERE id = :taskId")
    suspend fun completeTask(taskId: Int)

    @Query("UPDATE ${Constants.DB_TABLE_NAME} SET completed = 0 WHERE id = :taskId")
    suspend fun restoreTask(taskId: Int)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM ${Constants.DB_TABLE_NAME} WHERE id in (:taskIds)")
    suspend fun deleteFromIds(taskIds: Set<Int>)

    /*@Query("DELETE FROM todo")
    suspend fun deleteAllTodo()*/
}
