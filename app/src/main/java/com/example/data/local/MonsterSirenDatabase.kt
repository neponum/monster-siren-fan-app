package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AlbumEntity::class, SongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MonsterSirenDatabase : RoomDatabase() {
    abstract fun dao(): MonsterSirenDao

    companion object {
        @Volatile
        private var INSTANCE: MonsterSirenDatabase? = null

        fun getInstance(context: Context): MonsterSirenDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MonsterSirenDatabase::class.java,
                    "monster_siren_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
