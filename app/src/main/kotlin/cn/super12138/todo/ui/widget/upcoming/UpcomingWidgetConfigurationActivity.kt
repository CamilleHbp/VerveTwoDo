package cn.super12138.todo.ui.widget.upcoming

import cn.super12138.todo.logic.database.taskTags

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cn.super12138.todo.R
import cn.super12138.todo.logic.SettingsRepository
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.ui.theme.VerveDoTheme
import cn.super12138.todo.utils.configureEdgeToEdge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

class UpcomingWidgetConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_CANCELED, result)
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ||
            provider != ComponentName(this, UpcomingTaskWidgetReceiver::class.java)
        ) {
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val glanceId = GlanceAppWidgetManager(this@UpcomingWidgetConfigurationActivity)
                    .getGlanceIdBy(widgetId)
                val preferences = getAppWidgetState(
                    this@UpcomingWidgetConfigurationActivity,
                    PreferencesGlanceStateDefinition,
                    glanceId
                )
                val initialSort = UpcomingWidgetPreferences.sort(preferences)
                val initialCategories = UpcomingWidgetPreferences.categories(preferences)
                val settingsRepository: SettingsRepository = get()
                val taskRepository: TaskRepository = get()
                val tasksFlow = taskRepository.getAllTasks()

                setContent {
                    val categories by settingsRepository.categoriesFlow.collectAsStateWithLifecycle(emptyList())
                    val tasks by tasksFlow.collectAsStateWithLifecycle(emptyList())
                    val tags = remember(categories, tasks, initialCategories) {
                        (categories + tasks.flatMap { it.taskTags } + initialCategories)
                            .filter { it.isNotBlank() }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
                    }
                    var saving by remember { mutableStateOf(false) }

                    VerveDoTheme {
                        WidgetConfiguration(
                            initialSort = initialSort,
                            initialCategories = initialCategories,
                            tags = tags,
                            saving = saving,
                            onCancel = { finish() },
                            onSave = { sort, category ->
                                saving = true
                                lifecycleScope.launch {
                                    try {
                                        updateAppWidgetState(this@UpcomingWidgetConfigurationActivity, glanceId) {
                                            it[UpcomingWidgetPreferences.sortKey] = sort.name
                                            UpcomingWidgetPreferences.setCategories(it, category)
                                            it.remove(UpcomingWidgetPreferences.panelKey)
                                        }
                                        UpcomingTaskWidget().update(this@UpcomingWidgetConfigurationActivity, glanceId)
                                        setResult(RESULT_OK, result)
                                        finish()
                                    } catch (exception: CancellationException) {
                                        throw exception
                                    } catch (exception: Exception) {
                                        showConfigurationError(exception)
                                        saving = false
                                    }
                                }
                            }
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                showConfigurationError(exception)
                finish()
            }
        }
    }

    private fun showConfigurationError(exception: Exception) {
        Log.e("UpcomingWidget", "Unable to configure widget", exception)
        Toast.makeText(this, R.string.widget_settings_error, Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun WidgetConfiguration(
    initialSort: UpcomingTaskSort,
    initialCategories: Set<String>,
    tags: List<String>,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: (UpcomingTaskSort, Set<String>) -> Unit
) {
    var sort by rememberSaveable { mutableStateOf(initialSort) }
    var category by rememberSaveable { mutableStateOf<List<String>>(initialCategories.toList()) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
            ) {
                TextButton(onClick = onCancel, enabled = !saving) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = { onSave(sort, category.toSet()) }, enabled = !saving) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.widget_settings),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
                Text(
                    text = stringResource(R.string.widget_upcoming_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.pref_sorting_method),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                Column(Modifier.selectableGroup()) {
                    UpcomingTaskSort.entries.forEach { option ->
                        ConfigurationOption(
                            label = stringResource(option.labelRes()),
                            selected = sort == option,
                            enabled = !saving,
                            onClick = { sort = option }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.widget_filter_tag),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.widget_tags_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                TagOption(stringResource(R.string.widget_all_tags), category.isEmpty(), !saving) {
                    category = emptyList()
                }
                TagOption(stringResource(R.string.widget_no_tag), "" in category, !saving) {
                    category = if ("" in category) category - "" else category + ""
                }
            }
            items(tags, key = { it }) { tag ->
                TagOption(tag, tag in category, !saving) {
                    category = if (tag in category) category - tag else category + tag
                }
            }
        }
    }
}

@Composable
private fun ConfigurationOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        RadioButton(selected = selected, enabled = enabled, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun TagOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
            .selectable(selected, enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(vertical = 8.dp)) {
        Checkbox(checked = selected, enabled = enabled, onCheckedChange = null)
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
