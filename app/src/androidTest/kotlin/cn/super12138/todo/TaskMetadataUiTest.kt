package cn.super12138.todo

import android.graphics.Bitmap
import android.widget.FrameLayout
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.appwidget.compose
import androidx.test.platform.app.InstrumentationRegistry
import cn.super12138.todo.logic.SettingsRepository
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.ui.components.DueTimeDialog
import cn.super12138.todo.ui.pages.editor.EditorViewModel
import cn.super12138.todo.ui.pages.editor.TaskEditorPage
import cn.super12138.todo.ui.pages.settings.components.category.CategoryPromptDialog
import cn.super12138.todo.ui.theme.VerveDoTheme
import cn.super12138.todo.ui.widget.upcoming.UpcomingTaskWidget
import cn.super12138.todo.ui.widget.upcoming.UpcomingTaskWidgetReceiver
import cn.super12138.todo.utils.ConfettiController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class TaskMetadataUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val repo get() = GlobalContext.get().get<TaskRepository>()
    private val settings get() = GlobalContext.get().get<SettingsRepository>()
    private val ids = mutableSetOf<Int>()

    @After fun cleanUp() = runBlocking { if (ids.isNotEmpty()) repo.deleteTaskFromIds(ids) }

    @Test fun editKeepsDetailsAndAllowsRemovingTagsAndTime() {
        val task = seed("Review the kitchen drawings", 0)
        val model = EditorViewModel(task, repo, settings, GlobalContext.get().get<TagRepository>(), ConfettiController())
        var saved = false
        compose.setContent { VerveDoTheme { TaskEditorPage(task = task, viewModel = model, onSaved = { saved = true }) } }
        compose.onNodeWithText(context.getString(R.string.remove_due_time)).performScrollTo()
        capture("editor")
        compose.onNodeWithText(context.getString(R.string.remove_due_time)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.tag_remove, "Home")).performScrollTo().performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.action_save)).performClick()
        compose.waitUntil(10_000) { saved }
        val updated = runBlocking { repo.getAllTasks().first().single { it.id == task.id } }
        assertEquals(listOf("Work"), updated.tags)
        assertNull(updated.dueTimeMinutes)
        assertEquals(task.details, updated.details)
    }

    @Test fun timePickerConfirms24HourTime() {
        var result: Int? = null
        compose.setContent { VerveDoTheme { DueTimeDialog(14 * 60 + 30, { result = it }, {}) } }
        capture("time-picker")
        compose.onNodeWithText(context.getString(R.string.action_confirm)).performClick()
        compose.runOnIdle { assertEquals(870, result) }
    }

    @Test fun colorCanBeCustomizedAndInvalidHexCannotSave() {
        var result: Int? = null
        compose.setContent { VerveDoTheme {
            CategoryPromptDialog(visible = true, initialCategory = "Work", initialColor = 0xFF3877B8.toInt(),
                onSave = {}, onSaveColor = { _, color -> result = color }, onDismiss = {})
        } }
        capture("tag-colour")
        val field = compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.tag_color)))
        field.performTextReplacement("#invalid")
        compose.onNodeWithText(context.getString(R.string.action_save)).performClick()
        compose.runOnIdle { assertNull(result) }
        field.performTextReplacement("#A15475")
        compose.onNodeWithText(context.getString(R.string.action_save)).performClick()
        compose.runOnIdle { assertEquals(0xFFA15475.toInt(), result) }
    }

    @Test fun upcomingWidgetRendersMultipleColorsAnd24HourTimes() {
        seed("Review the kitchen drawings", 0)
        seed("Book the train tickets", 1)
        seed("Prepare the autumn planting", 12)
        val remoteViews = runBlocking {
            UpcomingTaskWidget().compose(context, size = DpSize(340.dp, 620.dp), state = mutablePreferencesOf())
        }
        lateinit var host: AppWidgetHostView
        compose.setContent { VerveDoTheme { Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            AndroidView(factory = { activityContext ->
                AppWidgetHostView(activityContext).apply {
                    setAppWidget(-1, AppWidgetManager.getInstance(context).installedProviders.single {
                        it.provider == ComponentName(context, UpcomingTaskWidgetReceiver::class.java)
                    })
                    updateAppWidget(remoteViews)
                    host = this
                }
            }, modifier = Modifier.size(340.dp, 620.dp))
            }
        } } }
        compose.waitUntil(10_000) {
            var ready = false
            instrumentation.runOnMainSync { ready = host.texts().any { it.contains("14:30") } }
            ready
        }
        capture("widget")
    }

    private fun seed(title: String, days: Long): TaskEntity = runBlocking {
        val task = TaskEntity(content = title, category = "Work", tags = listOf("Work", "Home"),
            details = "Bring the floor plan and confirm the measurements before the meeting.", priority = 0f,
            dueDateMillis = LocalDate.now().plusDays(days).atTime(14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dueTimeMinutes = 870)
        repo.insertTask(task)
        repo.getAllTasks().first().last { it.content == title }.also { ids += it.id }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        if (InstrumentationRegistry.getArguments().getString("captureMetadata") != "true") return
        instrumentation.waitForIdleSync()
        SystemClock.sleep(600)
        val file = File(context.getExternalFilesDir(null), "metadata-review/$name.png")
        file.parentFile!!.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun View.texts(): List<String> = when (this) {
        is TextView -> listOf(text.toString())
        is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).texts() }
        else -> emptyList()
    }
}
