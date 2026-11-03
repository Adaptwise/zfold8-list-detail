package com.adaptwise.foldsamples.listdetail

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
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

// Serializable keys let rememberNavBackStack restore the selection after rotation and fold/unfold:
// https://developer.android.com/guide/navigation/navigation-3
@Serializable
data object ItemListKey : NavKey

@Serializable
data class ItemDetailKey(val id: Int) : NavKey

/**
 * List-detail with Navigation 3 and the Material ListDetailSceneStrategy, the approach Google's
 * adaptive guidance prescribes:
 * https://developer.android.com/guide/navigation/navigation-3/recipes/material-listdetail
 *
 * [adaptiveInfo] is a parameter so screenshot tests can inject fold postures; the app uses
 * currentWindowAdaptiveInfoV2(), which reports the V2 breakpoints and the window posture:
 * https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ListDetailScreen(
    modifier: Modifier = Modifier,
    items: List<Item> = sampleItems,
    adaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
    initialSelectionId: Int? = null,
) {
    val backStack = rememberNavBackStack(
        *listOfNotNull<NavKey>(ItemListKey, initialSelectionId?.let(::ItemDetailKey)).toTypedArray(),
    )
    // The directive applies the window size class and keeps panes off separating hinges.
    val directive = remember(adaptiveInfo) { calculatePaneScaffoldDirective(adaptiveInfo) }
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
                            // Replace rather than stack details, so Back returns to the list.
                            backStack.removeAll { it is ItemDetailKey }
                            backStack.add(ItemDetailKey(item.id))
                        },
                    )
                }
                entry<ItemDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    ItemDetail(
                        item = items.first { it.id == key.id },
                        posture = adaptiveInfo.windowPosture,
                        // No back affordance when the list is visible beside the detail.
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

/**
 * In tabletop posture the title sits above the fold and the body below it, the split
 * Google recommends for half-opened horizontal folds:
 * https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware
 */
@Composable
private fun ItemDetail(item: Item, posture: Posture, onBack: (() -> Unit)?) {
    val horizontalHinge = posture.hingeList.firstOrNull { !it.isVertical && it.isSeparating }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("Back") }
        }
        if (posture.isTabletop && horizontalHinge != null) {
            val hingeHeight = with(LocalDensity.current) { horizontalHinge.bounds.height.toDp() }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
                DetailTitle(item)
            }
            Spacer(Modifier.height(hingeHeight))
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
