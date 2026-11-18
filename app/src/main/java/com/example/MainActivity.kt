package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.SatViewTheme

const val ROUTE_CHANNELS = "channels"
const val ROUTE_FAVORITES = "favorites"
const val ROUTE_IMPORT = "import"
const val ROUTE_GUIDE = "guide"
const val ROUTE_PLAYER = "player/{channelId}"

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelTabs = listOf(
    TopLevelTab(
        route = ROUTE_CHANNELS,
        labelRes = R.string.nav_channels,
        selectedIcon = Icons.Filled.LiveTv,
        unselectedIcon = Icons.Outlined.LiveTv,
        testTag = "nav_tab_channels"
    ),
    TopLevelTab(
        route = ROUTE_FAVORITES,
        labelRes = R.string.nav_favorites,
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarOutline,
        testTag = "nav_tab_favorites"
    ),
    TopLevelTab(
        route = ROUTE_IMPORT,
        labelRes = R.string.nav_import,
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        testTag = "nav_tab_import"
    ),
    TopLevelTab(
        route = ROUTE_GUIDE,
        labelRes = R.string.nav_guide,
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info,
        testTag = "nav_tab_guide"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SatViewTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SatViewApp()
                }
            }
        }
    }
}

@Composable
fun SatViewApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isPlayerRoute = currentDestination?.route?.startsWith("player/") == true

    val filteredChannels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val totalChannelCount by viewModel.totalChannelCount.collectAsStateWithLifecycle()
    val filteredChannelCount by viewModel.filteredChannelCount.collectAsStateWithLifecycle()
    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
    val recentChannels by viewModel.recentChannels.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()
    val isSyncingDirectory by viewModel.isSyncingDirectory.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()

    val navigateToPlayer: (Long) -> Unit = { channelId ->
        navController.navigate("player/$channelId") {
            launchSingleTop = true
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = if (isPlayerRoute) WindowInsets(0) else WindowInsets.safeDrawing,
            bottomBar = {
                if (!isPlayerRoute && !useNavRail) {
                    NavigationBar(
                        containerColor = MidnightSurface,
                        tonalElevation = 0.dp
                    ) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == tab.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BroadcastCyan,
                                    selectedTextColor = BroadcastCyan,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isPlayerRoute && useNavRail) {
                    NavigationRail(containerColor = MidnightSurface) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == tab.route
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BroadcastCyan,
                                    selectedTextColor = BroadcastCyan,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_CHANNELS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_CHANNELS) {
                        ChannelsScreen(
                            channels = filteredChannels,
                            totalChannelCount = totalChannelCount,
                            filteredChannelCount = filteredChannelCount,
                            recentChannels = recentChannels,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            isSyncingDirectory = isSyncingDirectory,
                            syncError = syncError,
                            onSearchChange = viewModel::onSearchQueryChange,
                            onCategorySelect = viewModel::onCategorySelected,
                            onLoadMore = viewModel::loadMoreChannels,
                            onChannelPlay = navigateToPlayer,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onDeleteChannel = viewModel::deleteChannel,
                            onRetrySync = { viewModel.syncGlobalDirectory(force = true) },
                            onNavigateToImport = { navigateToTab(ROUTE_IMPORT) }
                        )
                    }

                    composable(ROUTE_FAVORITES) {
                        FavoritesScreen(
                            favoriteChannels = favoriteChannels,
                            onChannelPlay = navigateToPlayer,
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                    }

                    composable(ROUTE_IMPORT) {
                        ImportScreen(
                            importStatus = importStatus,
                            playlists = playlists,
                            ftaPresets = viewModel.ftaPresets,
                            isSyncingDirectory = isSyncingDirectory,
                            onSyncAllGlobal = { viewModel.syncGlobalDirectory(force = true) },
                            onImportPreset = viewModel::importPresetDirectory,
                            onImportUrl = viewModel::importM3uFromUrl,
                            onImportFileUri = viewModel::importM3uFromUri,
                            onAddSingleChannel = { name, url, cat, logo ->
                                viewModel.addSingleChannel(name, url, cat, logo) { newId ->
                                    navigateToPlayer(newId)
                                }
                            },
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onRestoreSamples = viewModel::restoreSampleChannels,
                            onClearStatus = viewModel::clearImportStatus
                        )
                    }

                    composable(ROUTE_GUIDE) {
                        GuideScreen()
                    }

                    composable(
                        route = ROUTE_PLAYER,
                        arguments = listOf(navArgument("channelId") { type = NavType.LongType })
                    ) { entry ->
                        val channelId = entry.arguments?.getLong("channelId") ?: -1L
                        val activeChannel by viewModel.observeChannel(channelId)
                            .collectAsStateWithLifecycle(initialValue = null)
                        val zapChannels by viewModel.observeQuickZapChannels(
                            activeChannel?.category ?: ""
                        ).collectAsStateWithLifecycle(initialValue = emptyList())

                        PlayerScreen(
                            channel = activeChannel,
                            allChannels = zapChannels,
                            onSelectChannel = { nextId ->
                                navController.navigate("player/$nextId") {
                                    popUpTo(ROUTE_PLAYER) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onMarkWatched = viewModel::markWatched,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
