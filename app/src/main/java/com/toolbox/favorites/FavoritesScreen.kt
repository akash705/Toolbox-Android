package com.toolbox.favorites

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toolbox.core.persistence.UserPreferencesRepository
import com.toolbox.dashboard.ToolDefinition
import com.toolbox.dashboard.allTools
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    onToolClick: (ToolDefinition) -> Unit,
    onBrowseAll: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = UserPreferencesRepository(context)
    val favoriteIds by repository.favoriteToolIds.collectAsState(initial = emptySet())
    val pinnedIds by repository.pinnedToolIds.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val favoriteTools = allTools.filter { it.id in favoriteIds }
    val pinnedTools = favoriteTools.filter { it.id in pinnedIds }
    val unpinnedTools = favoriteTools.filter { it.id !in pinnedIds }

    if (favoriteTools.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(48.dp),
            ) {
                Icon(
                    Icons.Outlined.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Favorites Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Personalize your technical atelier. Long-press any tool on the dashboard to pin it here for instant access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBrowseAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text("Browse All Tools")
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (pinnedTools.isNotEmpty()) {
                item {
                    Text(
                        text = "Pinned",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                items(pinnedTools, key = { it.id }) { tool ->
                    FavoriteToolItem(
                        tool = tool,
                        isPinned = true,
                        canPin = true,
                        onToolClick = onToolClick,
                        onTogglePin = { scope.launch { repository.togglePin(tool.id) } },
                        onRemove = { scope.launch { repository.toggleFavorite(tool.id) } },
                    )
                }
            }
            if (unpinnedTools.isNotEmpty()) {
                item {
                    Text(
                        text = if (pinnedTools.isNotEmpty()) "Others" else "All Favorites",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 4.dp,
                            top = if (pinnedTools.isNotEmpty()) 8.dp else 0.dp,
                            bottom = 4.dp,
                        ),
                    )
                }
                items(unpinnedTools, key = { it.id }) { tool ->
                    FavoriteToolItem(
                        tool = tool,
                        isPinned = false,
                        canPin = pinnedIds.size < 3,
                        onToolClick = onToolClick,
                        onTogglePin = { scope.launch { repository.togglePin(tool.id) } },
                        onRemove = { scope.launch { repository.toggleFavorite(tool.id) } },
                    )
                }
            }
            item {
                Text(
                    text = "Swipe left to remove from favorites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun FavoriteToolItem(
    tool: ToolDefinition,
    isPinned: Boolean,
    canPin: Boolean,
    onToolClick: (ToolDefinition) -> Unit,
    onTogglePin: () -> Unit,
    onRemove: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "dismiss-bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToolClick(tool) },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = tool.category.tileColor,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = tool.category.iconTint,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tool.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = tool.category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onTogglePin,
                    enabled = isPinned || canPin,
                ) {
                    Icon(
                        if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "Unpin" else "Pin to top",
                        tint = if (isPinned) {
                            MaterialTheme.colorScheme.primary
                        } else if (canPin) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                }
            }
        }
    }
}
