package com.adaptwise.foldsamples.listdetail.broken

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.serialization.Serializable

// Serializable keys match the list-detail sample. This copy does not save them.
@Serializable
data object ItemListKey : NavKey

@Serializable
data class ItemDetailKey(val id: Int) : NavKey

/**
 * The list-detail sample with the posture fix taken out.
 *
 * A compact window, including a Z Fold cover, still gets two panes. An unfolded
 * portrait window does too. A book hinge is not excluded, and a tabletop fold
 * gets no gap. The selection is forgotten when the activity is recreated.
 * The list-detail module is this screen with those behaviors put back.
 * Capture sizes for the Z Fold 8 are in [ZFold8].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ListDetailScreen(
    modifier: Modifier = Modifier,
    items: List<Item> = sampleItems,
    adaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
    initialSelectionId: Int? = null,
) {
    val backStack = remember {
        mutableStateListOf<NavKey>(ItemListKey).apply {
            initialSelectionId?.let { add(ItemDetailKey(it)) }
        }
    }
    val directive = remember(adaptiveInfo) { paneDirectiveIgnoringFold(adaptiveInfo) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val singlePane = directive.maxHorizontalPartitions == 1
    val selectedId = (backStack.lastOrNull() as? ItemDetailKey)?.id

    Surface(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.safeDrawingPadding(),
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(listDetailStrategy),
            entryProvider = entryProvider {
                entry<ItemListKey>(
                    metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { EmptyDetail() }),
                ) {
                    ItemList(
                        items = items,
                        selectedId = selectedId,
                        onItemClick = { item ->
                            backStack.removeAll { it is ItemDetailKey }
                            backStack.add(ItemDetailKey(item.id))
                        },
                    )
                }
                entry<ItemDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    ItemDetail(
                        item = items.first { it.id == key.id },
                        posture = adaptiveInfo.windowPosture,
                        onBack = if (singlePane) {
                            { backStack.removeLastOrNull() }
                        } else {
                            null
                        },
                    )
                }
            },
        )
    }
}

/** Always the unfolded landscape directive, with no hinge. The sample passes [window] through. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun paneDirectiveIgnoringFold(@Suppress("UNUSED_PARAMETER") window: WindowAdaptiveInfo): PaneScaffoldDirective {
    val unfolded = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(841f, 701f)
    return calculatePaneScaffoldDirective(WindowAdaptiveInfo(unfolded, Posture()))
}

/** No gap, so the detail is one column across a tabletop fold. The sample returns the hinge height. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun tabletopGap(
    @Suppress("UNUSED_PARAMETER") posture: Posture,
    @Suppress("UNUSED_PARAMETER") density: Density,
): Dp? = null

@Composable
private fun ItemList(items: List<Item>, selectedId: Int?, onItemClick: (Item) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id == selectedId
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = { Text(item.body, maxLines = 1) },
                colors = if (isSelected) {
                    ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    ListItemDefaults.colors()
                },
                modifier = Modifier
                    .semantics { this.selected = isSelected }
                    .clickable { onItemClick(item) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ItemDetail(item: Item, posture: Posture, onBack: (() -> Unit)?) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("Back") }
        }
        val gap = tabletopGap(posture, LocalDensity.current)
        if (gap != null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
                DetailTitle(item)
            }
            Spacer(Modifier.height(gap))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                DetailBody(item)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailTitle(item)
                DetailBody(item)
            }
        }
    }
}

@Composable
private fun DetailTitle(item: Item) {
    Text(item.title, style = MaterialTheme.typography.displaySmall)
}

@Composable
private fun DetailBody(item: Item) {
    Text(item.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun EmptyDetail() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Select an item", style = MaterialTheme.typography.titleMedium)
    }
}
