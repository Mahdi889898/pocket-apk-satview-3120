package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PlaylistWithCount
import com.example.data.model.ChannelCategory
import com.example.data.repository.FtaPresetDirectory
import com.example.ui.ImportStatus
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.SignalCoral
import com.example.ui.theme.SlateElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportScreen(
    importStatus: ImportStatus,
    playlists: List<PlaylistWithCount>,
    ftaPresets: List<FtaPresetDirectory>,
    isSyncingDirectory: Boolean,
    onSyncAllGlobal: () -> Unit,
    onImportPreset: (FtaPresetDirectory) -> Unit,
    onImportUrl: (url: String, name: String) -> Unit,
    onImportFileUri: (Uri) -> Unit,
    onAddSingleChannel: (name: String, url: String, category: ChannelCategory, logoUrl: String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRestoreSamples: () -> Unit,
    onClearStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistUrl by rememberSaveable { mutableStateOf("") }
    var playlistName by rememberSaveable { mutableStateOf("") }

    var channelName by rememberSaveable { mutableStateOf("") }
    var channelUrl by rememberSaveable { mutableStateOf("") }
    var channelLogo by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ChannelCategory.GENERAL) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportFileUri(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_import),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            AnimatedVisibility(visible = importStatus !is ImportStatus.Idle) {
                ImportStatusBanner(
                    status = importStatus,
                    onDismiss = onClearStatus
                )
            }

            // Card 0: One-Tap Global & Category FTA Directory Packs
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BroadcastCyan.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = BroadcastCyan
                        )
                        Text(
                            text = stringResource(R.string.fta_presets_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = stringResource(R.string.fta_presets_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSyncAllGlobal,
                        enabled = !isSyncingDirectory && importStatus !is ImportStatus.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("sync_1000_channels_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSyncingDirectory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = stringResource(R.string.sync_directory_btn))
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ftaPresets.forEachIndexed { index, preset ->
                            AssistChip(
                                onClick = { onImportPreset(preset) },
                                enabled = importStatus !is ImportStatus.Loading && !isSyncingDirectory,
                                modifier = Modifier.testTag("fta_preset_chip_$index"),
                                label = {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SlateElevated,
                                    labelColor = BroadcastCyan
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }

            // Card 1: M3U URL & SAF File Picker
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = BroadcastCyan
                        )
                        Text(
                            text = stringResource(R.string.import_m3u_url_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = stringResource(R.string.import_m3u_url_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text(stringResource(R.string.playlist_name_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = playlistUrl,
                            onValueChange = { playlistUrl = it },
                            label = { Text(stringResource(R.string.playlist_url_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_url_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Button(
                        onClick = { onImportUrl(playlistUrl, playlistName) },
                        enabled = importStatus !is ImportStatus.Loading && playlistUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("import_url_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (importStatus is ImportStatus.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = stringResource(R.string.import_from_url_btn))
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "audio/x-mpegurl",
                                    "application/vnd.apple.mpegurl",
                                    "application/x-mpegurl",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        enabled = importStatus !is ImportStatus.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("import_file_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.import_from_file_btn))
                    }
                }
            }

            // Card 2: Single Channel Manual Entry
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = BroadcastCyan
                        )
                        Text(
                            text = stringResource(R.string.add_single_channel_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = channelName,
                        onValueChange = { channelName = it },
                        label = { Text(stringResource(R.string.channel_name_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("single_channel_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = channelUrl,
                            onValueChange = { channelUrl = it },
                            label = { Text(stringResource(R.string.channel_url_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("single_channel_url_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = channelLogo,
                            onValueChange = { channelLogo = it },
                            label = { Text(stringResource(R.string.channel_logo_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("single_channel_logo_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.channel_category_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChannelCategory.editableCategories.forEach { cat ->
                            val selected = cat == selectedCategory
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.labelFa) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SlateElevated,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = BroadcastCyan
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onAddSingleChannel(channelName, channelUrl, selectedCategory, channelLogo)
                            channelName = ""
                            channelUrl = ""
                            channelLogo = ""
                        },
                        enabled = channelName.isNotBlank() && channelUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("save_single_channel_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = stringResource(R.string.add_channel_btn))
                    }
                }
            }

            // Card 3: Saved Playlists Management
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.saved_playlists_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = onRestoreSamples,
                            modifier = Modifier.testTag("restore_samples_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BroadcastCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.restore_samples_btn),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    playlists.forEach { playlist ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SlateElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.channels_count_format, playlist.channelCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeletePlaylist(playlist.id) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_playlist_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.delete_playlist),
                                        tint = SignalCoral
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportStatusBanner(
    status: ImportStatus,
    onDismiss: () -> Unit
) {
    val isError = status is ImportStatus.Error
    val accent = when (status) {
        is ImportStatus.Error -> SignalCoral
        is ImportStatus.Success -> EmeraldLive
        else -> BroadcastCyan
    }
    val text = when (status) {
        is ImportStatus.Loading -> stringResource(R.string.player_connecting)
        is ImportStatus.Success -> status.message
        is ImportStatus.Error -> status.message
        ImportStatus.Idle -> ""
    }

    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("import_status_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = accent
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
