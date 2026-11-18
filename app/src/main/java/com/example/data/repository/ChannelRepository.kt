package com.example.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.data.local.ChannelDao
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistWithCount
import com.example.data.model.ChannelCategory
import com.example.data.parser.M3uParser
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class FtaPresetDirectory(
    val title: String,
    val url: String,
    val maxChannels: Int = 5000
)

class ChannelRepository(
    private val dao: ChannelDao,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        const val PERSIAN_FTA_URL = "https://iptv-org.github.io/iptv/languages/fas.m3u"
        const val GLOBAL_FTA_URL = "https://iptv-org.github.io/iptv/index.m3u"

        val ftaPresets: List<FtaPresetDirectory> = listOf(
            FtaPresetDirectory(
                title = "دایرکتوری کامل جهانی (۱۰٬۰۰۰+ شبکه)",
                url = GLOBAL_FTA_URL,
                maxChannels = 15_000
            ),
            FtaPresetDirectory(
                title = "شبکه‌های فارسی آزاد",
                url = PERSIAN_FTA_URL,
                maxChannels = 1_000
            ),
            FtaPresetDirectory(
                title = "خبری جهان",
                url = "https://iptv-org.github.io/iptv/categories/news.m3u",
                maxChannels = 2_000
            ),
            FtaPresetDirectory(
                title = "ورزشی آزاد",
                url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
                maxChannels = 2_000
            ),
            FtaPresetDirectory(
                title = "فیلم و سینما",
                url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
                maxChannels = 2_000
            ),
            FtaPresetDirectory(
                title = "مستند",
                url = "https://iptv-org.github.io/iptv/categories/documentary.m3u",
                maxChannels = 2_000
            ),
            FtaPresetDirectory(
                title = "کودک و نوجوان",
                url = "https://iptv-org.github.io/iptv/categories/kids.m3u",
                maxChannels = 2_000
            ),
            FtaPresetDirectory(
                title = "موسیقی",
                url = "https://iptv-org.github.io/iptv/categories/music.m3u",
                maxChannels = 2_000
            )
        )
    }

    val totalChannelCount: Flow<Int> = dao.observeTotalChannelCount()
    val favoriteChannels: Flow<List<ChannelEntity>> = dao.observeFavoriteChannels()
    val recentChannels: Flow<List<ChannelEntity>> = dao.observeRecentChannels()
    val playlists: Flow<List<PlaylistWithCount>> = dao.observePlaylistsWithCount()

    fun observeFilteredChannels(query: String, category: String, limit: Int): Flow<List<ChannelEntity>> =
        dao.observeFilteredChannels(query, category, limit)

    fun observeFilteredChannelCount(query: String, category: String): Flow<Int> =
        dao.observeFilteredChannelCount(query, category)

    fun observeQuickZapChannels(category: String): Flow<List<ChannelEntity>> =
        dao.observeQuickZapChannels(category, limit = 35)

    fun observeChannel(id: Long): Flow<ChannelEntity?> = dao.observeChannelById(id)

    suspend fun ensureSampleChannelsIfEmpty() {
        if (dao.countChannels() == 0) {
            seedSampleChannels()
        }
    }

    suspend fun syncGlobalFtaDirectory(force: Boolean = false): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                if (!force && dao.countNonSampleChannels() >= 10_000) {
                    return@withContext Result.success(0)
                }

                var totalAdded = 0
                val persianResult = importPlaylistFromUrl(
                    url = PERSIAN_FTA_URL,
                    customName = "شبکه‌های فارسی آزاد (FTA)",
                    maxChannels = 1_000
                )
                persianResult.onSuccess { totalAdded += it }

                val globalResult = importPlaylistFromUrl(
                    url = GLOBAL_FTA_URL,
                    customName = "شبکه‌های آزاد جهانی (FTA)",
                    maxChannels = 15_000
                )
                globalResult.fold(
                    onSuccess = {
                        totalAdded += it
                        Result.success(totalAdded)
                    },
                    onFailure = { err ->
                        if (totalAdded > 0) Result.success(totalAdded) else Result.failure(err)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun restoreSampleChannels(): Int {
        val existingSamples = dao.countSampleChannels()
        if (existingSamples > 0) return 0
        return seedSampleChannels()
    }

    private suspend fun seedSampleChannels(): Int {
        val playlist = dao.getBuiltInPlaylist()
        val playlistId = playlist?.id ?: dao.insertPlaylist(
            PlaylistEntity(
                name = "شبکه‌های نمونه آزاد (HLS)",
                sourceUrl = null,
                isBuiltIn = true
            )
        )
        val samples = listOf(
            ChannelEntity(
                playlistId = playlistId,
                name = "پخش آزمایشی Big Buck Bunny",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                category = ChannelCategory.MOVIES.labelFa,
                isSample = true
            ),
            ChannelEntity(
                playlistId = playlistId,
                name = "سینمایی کوتاه Tears of Steel",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                category = ChannelCategory.MOVIES.labelFa,
                isSample = true
            ),
            ChannelEntity(
                playlistId = playlistId,
                name = "انیمیشن Sintel HD",
                streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                category = ChannelCategory.KIDS.labelFa,
                isSample = true
            ),
            ChannelEntity(
                playlistId = playlistId,
                name = "استریم چندزبانه Angel One",
                streamUrl = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8",
                category = ChannelCategory.DEMO.labelFa,
                isSample = true
            ),
            ChannelEntity(
                playlistId = playlistId,
                name = "تست استریم زنده BipBop",
                streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                category = ChannelCategory.DEMO.labelFa,
                isSample = true
            )
        )
        dao.insertChannels(samples)
        return samples.size
    }

    suspend fun importPlaylistFromUrl(
        url: String,
        customName: String?,
        maxChannels: Int = 15_000
    ): Result<Int> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!M3uParser.isSupportedStreamUrl(cleanUrl)) {
            return@withContext Result.failure(
                IllegalArgumentException("نشانی باید با https:// یا http:// شروع شود.")
            )
        }
        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "SatViewPlayer/1.0")
                .build()
            val parsed = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("خطای سرور: کد ${response.code}")
                }
                val stream = response.body?.byteStream()
                    ?: return@use emptyList()
                stream.bufferedReader().use { reader ->
                    M3uParser.parse(reader, maxChannels = maxChannels)
                }
            }
            if (parsed.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("هیچ شبکه معتبری در این فایل M3U یافت نشد.")
                )
            }
            val derivedName = customName?.trim()?.takeIf { it.isNotEmpty() }
                ?: Uri.parse(cleanUrl).lastPathSegment?.substringBeforeLast('.')
                    ?.takeIf { it.isNotBlank() }
                ?: "فهرست آنلاین"

            val existingPlaylistId = dao.getPlaylistBySourceUrl(cleanUrl)?.id
            val entities = parsed.map { item ->
                ChannelEntity(
                    playlistId = 0L,
                    name = item.name,
                    streamUrl = item.streamUrl,
                    logoUrl = item.logoUrl,
                    category = item.category
                )
            }
            dao.replacePlaylistAndChannels(
                playlist = PlaylistEntity(
                    name = derivedName,
                    sourceUrl = cleanUrl,
                    isBuiltIn = false
                ),
                existingPlaylistId = existingPlaylistId,
                channels = entities
            )
            Result.success(parsed.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importPlaylistFromUri(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: String?
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parsed = contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                M3uParser.parse(reader, maxChannels = 15_000)
            }.orEmpty()
            if (parsed.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("فایل انتخاب‌شده شامل شبکه یا لینک استریم معتبر نبود.")
                )
            }
            val playlistName = fileName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                ?: "فهرست محلی M3U"
            val entities = parsed.map { item ->
                ChannelEntity(
                    playlistId = 0L,
                    name = item.name,
                    streamUrl = item.streamUrl,
                    logoUrl = item.logoUrl,
                    category = item.category
                )
            }
            dao.replacePlaylistAndChannels(
                playlist = PlaylistEntity(
                    name = playlistName,
                    sourceUrl = null,
                    isBuiltIn = false
                ),
                existingPlaylistId = null,
                channels = entities
            )
            Result.success(parsed.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSingleChannel(
        name: String,
        streamUrl: String,
        category: String,
        logoUrl: String?
    ): Result<Long> {
        val cleanName = name.trim()
        val cleanUrl = streamUrl.trim()
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("نام شبکه را وارد کنید."))
        }
        if (!M3uParser.isSupportedStreamUrl(cleanUrl)) {
            return Result.failure(
                IllegalArgumentException("نشانی استریم باید با https:// یا http:// شروع شود.")
            )
        }
        val existingManual = dao.getPlaylistByName("شبکه‌های دستی من")
        val playlistId = existingManual?.id ?: dao.insertPlaylist(
            PlaylistEntity(
                name = "شبکه‌های دستی من",
                sourceUrl = null,
                isBuiltIn = false
            )
        )
        val channelId = dao.insertChannel(
            ChannelEntity(
                playlistId = playlistId,
                name = cleanName,
                streamUrl = cleanUrl,
                logoUrl = logoUrl?.trim()?.takeIf { it.startsWith("http", ignoreCase = true) },
                category = category
            )
        )
        return Result.success(channelId)
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        dao.setFavorite(id, !currentFavorite)
    }

    suspend fun markWatched(id: Long) {
        dao.markWatched(id, System.currentTimeMillis())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun deleteChannel(channelId: Long) {
        dao.deleteChannel(channelId)
    }
}
