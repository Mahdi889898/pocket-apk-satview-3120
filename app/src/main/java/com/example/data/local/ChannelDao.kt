package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val sourceUrl: String?,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val channelCount: Int
)

@Dao
interface ChannelDao {
    @Query("SELECT COUNT(*) FROM channels")
    fun observeTotalChannelCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM channels
        WHERE (:category = 'همه' OR category = :category)
          AND (:query = '' OR name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        """
    )
    fun observeFilteredChannelCount(query: String, category: String): Flow<Int>

    @Query(
        """
        SELECT * FROM channels
        WHERE (:category = 'همه' OR category = :category)
          AND (:query = '' OR name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY isFavorite DESC, id ASC
        LIMIT :limit
        """
    )
    fun observeFilteredChannels(query: String, category: String, limit: Int): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY id ASC")
    fun observeFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE lastWatchedAt > 0 ORDER BY lastWatchedAt DESC LIMIT 12")
    fun observeRecentChannels(): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT * FROM channels
        ORDER BY CASE WHEN category = :category THEN 0 ELSE 1 END, isFavorite DESC, id ASC
        LIMIT :limit
        """
    )
    fun observeQuickZapChannels(category: String, limit: Int = 35): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    fun observeChannelById(id: Long): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Query(
        """
        SELECT p.id, p.name, p.sourceUrl, p.isBuiltIn, p.createdAt, COUNT(c.id) AS channelCount
        FROM playlists p
        LEFT JOIN channels c ON p.id = c.playlistId
        GROUP BY p.id
        ORDER BY p.isBuiltIn ASC, p.createdAt DESC
        """
    )
    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun countChannels(): Int

    @Query("SELECT COUNT(*) FROM channels WHERE isSample = 1")
    suspend fun countSampleChannels(): Int

    @Query("SELECT COUNT(*) FROM channels WHERE isSample = 0")
    suspend fun countNonSampleChannels(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity): Long

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatchedAt = :timestamp WHERE id = :id")
    suspend fun markWatched(id: Long, timestamp: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: Long)

    @Query("SELECT * FROM playlists WHERE isBuiltIn = 1 LIMIT 1")
    suspend fun getBuiltInPlaylist(): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE name = :name AND isBuiltIn = 0 LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE sourceUrl = :sourceUrl LIMIT 1")
    suspend fun getPlaylistBySourceUrl(sourceUrl: String): PlaylistEntity?

    @Transaction
    suspend fun replacePlaylistAndChannels(
        playlist: PlaylistEntity,
        existingPlaylistId: Long?,
        channels: List<ChannelEntity>
    ): Long {
        if (existingPlaylistId != null) {
            deletePlaylist(existingPlaylistId)
        }
        val newPlaylistId = insertPlaylist(playlist)
        val withId = channels.map { it.copy(playlistId = newPlaylistId) }
        withId.chunked(1000).forEach { batch ->
            insertChannels(batch)
        }
        return newPlaylistId
    }
}
