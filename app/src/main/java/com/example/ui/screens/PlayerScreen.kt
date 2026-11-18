package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.local.ChannelEntity
import com.example.ui.components.ChannelLogoBadge
import com.example.ui.components.LivePillBadge
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.GoldStar
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SignalCoral
import com.example.ui.theme.SlateElevated

private enum class PlaybackUiState {
    CONNECTING,
    BUFFERING,
    PLAYING,
    ERROR
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: ChannelEntity?,
    allChannels: List<ChannelEntity>,
    onSelectChannel: (Long) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onMarkWatched: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var resizeModeIndex by rememberSaveable { mutableIntStateOf(0) }
    var playbackState by remember { mutableStateOf(PlaybackUiState.CONNECTING) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to stringResource(R.string.player_aspect_fit),
        AspectRatioFrameLayout.RESIZE_MODE_FILL to stringResource(R.string.player_aspect_fill),
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to stringResource(R.string.player_aspect_zoom)
    )
    val currentResizeMode = resizeModes[resizeModeIndex % resizeModes.size]

    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_500,
                /* maxBufferMs = */ 15_000,
                /* bufferForPlaybackMs = */ 800,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    // Manage fullscreen orientation & system bars cleanly
    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Restore orientation and system bars on exit
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            exoPlayer.release()
        }
    }

    // Pause on background / resume on foreground
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_START -> if (exoPlayer.mediaItemCount > 0) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Observe ExoPlayer events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = when (state) {
                    Player.STATE_IDLE -> PlaybackUiState.CONNECTING
                    Player.STATE_BUFFERING -> PlaybackUiState.BUFFERING
                    Player.STATE_READY -> PlaybackUiState.PLAYING
                    Player.STATE_ENDED -> PlaybackUiState.PLAYING
                    else -> PlaybackUiState.CONNECTING
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackState = PlaybackUiState.ERROR
                errorMessage = error.localizedMessage
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Load stream whenever active channel changes
    LaunchedEffect(channel?.id, channel?.streamUrl) {
        val active = channel ?: return@LaunchedEffect
        playbackState = PlaybackUiState.CONNECTING
        errorMessage = null
        onMarkWatched(active.id)
        exoPlayer.setMediaItem(MediaItem.fromUri(active.streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.player_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (playbackState) {
                                PlaybackUiState.CONNECTING -> stringResource(R.string.player_connecting)
                                PlaybackUiState.BUFFERING -> stringResource(R.string.player_buffering)
                                PlaybackUiState.PLAYING -> stringResource(R.string.player_live_active)
                                PlaybackUiState.ERROR -> stringResource(R.string.player_error_title)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (playbackState == PlaybackUiState.ERROR) SignalCoral else BroadcastCyan
                        )
                    }
                }

                if (channel != null) {
                    IconButton(
                        onClick = { onToggleFavorite(channel) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.cd_favorite_toggle),
                            tint = if (channel.isFavorite) GoldStar else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Video Viewport
        Box(
            modifier = if (isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            keepScreenOn = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        }
                    },
                    update = { view ->
                        view.resizeMode = currentResizeMode.first
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("media3_player_view")
                )
            }

            // Live badge overlay top-start
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                LivePillBadge(
                    text = stringResource(R.string.live_badge),
                    isLive = playbackState == PlaybackUiState.PLAYING
                )
            }

            // Fullscreen toggle top-end
            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("player_fullscreen_button")
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = stringResource(
                        if (isFullscreen) R.string.player_exit_fullscreen else R.string.player_fullscreen
                    ),
                    tint = Color.White
                )
            }

            // Loading / Buffering overlay
            if (playbackState == PlaybackUiState.CONNECTING || playbackState == PlaybackUiState.BUFFERING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = BroadcastCyan)
                        Text(
                            text = if (playbackState == PlaybackUiState.CONNECTING) {
                                stringResource(R.string.player_connecting)
                            } else {
                                stringResource(R.string.player_buffering)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }

            // Error overlay with Retry
            if (playbackState == PlaybackUiState.ERROR) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = SignalCoral,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = stringResource(R.string.player_error_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.player_error_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = {
                                playbackState = PlaybackUiState.CONNECTING
                                errorMessage = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            modifier = Modifier.testTag("player_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.player_retry))
                        }
                    }
                }
            }
        }

        if (!isFullscreen) {
            // Quick control strip (Mute, Aspect Ratio, Retry)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerActionChip(
                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    label = stringResource(if (isMuted) R.string.player_unmute else R.string.player_mute),
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_mute_button")
                )
                PlayerActionChip(
                    icon = Icons.Default.AspectRatio,
                    label = currentResizeMode.second,
                    onClick = { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_aspect_button")
                )
                PlayerActionChip(
                    icon = Icons.Default.Refresh,
                    label = stringResource(R.string.player_retry),
                    onClick = {
                        playbackState = PlaybackUiState.CONNECTING
                        exoPlayer.prepare()
                        exoPlayer.play()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_reload_button")
                )
            }

            // Channel zapping strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_switch_channel),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allChannels, key = { "zap_${it.id}" }) { item ->
                        val isCurrent = item.id == channel?.id
                        Card(
                            onClick = { if (!isCurrent) onSelectChannel(item.id) },
                            modifier = Modifier
                                .width(152.dp)
                                .testTag("zap_channel_${item.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    SlateElevated
                                }
                            ),
                            border = BorderStroke(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) BroadcastCyan else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChannelLogoBadge(
                                    name = item.name,
                                    logoUrl = item.logoUrl,
                                    modifier = Modifier.size(38.dp)
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) BroadcastCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(14.dp),
        color = SlateElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BroadcastCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
