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
