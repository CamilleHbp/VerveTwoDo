package cn.super12138.todo.ui.pages.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.super12138.todo.logic.nextTagColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.padding
import cn.super12138.todo.ui.components.TagManagementDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.super12138.todo.R
import cn.super12138.todo.ui.components.TodoFloatingActionButton
import cn.super12138.todo.ui.components.TopAppBarScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsDataCategory(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsDataCategoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingTag by rememberSaveable { mutableStateOf<String?>(null) }

    TopAppBarScaffold(
        title = stringResource(R.string.tag_manage),
        onBack = onNavigateUp,
        floatingActionButton = {
            TodoFloatingActionButton(
                iconRes = R.drawable.ic_add,
                text = stringResource(R.string.tag_create),
                expanded = true,
                onClick = { editingTag = "" }
            )
        },
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp)) {
                item {
                    Text(stringResource(R.string.tag_manage_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 20.dp))
                }
                if (uiState.categories.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.tag_empty), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.tag_empty_description), textAlign = TextAlign.Center)
                    }
                }
                itemsIndexed(uiState.categories, key = { _, tag -> tag }) { index, tag ->
                    val top = if (index == 0) 16.dp else 0.dp
                    val bottom = if (index == uiState.categories.lastIndex) 16.dp else 0.dp
                    Surface(
                        onClick = { editingTag = tag },
                        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(Modifier.size(14.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .background(Color(uiState.tagColors[tag] ?: nextTagColor(emptySet())), CircleShape))
                                Text(tag, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val count = uiState.usageCounts[tag] ?: 0
                                Text(if (count == 0) stringResource(R.string.tag_no_tasks)
                                    else pluralStringResource(R.plurals.tag_task_count, count, count),
                                    modifier = Modifier.widthIn(max = 104.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End)
                                Icon(painterResource(R.drawable.ic_edit_task), null, Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (index != uiState.categories.lastIndex) HorizontalDivider(
                                Modifier.padding(start = 46.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
    editingTag?.let { tag ->
        TagManagementDialog(tag = tag, onDismiss = { editingTag = null }, viewModel = viewModel)
    }
}
