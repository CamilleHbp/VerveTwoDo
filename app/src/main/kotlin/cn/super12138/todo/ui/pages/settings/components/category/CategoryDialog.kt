package cn.super12138.todo.ui.pages.settings.components.category

import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun CategoryPromptDialog(
    modifier: Modifier = Modifier,
    visible: Boolean,
    initialCategory: String = "",
    suggestedTags: List<String> = emptyList(),
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    var validate by rememberSaveable { mutableStateOf(false) }

    fun save() {
        validate = true
        if (category.isBlank()) return
        onSave(category)
        onDismiss()
    }

    BasicDialog(
        visible = true,
        painter = painterResource(R.drawable.ic_category),
        title = stringResource(R.string.pref_category_category_management),
        text = {
            TagTextField(
                value = category,
                onValueChange = { category = it },
                tags = suggestedTags,
                label = stringResource(R.string.tip_enter_category),
                isError = validate && category.isBlank(),
                onDone = ::save,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = stringResource(R.string.action_save),
        dismissButton = stringResource(R.string.action_cancel),
        onConfirm = ::save,
        onDismiss = onDismiss,
        modifier = modifier
    )
}