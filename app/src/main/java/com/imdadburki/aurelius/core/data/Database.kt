package com.imdadburki.aurelius.core.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** A track in the local library or a YouTube result kept in the queue/history. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String? = null,
    val isLocal: Boolean = true,
    val youtubeId: String? = null,
    val addedAtSec: Long = 0,
    /** Parent folder name derived from the file path (e.g. "Aurelius", "Download"). */
    val folderName: String = ""
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaId"],
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val mediaId: String,
    val position: Int,
    val title: String,
    val artist: String,
    val uri: String,
    val durationMs: Long,
    val artworkUri: String? = null
)

@Entity(tableName = "favorites", primaryKeys = ["mediaId"])
data class FavoriteEntity(
    val mediaId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history", primaryKeys = ["mediaId"])
data class PlayHistoryEntity(
    val mediaId: String,
    val title: String,
    val artist: String,
    val uri: String,
    val artworkUri: String? = null,
    val playedAt: Long = System.currentTimeMillis(),
    val lastPositionMs: Long = 0
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun insertPlaylist(p: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(p: PlaylistEntity)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :pid ORDER BY position ASC")
    fun tracks(pid: Long): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(t: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :pid AND mediaId = :mid")
    suspend fun removeTrack(pid: Long, mid: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :pid")
    suspend fun clearTracks(pid: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :pid")
    suspend fun maxPosition(pid: Long): Int
}

@Dao
interface FavoriteDao {
    @Query("SELECT mediaId FROM favorites")
    fun favoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(f: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mid")
    suspend fun remove(mid: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 50")
    fun recent(): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(h: PlayHistoryEntity)

    @Query("DELETE FROM play_history")
    suspend fun clear()
}

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, FavoriteEntity::class, PlayHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AureliusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        fun create(context: Context): AureliusDatabase =
            Room.databaseBuilder(context, AureliusDatabase::class.java, "aurelius.db")
                .build()
    }
}
