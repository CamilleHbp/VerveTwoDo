package cn.super12138.todo.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TagTextFieldTest {
    @get:Rule val compose = createComposeRule()

    @Test fun focusShowsExistingTagsAndTypingFiltersWithoutLosingFocus() {
        var value by mutableStateOf("")
        compose.setContent {
            MaterialTheme {
                TagTextField(value, { value = it }, listOf("Home", "Homework", "Work"), "Tag")
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.performClick()
        compose.onNodeWithText("Home", useUnmergedTree = true).assertExists()
        field.performTextReplacement("WO")
        compose.onNodeWithText("Home", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Homework", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Work", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals("Work", value) }
        compose.onNode(hasSetTextAction() and hasText("Work")).assertExists()
    }

    @Test fun unmatchedNewTagCanBeEnteredAndCleared() {
        var value by mutableStateOf("")
        var submitted = ""
        compose.setContent {
            MaterialTheme {
                TagTextField(value, { value = it }, listOf("Work"), "Tag", onDone = { submitted = value })
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.performTextReplacement("New tag")
        compose.onNodeWithText("Work", useUnmergedTree = true).assertDoesNotExist()
        field.performImeAction()
        compose.runOnIdle { assertEquals("New tag", submitted) }
        field.performTextReplacement("")
        compose.runOnIdle { assertEquals("", value) }
    }

    @Test fun tagsLoadedWhileTypingUpdateSuggestionsWithoutReplacingDraft() {
        var value by mutableStateOf("")
        var tags by mutableStateOf(emptyList<String>())
        compose.setContent {
            MaterialTheme { TagTextField(value, { value = it }, tags, "Tag") }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("wo")
        compose.runOnIdle { tags = listOf("Work") }
        compose.onNodeWithText("Work", useUnmergedTree = true).assertExists()
        compose.runOnIdle { assertEquals("wo", value) }
    }
}
