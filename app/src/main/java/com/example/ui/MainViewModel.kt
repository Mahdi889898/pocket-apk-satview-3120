package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistWithCount
import com.example.data.model.ChannelCategory
import com.example.data.repository.ChannelRepository
import com.example.data.repository.FtaPresetDirectory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Loading : ImportStatus
    data class Success(val message: String) : ImportStatus
    data class Error(val message: String) : ImportStatus
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChannelRepository(
        dao = AppDatabase.getInstance(application).channelDao()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ChannelCategory.ALL)
    val selectedCategory: StateFlow<ChannelCategory> = _selectedCategory.asStateFlow()

    private val _displayLimit = MutableStateFlow(80)

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> = _importStatus.asStateFlow()

    private val _isSyncingDirectory = MutableStateFlow(false)
    val isSyncingDirectory: StateFlow<Boolean> = _isSyncingDirectory.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    val ftaPresets: List<FtaPresetDirectory> = ChannelRepository.ftaPresets

    val totalChannelCount: StateFlow<Int> = repository.totalChannelCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val debouncedQuery: Flow<String> = _searchQuery
        .debounce(120)
        .distinctUntilChanged()

    val filteredChannelCount: StateFlow<Int> = combine(
        debouncedQuery,
        _selectedCategory
    ) { query, category ->
        query.trim() to category.labelFa
    }.flatMapLatest { (query, categoryLabel) ->
        repository.observeFilteredChannelCount(query, categoryLabel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        debouncedQuery,
        _selectedCategory,
        _displayLimit
    ) { query, category, limit ->
        Triple(query.trim(), category.labelFa, limit)
    }.flatMapLatest { (query, categoryLabel, limit) ->
        repository.observeFilteredChannels(query, categoryLabel, limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteChannels: StateFlow<List<ChannelEntity>> = repository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentChannels: StateFlow<List<ChannelEntity>> = repository.recentChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<PlaylistWithCount>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSampleChannelsIfEmpty()
            syncGlobalDirectory(force = false)
        }
    }

    fun loadMoreChannels() {
        val currentLoaded = filteredChannels.value.size
        val totalMatching = filteredChannelCount.value
        if (currentLoaded < totalMatching && _displayLimit.value <= currentLoaded) {
            _displayLimit.value += 100
        }
    }

    fun syncGlobalDirectory(force: Boolean = true) {
        if (_isSyncingDirectory.value) return
        viewModelScope.launch {
            _isSyncingDirectory.value = true
            _syncError.value = null
            val result = repository.syncGlobalFtaDirectory(force = force)
            result.fold(
                onSuccess = { added ->
                    if (force && added > 0) {
                        _importStatus.value = ImportStatus.Success(
                            "$added شبکه آزاد جهانی و فارسی همگام‌سازی شد."
                        )
                    }
                },
                onFailure = { err ->
                    _syncError.value = err.message ?: "خطا در دریافت فهرست شبکه‌های آزاد"
                }
            )
            _isSyncingDirectory.value = false
        }
    }

    fun importPresetDirectory(preset: FtaPresetDirectory) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            val result = repository.importPlaylistFromUrl(
                url = preset.url,
                customName = preset.title,
                maxChannels = preset.maxChannels
            )
            _importStatus.value = result.fold(
                onSuccess = { count ->
                    ImportStatus.Success("$count شبکه از «${preset.title}» اضافه شد.")
                },
                onFailure = { err ->
                    ImportStatus.Error(err.message ?: "خطا در دریافت فهرست شبکه‌ها")
                }
            )
        }
    }

    fun observeChannel(channelId: Long): Flow<ChannelEntity?> = repository.observeChannel(channelId)

    fun observeQuickZapChannels(category: String): Flow<List<ChannelEntity>> =
        repository.observeQuickZapChannels(category)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _displayLimit.value = 80
    }

    fun onCategorySelected(category: ChannelCategory) {
        _selectedCategory.value = category
        _displayLimit.value = 80
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, channel.isFavorite)
        }
    }

    fun markWatched(channelId: Long) {
        viewModelScope.launch {
            repository.markWatched(channelId)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun restoreSampleChannels() {
        viewModelScope.launch {
            val count = repository.restoreSampleChannels()
            _importStatus.value = if (count > 0) {
                ImportStatus.Success("$count شبکه نمونه آزاد بازیابی شد.")
            } else {
                ImportStatus.Success("شبکه‌های نمونه آزاد از قبل در فهرست موجود هستند.")
            }
        }
    }

    fun importM3uFromUrl(url: String, playlistName: String) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            val result = repository.importPlaylistFromUrl(url, playlistName)
            _importStatus.value = result.fold(
                onSuccess = { count ->
                    ImportStatus.Success("$count شبکه با موفقیت به فهرست اضافه شد.")
                },
                onFailure = { err ->
                    ImportStatus.Error(err.message ?: "خطا در دریافت فایل M3U")
                }
            )
        }
    }

    fun importM3uFromUri(uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            val resolver = getApplication<Application>().contentResolver
            val fileName = resolveDisplayName(uri)
            val result = repository.importPlaylistFromUri(resolver, uri, fileName)
            _importStatus.value = result.fold(
                onSuccess = { count ->
                    ImportStatus.Success("$count شبکه از فایل محلی درون‌ریزی شد.")
                },
                onFailure = { err ->
                    ImportStatus.Error(err.message ?: "خطا در خواندن فایل M3U")
                }
            )
        }
    }

    fun addSingleChannel(
        name: String,
        streamUrl: String,
        category: ChannelCategory,
        logoUrl: String,
        onAddedAndPlay: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addSingleChannel(
                name = name,
                streamUrl = streamUrl,
                category = category.labelFa,
                logoUrl = logoUrl
            )
            result.fold(
                onSuccess = { newId ->
                    _importStatus.value = ImportStatus.Success("شبکه «$name» ذخیره شد.")
                    onAddedAndPlay(newId)
                },
                onFailure = { err ->
                    _importStatus.value = ImportStatus.Error(err.message ?: "خطا در افزودن شبکه")
                }
            )
        }
    }

    fun clearImportStatus() {
        _importStatus.value = ImportStatus.Idle
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }
}
