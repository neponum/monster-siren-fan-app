package com.example

import android.app.Application
import com.example.data.local.MonsterSirenDatabase
import com.example.data.local.UserPreferences
import com.example.data.remote.NetworkModule
import com.example.data.repository.MonsterSirenRepository
import com.example.data.repository.OfflineDownloadManager
import com.example.player.PlayerController

class MonsterSirenApplication : Application() {

    lateinit var database: MonsterSirenDatabase
        private set

    lateinit var preferences: UserPreferences
        private set

    lateinit var downloadManager: OfflineDownloadManager
        private set

    lateinit var repository: MonsterSirenRepository
        private set

    lateinit var playerController: PlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        database = MonsterSirenDatabase.getInstance(this)
        preferences = UserPreferences(this)
        downloadManager = OfflineDownloadManager(
            context = this,
            api = NetworkModule.api,
            okHttpClient = NetworkModule.okHttpClient,
            dao = database.dao()
        )
        repository = MonsterSirenRepository(
            context = this,
            api = NetworkModule.api,
            dao = database.dao(),
            downloadManager = downloadManager,
            preferences = preferences
        )
        playerController = PlayerController(
            context = this,
            repository = repository
        )
    }
}
