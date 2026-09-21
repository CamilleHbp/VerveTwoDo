package cn.super12138.todo.ui.pages.overview

import cn.super12138.todo.logic.isDueOn
import java.time.LocalDate
import java.time.ZoneId

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.super12138.todo.logic.TaskRepository
import cn.super12138.todo.logic.database.TaskEntity
import cn.super12138.todo.utils.SystemUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OverviewViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    val uiState: StateFlow<OverviewUiState> = taskRepository.getAllTasks()
        .map {
            val total = it.size
            val completed = it.count { task -> task.isCompleted }
            val pending = total - completed

            val today = LocalDate.now()
            val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val weekEnd = today.plusDays(8).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val todayTasks = it.filter { task ->
                val due = task.dueDateMillis ?: return@filter false // 如果截止日期为空立即返回null
                isDueOn(due, today)
            }

            val nextWeekTasks = it.filter { task -> // 先过滤
                val due = task.dueDateMillis ?: return@filter false
                // 截止日期是否在今天到一周之后并且未完成
                due >= todayMillis && due < weekEnd && !task.isCompleted
            }.sortedWith( // 后排序
                comparator = compareBy<TaskEntity> { it.dueDateMillis } // 截止日期近的靠前
                    .thenBy { it.category } // TODO：可选删了
                    .thenByDescending { it.priority } // 优先级高的靠前
            )

            OverviewUiState(
                totalTasks = total,
                completedTasks = completed,
                pendingTasks = pending,
                todayTasks = todayTasks,
                nextWeekTasks = nextWeekTasks
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverviewUiState()
        )
}