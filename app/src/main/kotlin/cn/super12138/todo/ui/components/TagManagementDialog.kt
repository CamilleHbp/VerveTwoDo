package cn.super12138.todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.super12138.todo.R
import cn.super12138.todo.logic.nextTagColor
import cn.super12138.todo.ui.pages.settings.SettingsDataCategoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Empty tag opens creation; a non-empty tag edits the shared identity. */
@Composable
fun TagManagementDialog(
    tag: String,
    onDismiss: () -> Unit,
    initialColor: Int? = null,
    viewModel: SettingsDataCategoryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable(tag) { mutableStateOf(tag) }
    var colorHex by rememberSaveable(tag) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable(tag) { mutableStateOf(false) }
    LaunchedEffect(tag) { viewModel.clearError() }
    val color = colorHex ?: tagColorHex(initialColor ?: state.tagColors[tag]
        ?: nextTagColor(state.tagColors.values.toSet()))
    val targetName = if (name == tag) name else name.trim()
    val duplicate = targetName != tag && state.categories.any {
        it != tag && it.trim().equals(targetName, ignoreCase = true)
    }
    val count = state.usageCounts[tag] ?: 0
    val dismiss = { if (!state.isSaving) onDismiss() }
    val properties = DialogProperties(dismissOnBackPress = !state.isSaving, dismissOnClickOutside = !state.isSaving)

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.isSaving) confirmDelete = false },
            icon = { Icon(painterResource(R.drawable.ic_delete), null) },
            title = { Text(stringResource(R.string.tag_delete_title, tag)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (count == 0) stringResource(R.string.tag_delete_unused)
                        else pluralStringResource(R.plurals.tag_delete_message, count, count))
                    state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTag(tag, onDismiss) }, enabled = !state.isSaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.tag_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.clearError() }, enabled = !state.isSaving) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            properties = properties
        )
        return
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(if (tag.isEmpty()) R.string.tag_create else R.string.tag_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tag.isNotEmpty()) Text(stringResource(R.string.tag_edit_scope),
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name, onValueChange = { name = it; viewModel.clearError() },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true, enabled = !state.isSaving, isError = duplicate,
                    supportingText = if (duplicate) ({ Text(stringResource(R.string.tag_name_exists)) }) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                TagColorPicker(color, onChange = { colorHex = it; viewModel.clearError() }, enabled = !state.isSaving)
                state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                if (state.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (tag.isNotEmpty()) {
                    HorizontalDivider()
                    TextButton(onClick = { confirmDelete = true; viewModel.clearError() }, enabled = !state.isSaving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tag_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.saveTag(tag.takeIf { it.isNotEmpty() }, name, parseTagColor(color)!!, onDismiss) },
                enabled = !state.isSaving && name.isNotBlank() && !duplicate && parseTagColor(color) != null
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = dismiss, enabled = !state.isSaving) { Text(stringResource(R.string.action_cancel)) }
        },
        properties = properties
    )
}
