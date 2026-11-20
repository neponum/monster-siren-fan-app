package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.SongWithAlbum
import com.example.data.repository.LyricLine
import com.example.data.repository.MonsterSirenRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlaybackUiState(
    val currentSong: SongWithAlbum? = null,
    val queue: List<SongWithAlbum> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isPlayingOfflineFile: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val errorMessage: String? = null
)

class PlayerController(
    private val context: Context,
    private val repository: MonsterSirenRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null
    private var loadTrackJob: Job? = null
    private var pendingTrackStart: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionTicker() else stopPositionTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            val duration = mediaController?.duration?.takeIf { it > 0 } ?: _uiState.value.durationMs
            _uiState.update { it.copy(isBuffering = buffering, durationMs = duration) }

            if (playbackState == Player.STATE_ENDED) {
                handleTrackEnded()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer error: ${error.errorCodeName}", error)
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Не удалось воспроизвести трек. Проверьте сеть или скачайте его для офлайн-прослушивания."
                )
            }
        }
    }

    init {
        connectController()
        scope.launch {
            val savedShuffle = repository.preferences.shuffleEnabled.first()
            val savedRepeatOrdinal = repository.preferences.repeatMode.first()
            val savedRepeat = RepeatMode.entries.getOrElse(savedRepeatOrdinal) { RepeatMode.OFF }
            _uiState.update {
                it.copy(shuffleEnabled = savedShuffle, repeatMode = savedRepeat)
            }
        }
    }

    private fun connectController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    pendingTrackStart?.invoke()
                    pendingTrackStart = null
                }.onFailure { err ->
                    Log.e(TAG, "Failed connecting MediaController", err)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun playTrackList(tracks: List<SongWithAlbum>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _uiState.update {
            it.copy(
                queue = tracks,
                currentIndex = safeIndex,
                errorMessage = null
            )
        }
        loadAndPlayIndex(safeIndex)
    }

    fun playAtQueueIndex(index: Int) {
        val currentQueue = _uiState.value.queue
        if (index !in currentQueue.indices) return
        _uiState.update { it.copy(currentIndex = index, errorMessage = null) }
        loadAndPlayIndex(index)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE && _uiState.value.currentIndex >= 0) {
                loadAndPlayIndex(_uiState.value.currentIndex)
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceAtLeast(0L)
        controller.seekTo(clamped)
        _uiState.update { it.copy(positionMs = clamped) }
    }

    fun skipNext() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        val nextIndex = if (state.shuffleEnabled && state.queue.size > 1) {
            val candidates = state.queue.indices.filter { it != state.currentIndex }
            candidates.random()
        } else {
            val candidate = state.currentIndex + 1
            if (candidate <= state.queue.lastIndex) candidate else 0
        }
        _uiState.update { it.copy(currentIndex = nextIndex, errorMessage = null) }
        loadAndPlayIndex(nextIndex)
    }

    fun skipPrevious() {
        val controller = mediaController
        val state = _uiState.value
        if (state.queue.isEmpty()) return

        if (controller != null && controller.currentPosition > 3_500L) {
            controller.seekTo(0L)
            _uiState.update { it.copy(positionMs = 0L) }
            return
        }

        val prevIndex = if (state.currentIndex > 0) state.currentIndex - 1 else state.queue.lastIndex
        _uiState.update { it.copy(currentIndex = prevIndex, errorMessage = null) }
        loadAndPlayIndex(prevIndex)
    }

    fun toggleShuffle() {
        val next = !_uiState.value.shuffleEnabled
        _uiState.update { it.copy(shuffleEnabled = next) }
        scope.launch { repository.preferences.setShuffleEnabled(next) }
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
        scope.launch { repository.preferences.setRepeatMode(next.ordinal) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refreshCurrentSongMetadata() {
        val current = _uiState.value.currentSong ?: return
        scope.launch {
            val updated = repository.getSongWithAlbum(current.song.cid)
            if (updated != null) {
                _uiState.update { state ->
                    val updatedQueue = state.queue.map {
                        if (it.song.cid == updated.song.cid) updated else it
                    }
                    state.copy(currentSong = updated, queue = updatedQueue)
                }
            }
        }
    }

    private fun handleTrackEnded() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                mediaController?.seekTo(0L)
                mediaController?.play()
            }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                if (state.shuffleEnabled || state.currentIndex < state.queue.lastIndex) {
                    skipNext()
                } else {
                    _uiState.update { it.copy(isPlaying = false, positionMs = 0L) }
                }
            }
        }
    }

    private fun loadAndPlayIndex(index: Int) {
        val target = _uiState.value.queue.getOrNull(index) ?: return
        loadTrackJob?.cancel()
        loadTrackJob = scope.launch {
            _uiState.update {
                it.copy(
                    currentSong = target,
                    isBuffering = true,
                    positionMs = 0L,
                    durationMs = 0L,
                    errorMessage = null
                )
            }

            val resolved = repository.resolveSongForPlayback(target.song.cid)
            if (resolved == null) {
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "Трек «${target.song.name}» не скачан. Подключитесь к интернету или скачайте его для офлайн-прослушивания."
                    )
                }
                return@launch
            }

            repository.preferences.setLastSongCid(resolved.songWithAlbum.song.cid)

            _uiState.update { state ->
                val updatedQueue = state.queue.map {
                    if (it.song.cid == resolved.songWithAlbum.song.cid) resolved.songWithAlbum else it
                }
                state.copy(
                    currentSong = resolved.songWithAlbum,
                    queue = updatedQueue,
                    isPlayingOfflineFile = resolved.isOfflineFile,
                    lyrics = resolved.lyrics
                )
            }

            val startPlaybackAction = {
                val controller = mediaController
                if (controller != null) {
                    val artworkUri = resolved.songWithAlbum.albumLocalCoverPath
                        ?.takeIf { File(it).exists() }
                        ?.let { Uri.fromFile(File(it)) }
                        ?: resolved.songWithAlbum.albumCoverUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

                    val metadata = MediaMetadata.Builder()
                        .setTitle(resolved.songWithAlbum.song.name)
                        .setArtist(resolved.songWithAlbum.song.artistsJoined)
                        .setAlbumTitle(resolved.songWithAlbum.albumName ?: "Monster Siren Records")
                        .apply { if (artworkUri != null) setArtworkUri(artworkUri) }
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setMediaId(resolved.songWithAlbum.song.cid)
                        .setUri(resolved.mediaUri)
                        .setMediaMetadata(metadata)
                        .build()

                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                }
            }

            if (mediaController != null) {
                startPlaybackAction()
            } else {
                pendingTrackStart = startPlaybackAction
                if (controllerFuture == null) {
                    connectController()
                }
            }
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
                    _uiState.update { it.copy(positionMs = pos, durationMs = dur) }
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun release() {
        stopPositionTicker()
        loadTrackJob?.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    companion object {
        private const val TAG = "PlayerController"
    }
}
