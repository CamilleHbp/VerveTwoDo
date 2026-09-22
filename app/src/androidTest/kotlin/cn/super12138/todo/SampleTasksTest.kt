package cn.super12138.todo

import androidx.room3.Room
import androidx.test.platform.app.InstrumentationRegistry
import cn.super12138.todo.logic.database.TaskDatabase
import cn.super12138.todo.logic.database.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SampleTasksTest {
    @Test fun seedsOnlyAnEmptyDatabase() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TaskDatabase::class.java,
        ).build()
        try {
            val dao = db.taskDao()
            val sample = TaskEntity(content = "Sample", priority = 0f)
            assertTrue(dao.insertIfEmpty(listOf(sample)))
            assertFalse(dao.insertIfEmpty(listOf(sample)))
            assertEquals(1, dao.count())
            val saved = dao.getAll().first().single()
            dao.update(saved.copy(content = "User edited task", isCompleted = true))
            assertFalse(dao.insertIfEmpty(listOf(sample)))
            assertEquals("User edited task", dao.getAll().first().single().content)
            assertTrue(dao.getAll().first().single().isCompleted)
        } finally {
            db.close()
        }
    }
}
