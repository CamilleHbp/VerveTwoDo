package cn.super12138.todo.ui.widget.upcoming

import android.content.Intent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import cn.super12138.todo.R
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.database.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class WidgetTaskCreationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = GlobalContext.get().get<TaskRepository>()
    private val title = "Widget creation test ${UUID.randomUUID()}"
    private val seedTitle = "$title seed"

    @After fun removeCreatedTask() = runBlocking {
        val ids = repository.getAllTasks().first().filter { it.content == title || it.content == seedTitle }.map { it.id }.toSet()
        if (ids.isNotEmpty()) repository.deleteTaskFromIds(ids)
    }

    @Test fun savesSelectedTagAndTodayAfterRecreationThenCloses() {
        withEditor("Widget test") { scenario, _ ->
            enterTitle()
            scenario.recreate()
            compose.onNode(hasSetTextAction() and hasText(title)).assertExists()
            save()
            compose.waitUntil(10_000) { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
            val task = runBlocking { repository.getAllTasks().first().single { it.content == title } }
            assertEquals("Widget test", task.category)
            assertEquals(LocalDate.now(), Instant.ofEpochMilli(task.dueDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate())
            assertNotNull(task.createdAtMillis)
        }
    }

    @Test fun savesWithoutCategoryThenCloses() {
        withEditor("") { scenario, _ ->
            enterTitle()
            save()
            compose.waitUntil(10_000) { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
            val task = runBlocking { repository.getAllTasks().first().single { it.content == title } }
            assertEquals("", task.category)
        }
    }

    @Test fun blankTitleStaysOpenAndUnchangedBackCloses() {
        withEditor("Widget test") { scenario, activity ->
            save()
            compose.onNodeWithText(context.getString(R.string.error_no_content_entered)).assertExists()
            assertFalse(activity.isFinishing)
            compose.onNodeWithContentDescription(context.getString(R.string.action_back)).performClick()
            compose.waitUntil(10_000) { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
        }
    }

    @Test fun suggestsTagsFromCompletedTasksAndSavesSelectionAfterRecreation() {
        val tag = "Existing ${UUID.randomUUID()}"
        withEditor("") { scenario, _ ->
            runBlocking {
                repository.insertTask(TaskEntity(content = seedTitle, category = tag, isCompleted = true, priority = 0f))
            }
            enterTitle()
            compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.tag_optional)))
                .performTextReplacement(tag.take(16).uppercase())
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText(tag) and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(hasText(tag) and !hasSetTextAction()).performClick()
            scenario.recreate()
            compose.onNode(hasSetTextAction() and hasText(tag)).assertExists()
            save()
            compose.waitUntil(10_000) { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
            val task = runBlocking { repository.getAllTasks().first().single { it.content == title } }
            assertEquals(tag, task.category)
        }
    }

    @Test fun typedCaseVariantReusesExistingTagOnSave() {
        val tag = "Existing ${UUID.randomUUID()}"
        withEditor("") { scenario, _ ->
            runBlocking { repository.insertTask(TaskEntity(content = seedTitle, category = tag, priority = 0f)) }
            enterTitle()
            compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.tag_optional)))
                .performTextReplacement("  ${tag.uppercase()}  ")
            // Dismiss the suggestion popup by returning to the title before saving.
            compose.onNode(hasSetTextAction() and hasText(title)).performClick()
            save()
            compose.waitUntil(10_000) { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
            val task = runBlocking { repository.getAllTasks().first().single { it.content == title } }
            assertEquals(tag, task.category)
        }
    }

    private fun enterTitle() {
        compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.placeholder_add_todo)))
            .performTextInput(title)
    }

    private fun save() {
        compose.onNodeWithContentDescription(context.getString(R.string.action_save)).performClick()
    }

    private fun withEditor(category: String, block: (ActivityScenario<WidgetTaskCreationActivity>, WidgetTaskCreationActivity) -> Unit) {
        val intent = Intent(context, WidgetTaskCreationActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(WidgetTaskCreationActivity.EXTRA_CATEGORY, category)
        ActivityScenario.launch<WidgetTaskCreationActivity>(intent).use { scenario ->
            lateinit var activity: WidgetTaskCreationActivity
            scenario.onActivity { activity = it }
            compose.waitForIdle()
            block(scenario, activity)
        }
    }
}
