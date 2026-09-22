package cn.super12138.todo.ui.pages.overview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.test.platform.app.InstrumentationRegistry
import cn.super12138.todo.R
import cn.super12138.todo.logic.SettingsRepository
import cn.super12138.todo.logic.TagRepository
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.database.TaskDatabase
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.logic.datastore.DataStoreManager
import cn.super12138.todo.logic.model.OverviewCard
import cn.super12138.todo.logic.model.OverviewCardSize
import cn.super12138.todo.logic.model.OverviewLayout
import cn.super12138.todo.ui.theme.VerveDoTheme
import cn.super12138.todo.utils.toRelativeTimeString
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OverviewUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: TaskDatabase
    private lateinit var repository: TaskRepository
    private lateinit var settings: DataStoreManager
    private lateinit var model: OverviewViewModel
    private val store = ViewModelStore()
    private val preferencesScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferencesFile: File
    private var opened: TaskEntity? = null
    private var added = false
    private val longTitle = "Review the project proposal and send the final notes to the team"

    @Before fun prepare() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        repository = TaskRepository(database.taskDao())
        preferencesFile = File(context.cacheDir, "overview-${System.nanoTime()}.preferences_pb")
        settings = DataStoreManager(PreferenceDataStoreFactory.create(scope = preferencesScope) { preferencesFile })
        listOf(task(1, "Send the invoice", -1), task(2, "Review the kitchen drawings", 0),
            task(3, "Book the train tickets", 0), task(4, longTitle, 1),
            task(5, "Completed planning", 0).copy(isCompleted = true),
            task(6, "Later this month", 15), task(7, "An undated idea", null)).forEach {
            repository.insertTask(it)
        }
        model = OverviewViewModel(repository, settings, TagRepository(SettingsRepository(settings), repository),
            context.applicationContext as Application)
        store.put("overview", model)
    }

    @After fun cleanUp() {
        store.clear()
        preferencesScope.cancel()
        database.close()
        preferencesFile.delete()
    }

    @Test fun tasksOpenCompleteAndUndoWithoutLosingMetadata() {
        show()
        compose.onNodeWithText("Send the invoice").performClick()
        compose.runOnIdle { assertEquals(1, opened?.id) }
        compose.onNodeWithContentDescription(text(R.string.widget_complete_task, "Send the invoice")).performClick()
        compose.waitUntil(5_000) { savedTask(1).isCompleted }
        compose.onNodeWithText(text(R.string.widget_undo)).performClick()
        compose.waitUntil(5_000) { !savedTask(1).isCompleted }
        assertEquals(listOf("Work", "Personal"), savedTask(1).tags)
        assertEquals("Keep the supporting documents together.", savedTask(1).details)
        assertEquals(870, savedTask(1).dueTimeMinutes)
        compose.onNodeWithContentDescription(text(R.string.action_add_task)).performClick()
        compose.runOnIdle { assertTrue(added) }
    }

    @Test fun allPendingAndCompletedOpenTheRightCollectionsAndCanBeCleared() {
        show()
        openSummary(R.string.title_pending_task)
        compose.onNodeWithText(text(R.string.overview_pending_tasks)).assertIsDisplayed()
        compose.onNodeWithText("6 tasks").assertIsDisplayed()
        compose.onNodeWithTag("overview-filtered-list").performScrollToNode(hasText("An undated idea"))
        compose.onNodeWithText("An undated idea").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.overview_back)).performClick()
        openSummary(R.string.title_completed_task)
        compose.onNodeWithText(text(R.string.overview_completed_tasks)).assertIsDisplayed()
        compose.onNodeWithText("Completed planning").assertIsDisplayed()
        compose.onNodeWithText("1 task").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.overview_show_all_tasks)).performClick()
        compose.onNodeWithText(text(R.string.overview_all_tasks)).assertIsDisplayed()
        compose.onNodeWithText("7 tasks").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.overview_back)).performClick()
        openSummary(R.string.title_all_task)
        compose.onNodeWithText(text(R.string.overview_all_tasks)).assertIsDisplayed()
    }

    @Test fun customLayoutSavesVisibilityOrderSizeAndLocksAndCanReset() {
        show()
        compose.onNodeWithText(text(R.string.overview_edit)).performClick()
        val today = text(R.string.time_today)
        compose.onNodeWithContentDescription("$today: ${text(R.string.overview_compact)}").performClick()
        compose.onNodeWithContentDescription(text(R.string.overview_move_up, today)).performClick()
        compose.onNodeWithContentDescription(text(R.string.overview_lock_card, today)).performClick()
        compose.onNodeWithContentDescription(text(R.string.overview_show_card, today)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(text(R.string.overview_move_down, today)).assertIsNotEnabled()
        val next = text(R.string.overview_next_seven_days)
        compose.onNodeWithTag("overview-layout-editor").performScrollToNode(
            hasContentDescription(text(R.string.overview_show_card, next)))
        compose.onNodeWithContentDescription(text(R.string.overview_show_card, next)).performClick()
        capture("layout-editor")
        compose.onNodeWithText(text(R.string.action_save)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("overview-dashboard").fetchSemanticsNodes().isNotEmpty() }
        val saved = runBlocking { settings.overviewLayoutFlow.first() }
        assertEquals(OverviewCard.Today, saved.cards.first().card)
        assertTrue(saved.cards.first().locked)
        assertEquals(OverviewCardSize.Compact, saved.cards.first().size)
        assertFalse(saved.cards.single { it.card == OverviewCard.Upcoming }.visible)
        compose.onNodeWithContentDescription(text(R.string.widget_complete_task, "Review the kitchen drawings")).assertIsEnabled()

        compose.onNodeWithText(text(R.string.overview_edit)).performClick()
        compose.onNodeWithTag("overview-layout-editor").performScrollToNode(hasText(text(R.string.overview_reset_layout)))
        compose.onNodeWithText(text(R.string.overview_reset_layout)).performClick()
        compose.onNodeWithText(text(R.string.overview_reset)).performClick()
        compose.onNodeWithText(text(R.string.action_save)).performClick()
        compose.waitUntil(5_000) { runBlocking { settings.overviewLayoutFlow.first() } == OverviewLayout() }
    }

    @Test fun cancelledEditsDoNotChangeTheSavedLayout() {
        show()
        compose.onNodeWithText(text(R.string.overview_edit)).performClick()
        compose.onNodeWithContentDescription(text(R.string.overview_show_card, text(R.string.time_today))).performClick()
        compose.onNodeWithContentDescription(text(R.string.overview_back)).performClick()
        compose.onNodeWithText(text(R.string.overview_keep_editing)).performClick()
        compose.onNodeWithContentDescription(text(R.string.overview_show_card, text(R.string.time_today))).assertIsOff()
        compose.onNodeWithContentDescription(text(R.string.overview_back)).performClick()
        compose.onNodeWithText(text(R.string.overview_discard)).performClick()
        assertEquals(OverviewLayout(), runBlocking { settings.overviewLayoutFlow.first() })
    }

    @Test fun draggingAHandleChangesTheSavedOrder() {
        show()
        compose.onNodeWithText(text(R.string.overview_edit)).performClick()
        val origin = compose.onNodeWithTag("layout-Overdue").fetchSemanticsNode().boundsInRoot
        val destination = compose.onNodeWithTag("layout-Today").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription(text(R.string.overview_drag_card, text(R.string.overview_overdue)))
            .performTouchInput {
                down(center)
                advanceEventTime(700)
                moveBy(Offset(0f, destination.center.y - origin.center.y))
                advanceEventTime(100)
                up()
            }
        compose.onNodeWithText(text(R.string.action_save)).performClick()
        compose.waitUntil(5_000) {
            runBlocking { settings.overviewLayoutFlow.first() }.cards.first().card == OverviewCard.Today
        }
    }

    @Test fun layoutSurvivesClosingAndReopeningItsPreferencesFile() = runBlocking {
        val expected = OverviewLayout().move(OverviewCard.Upcoming, OverviewCard.Overdue)
            .configure(OverviewCard.All) { it.copy(visible = false) }
            .configure(OverviewCard.Today) { it.copy(locked = true, size = OverviewCardSize.Compact) }
        settings.setOverviewLayout(expected)
        preferencesScope.coroutineContext.job.cancelAndJoin()
        val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened = DataStoreManager(PreferenceDataStoreFactory.create(scope = reopenedScope) { preferencesFile })
            assertEquals(expected, reopened.overviewLayoutFlow.first())
        } finally {
            reopenedScope.coroutineContext.job.cancelAndJoin()
        }
    }

    @Test fun emptyHiddenDashboardHasARestorePath() {
        runBlocking { settings.setOverviewLayout(OverviewLayout().let {
            it.copy(cards = it.cards.map { card -> card.copy(visible = false) })
        }) }
        show()
        compose.onNodeWithText(text(R.string.overview_empty_layout)).assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.overview_edit)).onFirst().performClick()
        compose.onNodeWithTag("overview-layout-editor").assertIsDisplayed()
    }

    @Test fun capturePhoneLight() { show(); capture("phone-light") }

    @Test fun relativeDatesRecognizeYesterdayAndTomorrowWithTimes() {
        fun millis(days: Long) = LocalDate.now().plusDays(days).atTime(14, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(text(R.string.time_yesterday), millis(-1).toRelativeTimeString(context))
        assertEquals(text(R.string.time_today), millis(0).toRelativeTimeString(context))
        assertEquals(text(R.string.time_tomorrow), millis(1).toRelativeTimeString(context))
    }

    @Test fun capturePhoneDarkLargeText() { show(dark = true, fontScale = 1.3f); capture("phone-dark-large-text") }

    @Test fun captureExpandedLandscape() {
        runBlocking { settings.setOverviewLayout(OverviewLayout().let {
            it.copy(cards = it.cards.map { card -> card.copy(size = OverviewCardSize.Compact) })
        }) }
        show(width = 900, height = 600)
        capture("expanded-landscape")
    }

    private fun show(dark: Boolean = false, fontScale: Float = 1f, width: Int? = null, height: Int? = null) {
        compose.setContent {
            val deviceDensity = LocalDensity.current
            val density = if (width != null) context.resources.displayMetrics.widthPixels.toFloat() / width
                else deviceDensity.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                VerveDoTheme(darkTheme = dark, dynamicColor = false, animate = false) {
                    Box(Modifier.fillMaxWidth().then(if (height != null) Modifier.height(height.dp) else Modifier)
                        .testTag("overview-test-surface")) {
                        OverviewPage(toTaskAddPage = { added = true }, toTaskEditPage = { opened = it }, viewModel = model)
                    }
                }
            }
        }
        compose.waitUntil(10_000) { !model.uiState.value.isLoading }
        compose.waitForIdle()
    }

    private fun openSummary(resource: Int) {
        // Scroll the summary row above the floating Add button before tapping it.
        compose.onNodeWithTag("overview-dashboard").performScrollToIndex(OverviewLayout().cards.lastIndex)
        compose.onNodeWithText(text(resource)).performClick()
        compose.runOnIdle { assertFalse(added) }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "overview-review/$name.png")
        file.parentFile!!.mkdirs()
        compose.onNodeWithTag("overview-test-surface").captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun savedTask(id: Int) = runBlocking { repository.getAllTasks().first().single { it.id == id } }
    private fun text(resource: Int, vararg args: Any): String = context.getString(resource, *args)
    private fun task(id: Int, title: String, days: Long?) = TaskEntity(id = id, content = title,
        details = "Keep the supporting documents together.", tags = listOf("Work", "Personal"),
        category = "Work", priority = 0f, dueTimeMinutes = if (days == null) null else 870,
        dueDateMillis = days?.let { LocalDate.now().plusDays(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() })
}
