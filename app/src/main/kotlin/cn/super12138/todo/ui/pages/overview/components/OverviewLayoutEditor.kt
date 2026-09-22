package cn.super12138.todo.ui.pages.overview.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.super12138.todo.R
import cn.super12138.todo.logic.model.OverviewCard
import cn.super12138.todo.logic.model.OverviewCardSize
import cn.super12138.todo.logic.model.OverviewLayout
import cn.super12138.todo.ui.VerveDoDefaults
import kotlinx.coroutines.delay

@Composable
fun OverviewLayoutEditor(
    layout: OverviewLayout,
    enabled: Boolean,
    onChange: (OverviewLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val currentLayout by rememberUpdatedState(layout)
    val changeLayout by rememberUpdatedState(onChange)
    var dragged by remember { mutableStateOf<OverviewCard?>(null) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var resetDialog by remember { mutableStateOf(false) }
    val edge = with(LocalDensity.current) { 56.dp.toPx() }

    fun reorderAtPointer() {
        val card = dragged ?: return
        val target = state.layoutInfo.visibleItemsInfo.firstOrNull {
            pointerY >= it.offset && pointerY < it.offset + it.size
        }?.key as? String ?: return
        val targetCard = OverviewCard.entries.firstOrNull { it.name == target } ?: return
        if (card != targetCard) changeLayout(currentLayout.move(card, targetCard))
    }

    // Continue scrolling when a held card reaches an edge, without requiring more pointer movement.
    LaunchedEffect(dragged) {
        while (dragged != null) {
            val info = state.layoutInfo
            val delta = when {
                pointerY < info.viewportStartOffset + edge -> -12f
                pointerY > info.viewportEndOffset - edge -> 12f
                else -> 0f
            }
            if (delta != 0f) {
                state.scrollBy(delta)
                reorderAtPointer()
            }
            delay(32)
        }
    }

    LazyColumn(modifier.fillMaxSize().testTag("overview-layout-editor"), state = state,
        verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.overview_edit_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        items(layout.cards, key = { it.card.name }) { config ->
            val title = overviewCardTitle(config.card)
            val up = stringResource(R.string.overview_move_up, title)
            val down = stringResource(R.string.overview_move_down, title)
            val canMoveUp = enabled && layout.neighbor(config.card, -1) != null
            val canMoveDown = enabled && layout.neighbor(config.card, 1) != null
            val move: (Int) -> Unit = { direction ->
                currentLayout.neighbor(config.card, direction)?.let {
                    changeLayout(currentLayout.move(config.card, it))
                }
            }
            Surface(modifier = Modifier.fillMaxWidth().animateItem().testTag("layout-${config.card.name}"),
                color = if (dragged == config.card) MaterialTheme.colorScheme.secondaryContainer
                    else VerveDoDefaults.Colors.Container,
                shape = VerveDoDefaults.defaultShape) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dragLabel = stringResource(R.string.overview_drag_card, title)
                        Box(Modifier.size(48.dp).semantics {
                            contentDescription = dragLabel
                            customActions = buildList {
                                if (canMoveUp) add(CustomAccessibilityAction(up) { move(-1); true })
                                if (canMoveDown) add(CustomAccessibilityAction(down) { move(1); true })
                            }
                        }.pointerInput(config.card, config.locked, enabled) {
                            if (!config.locked && enabled) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragged = config.card
                                        val item = state.layoutInfo.visibleItemsInfo.firstOrNull {
                                            it.key == config.card.name
                                        }
                                        pointerY = item?.let { it.offset + it.size / 2f } ?: 0f
                                    },
                                    onDragEnd = { dragged = null },
                                    onDragCancel = { dragged = null },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        pointerY += amount.y
                                        reorderAtPointer()
                                    }
                                )
                            }
                        }, contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_overview_drag), null,
                                tint = if (config.locked || !enabled) MaterialTheme.colorScheme.outline
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(if (config.locked) R.string.overview_locked
                                else if (config.visible) R.string.overview_shown else R.string.overview_hidden),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            onChange(layout.configure(config.card) { it.copy(locked = !it.locked) })
                        }, enabled = enabled) {
                            Icon(painterResource(if (config.locked) R.drawable.ic_overview_lock
                                else R.drawable.ic_overview_unlock),
                                stringResource(if (config.locked) R.string.overview_unlock_card
                                    else R.string.overview_lock_card, title))
                        }
                        val showLabel = stringResource(R.string.overview_show_card, title)
                        Switch(checked = config.visible, onCheckedChange = { value ->
                            onChange(layout.configure(config.card) { it.copy(visible = value) })
                        }, enabled = enabled && !config.locked,
                            modifier = Modifier.semantics { contentDescription = showLabel })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically) {
                        OverviewCardSize.entries.forEach { size ->
                            val sizeLabel = stringResource(if (size == OverviewCardSize.Compact)
                                R.string.overview_compact else R.string.overview_expanded)
                            FilterChip(selected = config.size == size,
                                onClick = { onChange(layout.configure(config.card) { it.copy(size = size) }) },
                                enabled = enabled && !config.locked, label = { Text(sizeLabel) },
                                modifier = Modifier.semantics {
                                    contentDescription = "$title: $sizeLabel"
                                })
                        }
                        IconButton(onClick = { move(-1) }, enabled = canMoveUp) {
                            Icon(painterResource(R.drawable.ic_overview_up), up)
                        }
                        IconButton(onClick = { move(1) }, enabled = canMoveDown) {
                            Icon(painterResource(R.drawable.ic_overview_down), down)
                        }
                    }
                    if (config.card == OverviewCard.Overdue) {
                        Text(stringResource(R.string.overview_overdue_visibility),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            TextButton(onClick = { resetDialog = true }, enabled = enabled) {
                Icon(painterResource(R.drawable.ic_restart_alt), null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.overview_reset_layout))
            }
        }
    }
    if (resetDialog) {
        AlertDialog(onDismissRequest = { resetDialog = false },
            title = { Text(stringResource(R.string.overview_reset_layout)) },
            text = { Text(stringResource(R.string.overview_reset_message)) },
            confirmButton = { TextButton(onClick = {
                onChange(OverviewLayout())
                resetDialog = false
            }) { Text(stringResource(R.string.overview_reset)) } },
            dismissButton = { TextButton(onClick = { resetDialog = false }) {
                Text(stringResource(R.string.action_cancel))
            } })
    }
}
