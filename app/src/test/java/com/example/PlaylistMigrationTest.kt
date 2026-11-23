package com.example

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlbumEntity
import com.example.data.local.MonsterSirenDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Database(entities = [AlbumEntity::class, SongEntity::class], version = 1, exportSchema = false)
abstract class LegacyDatabase : RoomDatabase() {
    abstract fun dao(): LegacyDao
}

@Dao
interface LegacyDao {
    @Insert
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert
    suspend fun insertSong(song: SongEntity)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaylistMigrationTest {

    @Test
    fun migrationKeepsSongsAndCreatesPlaylists() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "playlist-migration-test.db"
        context.deleteDatabase(name)

        val legacy = Room.databaseBuilder(context, LegacyDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
        legacy.dao().insertAlbum(AlbumEntity(cid = "a1", name = "Album"))
        legacy.dao().insertSong(SongEntity(cid = "s1", albumCid = "a1", name = "Song"))
        legacy.close()

        val migrated = Room.databaseBuilder(context, MonsterSirenDatabase::class.java, name)
            .addMigrations(MonsterSirenDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val song = migrated.dao().getSongWithAlbum("s1")
        assertNotNull(song)
        assertEquals("Song", song?.song?.name)

        val playlistId = migrated.dao().insertPlaylist(PlaylistEntity(name = "My list"))
        migrated.dao().insertPlaylistSong(
            PlaylistSongEntity(playlistId = playlistId, songCid = "s1", position = 0)
        )
        assertEquals("My list", migrated.dao().getPlaylistById(playlistId)?.name)

            migrated.close()
            context.deleteDatabase(name)
        }
    }
}
