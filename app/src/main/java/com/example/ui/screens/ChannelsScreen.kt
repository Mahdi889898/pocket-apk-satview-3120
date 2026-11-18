package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChannelEntity
import com.example.data.model.ChannelCategory
import com.example.ui.components.ChannelCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RecentChannelChipCard
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SignalCoral
import com.example.ui.theme.SlateElevated

@Composable
fun ChannelsScreen(
    channels: List<ChannelEntity>,
    totalChannelCount: Int,
    filteredChannelCount: Int,
    recentChannels: List<ChannelEntity>,
    searchQuery: String,
    selectedCategory: ChannelCategory,
    isSyncingDirectory: Boolean,
    syncError: String?,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (ChannelCategory) -> Unit,
    onLoadMore: () -> Unit,
    onChannelPlay: (Long) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onDeleteChannel: (Long) -> Unit,
    onRetrySync: () -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember(channels.size, filteredChannelCount) {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            channels.size < filteredChannelCount && totalItems > 0 && lastVisible >= totalItems - 12
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(columns) }) {
                BroadcastHeroHeader(
                    channelCount = totalChannelCount,
                    onQuickAddClick = onNavigateToImport
                )
            }

            if (isSyncingDirectory || syncError != null) {
                item(span = { GridItemSpan(columns) }) {
                    AnimatedVisibility(visible = isSyncingDirectory || syncError != null) {
                        DirectorySyncBanner(
                            isSyncing = isSyncingDirectory,
                            errorMessage = syncError,
                            onRetry = onRetrySync
                        )
                    }
                }
            }

            item(span = { GridItemSpan(columns) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_channels_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_channels_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = BroadcastCyan
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface,
                        focusedBorderColor = BroadcastCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item(span = { GridItemSpan(columns) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ChannelCategory.entries, key = { it.name }) { category ->
                        val selected = category == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { onCategorySelect(category) },
                            modifier = Modifier.testTag("category_chip_${category.name}"),
                            label = {
                                Text(
                                    text = category.labelFa,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateElevated,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = BroadcastCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = BroadcastCyan
                            )
                        )
                    }
                }
            }

            if (recentChannels.isNotEmpty() && searchQuery.isBlank() && selectedCategory == ChannelCategory.ALL) {
                item(span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.recently_watched),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(recentChannels, key = { "recent_${it.id}" }) { recent ->
                                RecentChannelChipCard(
                                    channel = recent,
                                    onPlayClick = { onChannelPlay(recent.id) }
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.all_channels),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.channels_count_format, filteredChannelCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = BroadcastCyan
                    )
                }
            }

            if (channels.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    EmptyStatePanel(
                        icon = Icons.Default.TvOff,
                        title = stringResource(R.string.empty_channels_title),
                        subtitle = stringResource(R.string.empty_channels_subtitle),
                        actionContent = {
                            Button(
                                onClick = onNavigateToImport,
                                modifier = Modifier.testTag("empty_go_import_button")
                            ) {
                                Text(text = stringResource(R.string.nav_import))
                            }
                        }
                    )
                }
            } else {
                items(channels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        onPlayClick = { onChannelPlay(channel.id) },
                        onToggleFavorite = { onToggleFavorite(channel) },
                        onDeleteClick = { onDeleteChannel(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectorySyncBanner(
    isSyncing: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    val accent = if (isSyncing) BroadcastCyan else SignalCoral
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("directory_sync_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = BroadcastCyan
                    )
                    Text(
                        text = stringResource(R.string.syncing_directory),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = SignalCoral
                    )
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!isSyncing && errorMessage != null) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.sync_directory_retry),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun BroadcastHeroHeader(
    channelCount: Int,
    onQuickAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MidnightSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            BroadcastCyan.copy(alpha = 0.16f),
                            MidnightSurface,
                            ObsidianVoid
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SatelliteAlt,
                            contentDescription = null,
                            tint = BroadcastCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.channels_count_format, channelCount) + " آماده پخش",
                        style = MaterialTheme.typography.labelLarge,
                        color = BroadcastCyan
                    )
                }

                Surface(
                    onClick = onQuickAddClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hero_add_m3u_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, BroadcastCyan.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = BroadcastCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "M3U",
                            style = MaterialTheme.typography.labelLarge,
                            color = BroadcastCyan
                        )
                    }
                }
            }
        }
    }
}
