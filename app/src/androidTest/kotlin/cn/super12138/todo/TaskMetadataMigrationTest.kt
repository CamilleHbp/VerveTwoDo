package cn.super12138.todo

import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.test.platform.app.InstrumentationRegistry
import cn.super12138.todo.logic.database.TaskDatabase
import cn.super12138.todo.logic.database.taskTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TaskMetadataMigrationTest {
    @Test fun upgradesLegacyBackupWithoutLosingDataAndRoundTripsMetadata() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (version in listOf(5, 6)) {
            val name = "metadata-migration-${UUID.randomUUID()}"
            val file = context.getDatabasePath(name)
            file.parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE todo (content TEXT NOT NULL, category TEXT NOT NULL, completed INTEGER NOT NULL, priority REAL NOT NULL, due_date INTEGER, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL" +
                    (if (version == 6) ", created_at INTEGER" else "") + ")")
                db.execSQL("INSERT INTO todo (content, category, completed, priority, due_date, id) VALUES ('Legacy task', 'Work', 1, 2.0, 1790006400000, 42)")
                db.version = version
            }
            val db = Room.databaseBuilder(context, TaskDatabase::class.java, name)
                .addMigrations(TaskDatabase.MIGRATION_5_6, TaskDatabase.MIGRATION_6_7).build()
            try {
                val old = db.taskDao().getAll().first().single()
                assertEquals(42, old.id)
                assertEquals("Legacy task", old.content)
                assertEquals(listOf("Work"), old.taskTags)
                assertTrue(old.isCompleted)
                assertEquals(2f, old.priority)
                assertEquals(1790006400000, old.dueDateMillis)
                assertNull(old.dueTimeMinutes)
                assertEquals("", old.details)
                val updated = old.copy(tags = listOf("Work", "Home, garden", "Été"),
                    details = "First line\nSecond line, with \"quotes\"", dueTimeMinutes = 1439)
                db.taskDao().insert(updated)
                assertEquals(updated, db.taskDao().getAll().first().single())
                db.taskDao().insert(updated.copy(tags = emptyList(), category = "", dueTimeMinutes = null))
                assertTrue(db.taskDao().getAll().first().single().taskTags.isEmpty())
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
    }
}
