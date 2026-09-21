package cn.super12138.todo.ui.widget.upcoming

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.utils.updateTaskWidgets
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class UpcomingControlsAction : ActionCallback {
    companion object {
        val commandKey = ActionParameters.Key<String>("upcoming_command")
        val valueKey = ActionParameters.Key<String>("upcoming_value")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { preferences ->
            with(UpcomingWidgetPreferences) {
                when (parameters[commandKey]) {
                    "controls" -> {
                        preferences[controlsKey] = !(preferences[controlsKey] ?: true)
                        preferences.remove(panelKey)
                    }
                    "panel" -> preferences[panelKey] = parameters[valueKey].orEmpty()
                    "sort" -> {
                        val sort = UpcomingTaskSort.entries.find { it.name == parameters[valueKey] }
                        if (sort != null) preferences[sortKey] = sort.name
                        preferences.remove(panelKey)
                    }
                    "sort_field" -> {
                        UpcomingSortField.entries.find { it.name == parameters[valueKey] }?.let {
                            preferences[sortKey] = sort(preferences).withField(it).name
                        }
                    }
                    "sort_direction" -> preferences[sortKey] = sort(preferences).reverseDateOrder().name
                    "all_tags" -> setCategories(preferences, emptySet())
                    "tag" -> {
                        val tag = parameters[valueKey] ?: return@updateAppWidgetState
                        val selected = categories(preferences)
                        setCategories(preferences, if (tag in selected) selected - tag else selected + tag)
                    }
                }
            }
        }
        UpcomingTaskWidget().update(context, glanceId)
    }
}

class CompleteUpcomingTaskAction : ActionCallback, KoinComponent {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[taskIdKey] ?: return
        val repository = get<TaskRepository>()
        val task = repository.getAllTasks().first().find { it.id == taskId && !it.isCompleted } ?: return
        repository.completeTask(taskId)
        updateAppWidgetState(context, glanceId) {
            it[UpcomingWidgetPreferences.undoIdKey] = taskId
            it[UpcomingWidgetPreferences.undoTitleKey] = task.content
            it[UpcomingWidgetPreferences.undoUntilKey] = System.currentTimeMillis() + 30_000L
        }
        updateTaskWidgets(context)
    }

    companion object {
        val taskIdKey = ActionParameters.Key<Int>("upcoming_task_id")
    }
}

class RestoreUpcomingTaskAction : ActionCallback, KoinComponent {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[CompleteUpcomingTaskAction.taskIdKey] ?: return
        get<TaskRepository>().restoreTask(taskId)
        updateAppWidgetState(context, glanceId) {
            if (it[UpcomingWidgetPreferences.undoIdKey] == taskId) UpcomingWidgetPreferences.clearUndo(it)
        }
        updateTaskWidgets(context)
    }
}

class UndoUpcomingTaskAction : ActionCallback, KoinComponent {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val taskId = UpcomingWidgetPreferences.undoTaskId(preferences) ?: return
        get<TaskRepository>().restoreTask(taskId)
        updateAppWidgetState(context, glanceId) {
            if (it[UpcomingWidgetPreferences.undoUntilKey] == preferences[UpcomingWidgetPreferences.undoUntilKey]) {
                UpcomingWidgetPreferences.clearUndo(it)
            }
        }
        updateTaskWidgets(context)
    }
}
