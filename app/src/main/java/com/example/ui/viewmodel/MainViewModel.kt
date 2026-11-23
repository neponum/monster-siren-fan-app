package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.R
import com.example.data.local.AlbumWithStats
import com.example.data.local.PlaylistWithCount
import com.example.data.local.SongWithAlbum
import com.example.data.remote.NewsDetailDto
import com.example.data.remote.NewsSummaryDto
import com.example.data.repository.MonsterSirenRepository
import com.example.data.update.AppUpdate
import com.example.data.update.UpdateManager
import com.example.player.PlaybackUiState
import com.example.player.PlayerController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val albums: List<AlbumWithStats> = emptyList(),
    val featuredAlbums: List<AlbumWithStats> = emptyList(),
    val songs: List<SongWithAlbum> = emptyList(),
    val downloadedCount: Int = 0,
    val totalOfflineBytes: Long = 0L,
    val offlineOnlyMode: Boolean = false,
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val isNetworkConnected: Boolean = true
)

data class OfflineVaultUiState(
    val downloadedSongs: List<SongWithAlbum> = emptyList(),
    val downloadedAlbums: List<AlbumWithStats> = emptyList(),
    val favoriteSongs: List<SongWithAlbum> = emptyList(),
    val activeDownloads: List<SongWithAlbum> = emptyList(),
    val totalOfflineBytes: Long = 0L
)

data class AlbumDetailUiState(
    val album: AlbumWithStats? = null,
    val songs: List<SongWithAlbum> = emptyList()
)

data class NewsUiState(
    val items: List<NewsSummaryDto> = emptyList(),
    val isLoading: Boolean = false,
    val selectedDetail: NewsDetailDto? = null,
    val isLoadingDetail: Boolean = false
)

data class UpdateUiState(
    val update: AppUpdate? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val infoMessage: String? = null,
    val error: String? = null
)

class MainViewModel(
    private val repository: MonsterSirenRepository,
    val playerController: PlayerController,
    private val appContext: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSyncing = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _newsState = MutableStateFlow(NewsUiState())
    val newsState: StateFlow<NewsUiState> = _newsState.asStateFlow()

    private val updateManager = UpdateManager(appContext)

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _whatsNew = MutableStateFlow<String?>(null)
    val whatsNew: StateFlow<String?> = _whatsNew.asStateFlow()

    val disclaimerAccepted: StateFlow<Boolean> = repository.preferences.disclaimerAccepted
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val playlists: StateFlow<List<PlaylistWithCount>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState

    private data class SyncUiMeta(
        val searchQuery: String,
        val offlineOnly: Boolean,
        val isSyncing: Boolean,
        val syncError: String?
    )

    private val syncMetaFlow = combine(
        _searchQuery,
        repository.preferences.offlineOnlyMode,
        _isSyncing,
        _syncError
    ) { query, offlineOnly, syncing, err ->
        SyncUiMeta(query, offlineOnly, syncing, err)
    }

    val catalogState: StateFlow<CatalogUiState> = combine(
        repository.allAlbums,
        repository.allSongs,
        repository.downloadedSongCount,
        repository.totalOfflineBytes,
        syncMetaFlow
    ) { allAlbums, allSongs, downloadedCount, totalBytes, meta ->
        val query = meta.searchQuery.trim().lowercase()

        val filteredAlbums = allAlbums.filter { item ->
            val matchesOffline = !meta.offlineOnly || item.hasAnyDownloaded
            val matchesQuery = query.isEmpty() ||
                item.album.name.lowercase().contains(query) ||
                item.album.artistsJoined.lowercase().contains(query) ||
                item.album.cid.contains(query)
            matchesOffline && matchesQuery
        }

        val filteredSongs = allSongs.filter { item ->
            val matchesOffline = !meta.offlineOnly || item.isDownloaded
            val matchesQuery = query.isEmpty() ||
                item.song.name.lowercase().contains(query) ||
                item.song.artistsJoined.lowercase().contains(query) ||
                (item.albumName?.lowercase()?.contains(query) == true)
            matchesOffline && matchesQuery
        }

        val featured = (if (meta.offlineOnly) filteredAlbums else allAlbums).take(6)

        CatalogUiState(
            albums = filteredAlbums,
            featuredAlbums = featured,
            songs = filteredSongs,
            downloadedCount = downloadedCount,
            totalOfflineBytes = totalBytes,
            offlineOnlyMode = meta.offlineOnly,
            searchQuery = meta.searchQuery,
            isSyncing = meta.isSyncing,
            syncError = meta.syncError,
            isNetworkConnected = repository.isNetworkAvailable()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState(isSyncing = true)
    )

    val offlineVaultState: StateFlow<OfflineVaultUiState> = combine(
        repository.downloadedSongs,
        repository.allAlbums,
        repository.favoriteSongs,
        repository.activeDownloads,
        repository.totalOfflineBytes
    ) { downloadedSongs, allAlbums, favorites, activeDownloads, totalBytes ->
        OfflineVaultUiState(
            downloadedSongs = downloadedSongs,
            downloadedAlbums = allAlbums.filter { it.hasAnyDownloaded },
            favoriteSongs = favorites,
            activeDownloads = activeDownloads,
            totalOfflineBytes = totalBytes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OfflineVaultUiState()
    )

    init {
        syncCatalog(showFeedback = false)
        loadNews()
        checkForUpdates(manual = false)
        loadWhatsNewIfNeeded()
    }

    private fun loadWhatsNewIfNeeded() {
        viewModelScope.launch {
            val currentVersion = BuildConfig.VERSION_NAME
            val lastSeen = repository.preferences.lastSeenVersion.firstOrNull()
            if (lastSeen == currentVersion) return@launch

            val accepted = repository.preferences.disclaimerAccepted.firstOrNull() ?: false
            if (lastSeen == null && !accepted) {
                // Fresh install: the fan disclaimer is enough for the first launch.
                repository.preferences.setLastSeenVersion(currentVersion)
                return@launch
            }

            val notes = readChangelog()

            if (notes.isNullOrBlank()) {
                repository.preferences.setLastSeenVersion(currentVersion)
            } else {
                _whatsNew.value = notes
            }
        }
    }

    fun showWhatsNew() {
        _whatsNew.value = readChangelog()
    }

    private fun readChangelog(): String? = runCatching {
        appContext.resources.openRawResource(R.raw.changelog)
            .bufferedReader()
            .use { it.readText() }
            .trim()
    }.getOrNull()?.takeIf { it.isNotBlank() }

    fun dismissWhatsNew() {
        _whatsNew.value = null
        viewModelScope.launch {
            repository.preferences.setLastSeenVersion(BuildConfig.VERSION_NAME)
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        if (_updateState.value.isChecking) return
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isChecking = true,
                infoMessage = null,
                error = null
            )
            val result = updateManager.check()
            result.onSuccess { available ->
                val dismissedTag = repository.preferences.updateDismissedTag.firstOrNull()
                val shouldShow = available != null &&
                    (manual || available.tag != dismissedTag)
                _updateState.value = if (available != null && shouldShow) {
                    _updateState.value.copy(
                        isChecking = false,
                        update = available,
                        infoMessage = null
                    )
                } else {
                    _updateState.value.copy(
                        isChecking = false,
                        infoMessage = if (manual && available == null) "Установлена последняя версия" else null
                    )
                }
            }.onFailure {
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    error = if (manual) "Не удалось проверить обновления" else null
                )
            }
        }
    }

    fun dismissUpdate() {
        viewModelScope.launch {
            _updateState.value.update?.let { repository.preferences.setUpdateDismissedTag(it.tag) }
            _updateState.value = _updateState.value.copy(update = null, isDownloading = false)
        }
    }

    fun downloadAndInstallUpdate() {
        val update = _updateState.value.update ?: return
        if (_updateState.value.isDownloading) return
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isDownloading = true,
                progress = 0,
                error = null
            )
            runCatching {
                val apk = updateManager.download(update) { progress ->
                    _updateState.value = _updateState.value.copy(progress = progress)
                }
                updateManager.install(apk)
            }.onFailure { error ->
                _updateState.value = _updateState.value.copy(
                    isDownloading = false,
                    error = error.message ?: "Не удалось установить обновление"
                )
            }.onSuccess {
                _updateState.value = _updateState.value.copy(
                    isDownloading = false,
                    progress = 100
                )
            }
        }
    }

    fun dismissDisclaimer() {
        viewModelScope.launch {
            repository.preferences.setDisclaimerAccepted(true)
        }
    }

    fun consumeUpdateMessage() {
        _updateState.value = _updateState.value.copy(infoMessage = null, error = null)
    }

    fun loadNews() {
        if (_newsState.value.isLoading) return
        viewModelScope.launch {
            _newsState.value = _newsState.value.copy(isLoading = true)
            val result = repository.fetchNews()
            _newsState.value = _newsState.value.copy(
                isLoading = false,
                items = result.getOrElse { _newsState.value.items }
            )
        }
    }

    fun openNewsDetail(cid: String) {
        viewModelScope.launch {
            _newsState.value = _newsState.value.copy(isLoadingDetail = true)
            val result = repository.fetchNewsDetail(cid)
            result.onSuccess { detail ->
                _newsState.value = _newsState.value.copy(
                    isLoadingDetail = false,
                    selectedDetail = detail
                )
            }.onFailure {
                _newsState.value = _newsState.value.copy(isLoadingDetail = false)
                _snackbarMessage.value = "Не удалось загрузить публикацию без интернета"
            }
        }
    }

    fun closeNewsDetail() {
        _newsState.value = _newsState.value.copy(selectedDetail = null, isLoadingDetail = false)
    }

    fun syncCatalog(showFeedback: Boolean = true) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            val result = repository.syncCatalog()
            _isSyncing.value = false
            result.onFailure { err ->
                val message = err.message ?: "Не удалось обновить каталог"
                _syncError.value = message
                if (showFeedback) {
                    _snackbarMessage.value = message
                }
            }.onSuccess {
                if (showFeedback) {
                    _snackbarMessage.value = "Каталог Monster Siren обновлён"
                }
            }
        }
    }

    fun observeAlbumDetail(albumCid: String): Flow<AlbumDetailUiState> {
        return combine(
            repository.observeAlbum(albumCid),
            repository.observeSongsByAlbum(albumCid)
        ) { album, songs ->
            AlbumDetailUiState(album = album, songs = songs)
        }
    }

    fun observePlaylistSongs(playlistId: Long): Flow<List<SongWithAlbum>> =
        repository.observePlaylistSongs(playlistId)

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            runCatching { repository.createPlaylist(name) }
                .onSuccess { _snackbarMessage.value = "Плейлист «${name.trim()}» создан" }
                .onFailure { _snackbarMessage.value = it.message ?: "Не удалось создать плейлист" }
        }
    }

    fun createPlaylistWithSong(name: String, songCid: String) {
        viewModelScope.launch {
            runCatching {
                val playlistId = repository.createPlaylist(name)
                repository.addSongToPlaylist(playlistId, songCid)
            }.onSuccess {
                _snackbarMessage.value = "Плейлист «${name.trim()}» создан"
            }.onFailure {
                _snackbarMessage.value = it.message ?: "Не удалось создать плейлист"
            }
        }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, name)
            _snackbarMessage.value = "Плейлист переименован"
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _snackbarMessage.value = "Плейлист удалён"
        }
    }

    fun addSongToPlaylist(playlistId: Long, songCid: String) {
        viewModelScope.launch {
            val added = repository.addSongToPlaylist(playlistId, songCid)
            _snackbarMessage.value = if (added) {
                "Трек добавлен в плейлист"
            } else {
                "Трек уже есть в этом плейлисте"
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songCid: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songCid)
            _snackbarMessage.value = "Трек убран из плейлиста"
        }
    }

    fun loadAlbumDetail(albumCid: String) {
        viewModelScope.launch {
            repository.loadAlbumDetail(albumCid)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleOfflineOnlyMode() {
        viewModelScope.launch {
            val next = !catalogState.value.offlineOnlyMode
            repository.preferences.setOfflineOnlyMode(next)
            _snackbarMessage.value = if (next) {
                "Режим «Только офлайн» включён"
            } else {
                "Показан полный каталог Monster Siren"
            }
        }
    }

    fun playSongs(songs: List<SongWithAlbum>, startIndex: Int = 0) {
        val offlineOnly = catalogState.value.offlineOnlyMode
        val targetList = if (offlineOnly) {
            val clicked = songs.getOrNull(startIndex)
            val downloadedOnly = songs.filter { it.isDownloaded }
            if (downloadedOnly.isEmpty()) {
                _snackbarMessage.value = "Нет скачанных треков для прослушивания без интернета"
                return
            }
            val newStart = clicked?.let { c -> downloadedOnly.indexOfFirst { it.song.cid == c.song.cid } }
                ?.takeIf { it >= 0 } ?: 0
            playerController.playTrackList(downloadedOnly, newStart)
            return
        } else {
            songs
        }
        playerController.playTrackList(targetList, startIndex)
    }

    fun playAlbum(albumCid: String) {
        viewModelScope.launch {
            repository.loadAlbumDetail(albumCid)
            val songs = catalogState.value.songs.filter { it.song.albumCid == albumCid }
                .sortedBy { it.song.trackIndex }
            if (songs.isNotEmpty()) {
                playSongs(songs, 0)
            } else {
                _snackbarMessage.value = "Загрузка треков альбома…"
            }
        }
    }

    fun downloadSong(songCid: String) {
        if (!repository.isNetworkAvailable()) {
            _snackbarMessage.value = "Для загрузки трека необходимо подключение к интернету"
            return
        }
        repository.downloadManager.downloadSong(songCid)
    }

    fun cancelDownload(songCid: String) {
        repository.downloadManager.cancelDownload(songCid)
    }

    fun deleteSongDownload(songCid: String) {
        viewModelScope.launch {
            repository.downloadManager.deleteSongDownload(songCid)
            playerController.refreshCurrentSongMetadata()
            _snackbarMessage.value = "Офлайн-копия трека удалена"
        }
    }

    fun downloadAlbum(albumCid: String) {
        if (!repository.isNetworkAvailable()) {
            _snackbarMessage.value = "Для загрузки альбома необходимо подключение к интернету"
            return
        }
        repository.downloadManager.downloadAlbum(albumCid)
        _snackbarMessage.value = "Загрузка альбома для офлайн-прослушивания…"
    }

    fun deleteAlbumDownloads(albumCid: String) {
        viewModelScope.launch {
            repository.downloadManager.deleteAlbumDownloads(albumCid)
            playerController.refreshCurrentSongMetadata()
            _snackbarMessage.value = "Кэш альбома очищен"
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.downloadManager.clearAllDownloads()
            playerController.refreshCurrentSongMetadata()
            _snackbarMessage.value = "Все офлайн-файлы удалены"
        }
    }

    fun toggleFavorite(songCid: String) {
        viewModelScope.launch {
            repository.toggleFavorite(songCid)
            playerController.refreshCurrentSongMetadata()
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val repository: MonsterSirenRepository,
        private val playerController: PlayerController,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, playerController, appContext) as T
        }
    }
}
