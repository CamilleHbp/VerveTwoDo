package cn.super12138.todo.ui.pages.settings.components.category

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cn.super12138.todo.R
import cn.super12138.todo.ui.components.BasicDialog
import cn.super12138.todo.ui.components.TagTextField
import cn.super12138.todo.ui.components.TagColorPicker
import cn.super12138.todo.ui.components.parseTagColor
import cn.super12138.todo.ui.components.tagColorHex

@Composable
fun CategoryPromptDialog(
    modifier: Modifier = Modifier,
    visible: Boolean,
    initialCategory: String = "",
    suggestedTags: List<String> = emptyList(),
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    initialColor: Int? = null,
    onSaveColor: ((String, Int) -> Unit)? = null
) {
    if (!visible) return
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    var validate by rememberSaveable { mutableStateOf(false) }
    var colorHex by rememberSaveable(initialCategory, initialColor) {
        mutableStateOf(tagColorHex(initialColor ?: cn.super12138.todo.logic.tagColorPalette.first()))
    }

    fun save() {
        validate = true
        if (category.isBlank()) return
        if (onSaveColor != null) {
            val color = parseTagColor(colorHex) ?: return
            onSaveColor(category, color)
        } else onSave(category)
        onDismiss()
    }

    BasicDialog(
        visible = true,
        painter = painterResource(R.drawable.ic_category),
        title = stringResource(R.string.pref_category_category_management),
        text = {
            Column {
            TagTextField(
                value = category,
                onValueChange = { category = it },
                tags = suggestedTags,
                label = stringResource(R.string.tip_enter_category),
                isError = validate && category.isBlank(),
                onDone = if (onSaveColor == null) ::save else null,
                modifier = Modifier.fillMaxWidth()
            )
            if (onSaveColor != null) TagColorPicker(colorHex) { colorHex = it }
            }
        },
        confirmButton = stringResource(R.string.action_save),
        dismissButton = stringResource(R.string.action_cancel),
        onConfirm = ::save,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
