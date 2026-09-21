package cn.super12138.todo.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import cn.super12138.todo.R
import cn.super12138.todo.ui.pages.settings.components.category.CategoryPromptDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryPromptDialogTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun dialogSuggestsTagsAndKeepsTheDraftThroughRecreation() {
        val restoration = StateRestorationTester(compose)
        var saved: String? = null
        restoration.setContent {
            MaterialTheme {
                CategoryPromptDialog(
                    visible = true,
                    suggestedTags = listOf("Home", "Work"),
                    onSave = { saved = it },
                    onDismiss = {}
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("wo")
        compose.onNodeWithText("Work", useUnmergedTree = true).performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction() and hasText("Work")).assertExists()
        compose.onNodeWithText(context.getString(R.string.action_save)).performClick()
        compose.runOnIdle { assertEquals("Work", saved) }
    }

    @Test fun blankPresetIsRejectedAndReopeningStartsFresh() {
        var visible by mutableStateOf(true)
        var saved: String? = null
        compose.setContent {
            MaterialTheme {
                CategoryPromptDialog(
                    visible = visible,
                    onSave = { saved = it },
                    onDismiss = { visible = false }
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("   ")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText(context.getString(R.string.error_no_content_entered)).assertExists()
        compose.runOnIdle { assertEquals(null, saved) }
        compose.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        compose.runOnIdle { visible = true }
        compose.onNodeWithText(context.getString(R.string.error_no_content_entered)).assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("   ")).assertDoesNotExist()
    }
}
