package cn.super12138.todo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.super12138.todo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueTimeDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_due_time)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { TimePicker(state = state) } },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
            Text(stringResource(R.string.action_confirm))
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
