package cn.super12138.todo

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.test.platform.app.InstrumentationRegistry
import cn.super12138.todo.logic.*
import cn.super12138.todo.logic.database.*
import cn.super12138.todo.logic.datastore.DataStoreManager
import cn.super12138.todo.ui.components.TagManagementDialog
import cn.super12138.todo.ui.components.EditableTag
import cn.super12138.todo.ui.pages.editor.EditorViewModel
import cn.super12138.todo.ui.pages.settings.SettingsDataCategory
import cn.super12138.todo.ui.pages.settings.SettingsDataCategoryViewModel
import cn.super12138.todo.ui.theme.VerveDoTheme
import cn.super12138.todo.ui.widget.upcoming.UpcomingWidgetPreferences
import cn.super12138.todo.utils.ConfettiController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File

class TagManagementTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var database: TaskDatabase
    private lateinit var repository: TaskRepository
    private lateinit var settings: SettingsRepository
    private lateinit var tags: TagRepository
    private lateinit var model: SettingsDataCategoryViewModel
    private val store = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferences: File
    private val purple = 0xFF7953A8.toInt()
    private val sample = TaskEntity("Prepare the garden", category = "Home", tags = listOf("Home", "Garden"),
        priority = 1f, id = 1, details = "Keep the herbs near the path.", dueDateMillis = 1234L,
        createdAtMillis = 123L, dueTimeMinutes = 870, subtasks = listOf(Subtask("Choose seeds", id = "seeds")))

    @Before fun prepare() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        repository = TaskRepository(database.taskDao())
        preferences = File(context.cacheDir, "tags-${System.nanoTime()}.preferences_pb")
        settings = SettingsRepository(DataStoreManager(PreferenceDataStoreFactory.create(scope = scope) { preferences }))
        tags = TagRepository(settings, repository)
        settings.setCategories(listOf("Home", "Garden", "Unused"))
        settings.setTagColor("Home", purple)
        repository.insertTask(sample)
        repository.insertTask(TaskEntity("Completed legacy task", category = "Home", isCompleted = true, priority = 0f, id = 2))
        model = SettingsDataCategoryViewModel(tags, context.applicationContext as Application, refreshWidgets = { _, _ -> })
        store.put("tags", model)
    }

    @After fun cleanUp() {
        store.clear()
        scope.cancel()
        database.close()
        preferences.delete()
    }

    @Test fun renameAndDeletePreserveTasksAndUnrelatedMetadata() = runBlocking {
        tags.save("Home", "  Household  ", purple)
        assertEquals(sample.copy(category = "Household", tags = listOf("Household", "Garden")), saved(1))
        assertEquals(listOf("Household"), saved(2).taskTags)
        assertTrue(saved(2).isCompleted)
        assertFalse("Home" in tags.tags.first())
        assertEquals(purple, settings.tagColorsFlow.first()["Household"])
        assertFalse("Home" in settings.tagColorsFlow.first())
        tags.delete("Household")
        assertEquals(sample.copy(category = "Garden", tags = listOf("Garden")), saved(1))
        assertEquals("", saved(2).category)
        assertTrue(saved(2).taskTags.isEmpty())
        assertEquals(2, repository.getAllTasks().first().size)
        tags.delete("Garden")
        assertTrue(saved(1).taskTags.isEmpty())
        assertEquals("", saved(1).category)
        assertFalse("Garden" in tags.tags.first())
    }

    @Test fun rejectsDuplicatesAllowsCaseChangeAndRemovesUnusedTags() = runBlocking {
        try { tags.save("Home", " garden ", purple); fail("Expected duplicate rejection") }
        catch (_: DuplicateTagException) { }
        assertEquals(sample, saved(1))
        tags.save("Home", "HOME", purple)
        assertEquals(listOf("HOME", "Garden"), saved(1).taskTags)
        tags.delete("Unused")
        assertFalse("Unused" in tags.tags.first())
    }

    @Test fun widgetFiltersFollowRenamesAndDropDeletedTags() {
        val preferences = mutablePreferencesOf(UpcomingWidgetPreferences.categoryKey to "Home")
        UpcomingWidgetPreferences.replaceTag(preferences, "Home", "Household")
        assertEquals(setOf("Household"), UpcomingWidgetPreferences.categories(preferences))
        assertNull(preferences[UpcomingWidgetPreferences.categoryKey])
        UpcomingWidgetPreferences.setCategories(preferences, setOf("Household", "Garden"))
        UpcomingWidgetPreferences.replaceTag(preferences, "Household", null)
        assertEquals(setOf("Garden"), UpcomingWidgetPreferences.categories(preferences))
        UpcomingWidgetPreferences.replaceTag(preferences, "Garden", null)
        assertTrue(UpcomingWidgetPreferences.categories(preferences).isEmpty())
    }

    @Test fun colorOnlyEditsPreserveLegacyCaseAndWhitespaceIdentities() = runBlocking {
        settings.setCategories(listOf("Home", "home", " Home "))
        tags.save("Home", "Home", purple)
        tags.save(" Home ", " Home ", purple)
        assertTrue(tags.tags.first().containsAll(listOf("Home", "home", " Home ")))
        assertEquals(sample, saved(1))
        tags.delete("home")
        assertEquals(sample, saved(1))
    }

    @Test fun openEditorDraftFollowsGlobalChangesWithoutLosingOtherEdits() {
        lateinit var editor: EditorViewModel
        instrumentation.runOnMainSync {
            editor = EditorViewModel(sample, repository, settings, tags, ConfettiController())
            editor.setContentText("A draft I have not saved")
            store.put("editor", editor)
        }
        val collect = scope.launch { editor.uiState.collect {} }
        runBlocking { tags.save("Home", "Household", purple); tags.delete("Garden") }
        compose.waitUntil(5_000) { editor.uiState.value.tags == listOf("Household") }
        assertEquals("A draft I have not saved", editor.uiState.value.content)
        instrumentation.runOnMainSync { editor.saveNewTask() }
        compose.waitUntil(5_000) { runBlocking { saved(1).content == "A draft I have not saved" } }
        assertEquals(listOf("Household"), runBlocking { saved(1).taskTags })
        collect.cancel()
    }

    @Test fun editValidatesNameAndColorAndKeepsDraftOnRecreation() {
        var visible by mutableStateOf(true)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { VerveDoTheme(dynamicColor = false, animate = false) {
            if (visible) TagManagementDialog("Home", { visible = false }, viewModel = model)
        } }
        compose.waitUntil(5_000) { model.uiState.value.categories.isNotEmpty() }
        capture("edit-light")
        val name = compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.tag_name)))
        name.performTextReplacement(" Garden ")
        compose.onNodeWithText(context.getString(R.string.tag_name_exists)).assertExists()
        compose.onNodeWithText(context.getString(R.string.action_save)).assertIsNotEnabled()
        name.performTextReplacement("Household")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction() and hasText("Household")).assertExists()
        val color = compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.tag_color)))
        for (invalid in listOf("#invalid", "#+12345", "#-00001")) {
            color.performScrollTo().performTextReplacement(invalid)
            compose.onNodeWithText(context.getString(R.string.action_save)).assertIsNotEnabled()
        }
        color.performTextReplacement("#7953A8")
        compose.onNodeWithText(context.getString(R.string.action_save)).performClick()
        compose.waitUntil(5_000) { !visible }
        assertEquals(listOf("Household", "Garden"), runBlocking { saved(1).taskTags })
    }

    @Test fun managerShowsCountsAndDeletionRequiresConfirmation() {
        compose.setContent { VerveDoTheme(dynamicColor = false, animate = false) {
            SystemBars(false)
            SettingsDataCategory(onNavigateUp = {}, viewModel = model)
        } }
        compose.waitUntil(5_000) { model.uiState.value.categories.size == 3 }
        capture("manager-light")
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText(context.getString(R.string.tag_delete)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.tag_delete_title, "Home")).assertExists()
        capture("delete")
        compose.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        assertEquals(2, runBlocking { tags.usageCounts.first()["Home"] })
        compose.onNodeWithText(context.getString(R.string.tag_delete)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.tag_delete)).performClick()
        compose.waitUntil(5_000) { "Home" !in model.uiState.value.categories }
        assertEquals(2, runBlocking { repository.getAllTasks().first().size })
    }

    @Test fun darkLargeTextKeepsDialogActionsReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                VerveDoTheme(darkTheme = true, dynamicColor = false, animate = false) {
                    TagManagementDialog("Home", {}, viewModel = model)
                }
            }
        }
        compose.waitUntil(5_000) { model.uiState.value.categories.isNotEmpty() }
        capture("edit-dark-large")
        compose.onNodeWithText(context.getString(R.string.tag_delete)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.action_save)).assertIsDisplayed()
    }

    @Test fun tapAndLongPressOpenTagEditor() {
        var editing by mutableStateOf(false)
        compose.setContent { VerveDoTheme(dynamicColor = false, animate = false) {
            EditableTag("Home", purple, onEdit = { editing = true })
            if (editing) TagManagementDialog("Home", { editing = false }, viewModel = model)
        } }
        compose.onNodeWithText("Home").performTouchInput { longClick() }
        compose.onNodeWithText(context.getString(R.string.tag_edit)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText(context.getString(R.string.tag_edit)).assertIsDisplayed()
        assertEquals(sample, runBlocking { saved(1) })
    }

    @Test fun colorWheelAndBrightnessSaveTheChosenColor() {
        var visible by mutableStateOf(true)
        compose.setContent { VerveDoTheme(dynamicColor = false, animate = false) {
            if (visible) TagManagementDialog("Home", { visible = false }, initialColor = purple, viewModel = model)
        } }
        compose.waitUntil(5_000) { model.uiState.value.categories.isNotEmpty() }
        val wheel = compose.onNodeWithContentDescription(context.getString(R.string.tag_color_wheel))
        wheel.performScrollTo().performTouchInput { click(Offset(width * 0.8f, height * 0.5f)) }
        compose.onNodeWithContentDescription(context.getString(R.string.tag_color_brightness))
            .performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        compose.waitForIdle()
        val field = compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.tag_color)))
        val hex = field.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text
        val color = cn.super12138.todo.ui.components.parseTagColor(hex)!!
        assertNotEquals(purple, color)
        assertTrue(maxOf((color shr 16) and 255, (color shr 8) and 255, color and 255) in 126..129)
        compose.onNodeWithText(context.getString(R.string.action_save)).performClick()
        compose.waitUntil(5_000) { !visible }
        assertEquals(color, runBlocking { settings.tagColorsFlow.first()["Home"] })
        assertEquals(sample, runBlocking { saved(1) })
    }

    private suspend fun saved(id: Int) = repository.observeTask(id).first()!!

    @Composable
    private fun SystemBars(dark: Boolean) {
        val window = androidx.activity.compose.LocalActivity.current!!.window
        val view = androidx.compose.ui.platform.LocalView.current
        SideEffect { androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark }
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureTags") != "true") return
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(400)
        val prefix = InstrumentationRegistry.getArguments().getString("capturePrefix") ?: "phone"
        val file = File(context.getExternalFilesDir(null), "tag-review/$prefix-$name.png")
        file.parentFile!!.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
