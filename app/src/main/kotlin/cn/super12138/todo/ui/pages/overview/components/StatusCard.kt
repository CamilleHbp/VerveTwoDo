package cn.super12138.todo.ui.pages.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import cn.super12138.todo.ui.VerveDoDefaults
import cn.super12138.todo.utils.VibrationUtils

@Composable
fun OverviewStatusCard(title: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    Surface(onClick = {
        VibrationUtils.performHapticFeedback(view)
        onClick()
    }, modifier = modifier, shape = VerveDoDefaults.defaultShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
        Column(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium)
        }
    }
}
