package cn.super12138.todo.logic.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cn.super12138.todo.R

enum class Priority(
    val value: Float,
    @param:StringRes val nameRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    NotUrgent(value = -2f, nameRes = R.string.priority_not_urgent, iconRes = R.drawable.ic_priority_lowest),
    NotImportant(value = -1f, nameRes = R.string.priority_not_important, iconRes = R.drawable.ic_priority_low),
    Default(value = 0f, nameRes = R.string.priority_default, iconRes = R.drawable.ic_priority_default),
    Important(value = 1f, nameRes = R.string.priority_important, iconRes = R.drawable.ic_priority_high),
    Urgent(value = 2f, nameRes = R.string.priority_urgent, iconRes = R.drawable.ic_priority_highest);

    companion object {
        fun fromFloat(float: Float) = entries.find { it.value == float } ?: Default
    }
}
