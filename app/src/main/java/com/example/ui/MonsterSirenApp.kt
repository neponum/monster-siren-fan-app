package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.components.FanDisclaimerDialog
import com.example.ui.components.MsrNavMenuItem
import com.example.ui.components.MsrPersistentMiniPlayer
import com.example.ui.components.MsrSlideOverNavMenu
import com.example.ui.components.UpdateDialog
import com.example.ui.components.WhatsNewDialog
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.FullPlayerOverlay
import com.example.ui.screens.MsrContactScreen
import com.example.ui.screens.MsrInfoScreen
import com.example.ui.screens.MsrPlaylistScreen
import com.example.ui.screens.MsrSlideOverPlaylistDrawer
import com.example.ui.screens.MusicCatalogScreen
import com.example.ui.theme.MsrBackground
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSurfaceElevated
import com.example.ui.theme.MsrWhite
import com.example.ui.viewmodel.AlbumDetailUiState
import com.example.ui.viewmodel.MainViewModel

const val ROUTE_MUSIC = "music"
const val ROUTE_PLAYLIST = "playlist"
const val ROUTE_INFO = "info"
const val ROUTE_CONTACT = "contact"
const val ROUTE_ALBUM_DETAIL = "album/{albumCid}"

@Composable
fun MonsterSirenApp(
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_MUSIC

    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val offlineVaultState by viewModel.offlineVaultState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val disclaimerAccepted by viewModel.disclaimerAccepted.collectAsStateWithLifecycle()
    val whatsNew by viewModel.whatsNew.collectAsStateWithLifecycle()

    var showNavDrawer by rememberSaveable { mutableStateOf(false) }
    var showFullPlayer by rememberSaveable { mutableStateOf(false) }
    var showPlaylistDrawer by rememberSaveable { mutableStateOf(false) }
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Captures live underlying screen pixels for the left-70% frosted-blur navigation drawer
    val backdropLayer = rememberGraphicsLayer()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(playbackState.currentSong?.song?.cid) {
        if (playbackState.currentSong != null && !askedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askedNotificationPermission = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        val err = playbackState.errorMessage
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            viewModel.playerController.clearError()
        }
    }

    val openAlbum: (String) -> Unit = { albumCid ->
        navController.navigate("album/$albumCid")
    }

    val openNavDrawer: () -> Unit = {
        showNavDrawer = true
    }

    val navigateTopLevel: (String) -> Unit = { route ->
        showNavDrawer = false
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Exact 4 requested menu tabs: MUSIC, PLAYLIST, INFO, CONTACT
    val navMenuItems = listOf(
        MsrNavMenuItem(
            route = ROUTE_MUSIC,
            primaryLabel = "MUSIC",
            secondaryLabel = stringResource(R.string.menu_sub_music)
        ),
        MsrNavMenuItem(
            route = ROUTE_PLAYLIST,
            primaryLabel = "PLAYLIST",
            secondaryLabel = stringResource(R.string.menu_sub_playlist),
            badgeText = offlineVaultState.downloadedSongs.size.takeIf { it > 0 }?.toString()
        ),
        MsrNavMenuItem(
            route = ROUTE_INFO,
            primaryLabel = "INFO",
            secondaryLabel = stringResource(R.string.menu_sub_info)
        ),
        MsrNavMenuItem(
            route = ROUTE_CONTACT,
            primaryLabel = "CONTACT",
            secondaryLabel = stringResource(R.string.menu_sub_contact)
        )
    )

    // Cycle between Repeat All -> Repeat One -> Shuffle cleanly from the single bottom-left mode button
    val cyclePlaybackMode: () -> Unit = {
        val st = playbackState
        when {
            st.shuffleEnabled -> {
                viewModel.playerController.toggleShuffle()
            }
            st.repeatMode == com.example.player.RepeatMode.ONE -> {
                viewModel.playerController.cycleRepeatMode()
                viewModel.playerController.toggleShuffle()
            }
            else -> {
                viewModel.playerController.cycleRepeatMode()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MsrBackground)
    ) {
        Scaffold(
            containerColor = MsrBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MsrSurfaceElevated,
                        contentColor = MsrWhite,
                        actionColor = MsrPureWhite
                    )
                }
            },
            bottomBar = {
                // Persistent #1D1D1D Monster Siren mini-player bar (always remains visible below the nav drawer)
                MsrPersistentMiniPlayer(
                    playbackState = playbackState,
                    onOpenFullPlayer = {
                        if (playbackState.currentSong != null) {
                            showNavDrawer = false
                            showFullPlayer = true
                        }
                    },
                    onTogglePlayPause = viewModel.playerController::togglePlayPause,
                    onOpenPlaylistDrawer = {
                        showNavDrawer = false
                        showPlaylistDrawer = true
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Record underlying NavHost into backdropLayer so the left-70% nav menu renders a real frosted blur
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            backdropLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(backdropLayer)
                        }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_MUSIC,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(ROUTE_MUSIC) {
                            MusicCatalogScreen(
                                state = catalogState,
                                playbackState = playbackState,
                                isMenuOpen = showNavDrawer,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onToggleOfflineOnly = viewModel::toggleOfflineOnlyMode,
                                onSyncCatalog = { viewModel.syncCatalog(showFeedback = true) },
                                onOpenMenu = openNavDrawer,
                                onAlbumClick = openAlbum,
                                onPlaySongs = viewModel::playSongs,
                                onDownloadAlbum = viewModel::downloadAlbum,
                                onDownloadSong = viewModel::downloadSong,
                                onCancelDownload = viewModel::cancelDownload,
                                onDeleteSongDownload = viewModel::deleteSongDownload
                            )
                        }

                        composable(ROUTE_PLAYLIST) {
                            MsrPlaylistScreen(
                                catalogState = catalogState,
                                offlineState = offlineVaultState,
                                playbackState = playbackState,
                                playlists = playlists,
                                isMenuOpen = showNavDrawer,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onOpenMenu = openNavDrawer,
                                onPlaySongs = viewModel::playSongs,
                                onPlayQueueIndex = viewModel.playerController::playAtQueueIndex,
                                onToggleOfflineOnly = viewModel::toggleOfflineOnlyMode,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onDownloadSong = viewModel::downloadSong,
                                onCancelDownload = viewModel::cancelDownload,
                                onDeleteSongDownload = viewModel::deleteSongDownload,
                                onClearAllDownloads = viewModel::clearAllDownloads,
                                observePlaylistSongs = viewModel::observePlaylistSongs,
                                onCreatePlaylist = viewModel::createPlaylist,
                                onCreatePlaylistWithSong = viewModel::createPlaylistWithSong,
                                onRenamePlaylist = viewModel::renamePlaylist,
                                onDeletePlaylist = viewModel::deletePlaylist,
                                onAddSongToPlaylist = viewModel::addSongToPlaylist,
                                onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist
                            )
                        }

                        composable(ROUTE_INFO) {
                            MsrInfoScreen(
                                catalogState = catalogState,
                                offlineState = offlineVaultState,
                                newsState = newsState,
                                isMenuOpen = showNavDrawer,
                                onOpenMenu = openNavDrawer,
                                onSyncCatalog = { viewModel.syncCatalog(showFeedback = true) },
                                onToggleOfflineOnly = viewModel::toggleOfflineOnlyMode,
                                onRefreshNews = viewModel::loadNews,
                                onOpenNewsDetail = viewModel::openNewsDetail,
                                onCloseNewsDetail = viewModel::closeNewsDetail,
                                updateState = updateState,
                                onCheckUpdates = { viewModel.checkForUpdates(manual = true) },
                                onShowWhatsNew = viewModel::showWhatsNew
                            )
                        }

                        composable(ROUTE_CONTACT) {
                            MsrContactScreen(
                                isMenuOpen = showNavDrawer,
                                onOpenMenu = openNavDrawer
                            )
                        }

                        composable(
                            route = ROUTE_ALBUM_DETAIL,
                            arguments = listOf(navArgument("albumCid") { type = NavType.StringType })
                        ) { entry ->
                            val albumCid = entry.arguments?.getString("albumCid") ?: return@composable
                            val albumFlow = remember(albumCid) { viewModel.observeAlbumDetail(albumCid) }
                            val albumDetailState by albumFlow.collectAsStateWithLifecycle(
                                initialValue = AlbumDetailUiState()
                            )
                            AlbumDetailScreen(
                                albumCid = albumCid,
                                state = albumDetailState,
                                playbackState = playbackState,
                                isMenuOpen = showNavDrawer,
                                onLoadAlbumDetail = viewModel::loadAlbumDetail,
                                onBack = { navController.popBackStack() },
                                onOpenMenu = openNavDrawer,
                                onPlaySongs = viewModel::playSongs,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onDownloadAlbum = viewModel::downloadAlbum,
                                onDeleteAlbumDownloads = viewModel::deleteAlbumDownloads,
                                onDownloadSong = viewModel::downloadSong,
                                onCancelDownload = viewModel::cancelDownload,
                                onDeleteSongDownload = viewModel::deleteSongDownload
                            )
                        }
                    }
                }

                // Left-Side Frosted-Glass Navigation Drawer Overlay (sits directly above the Mini-Player)
                MsrSlideOverNavMenu(
                    visible = showNavDrawer,
                    activeRoute = currentRoute,
                    items = navMenuItems,
                    backdropLayer = backdropLayer,
                    onSelectRoute = navigateTopLevel,
                    onDismiss = { showNavDrawer = false }
                )
            }
        }

        // Slide-over Playlist Drawer when opened directly from the persistent Mini-Player list icon
        if (!showFullPlayer) {
            MsrSlideOverPlaylistDrawer(
                visible = showPlaylistDrawer,
                playbackState = playbackState,
                allSongsFallback = catalogState.songs,
                onDismiss = { showPlaylistDrawer = false },
                onSelectQueueIndex = { idx ->
                    viewModel.playerController.playAtQueueIndex(idx)
                    showPlaylistDrawer = false
                },
                onSelectFallbackSong = { songs, idx ->
                    viewModel.playSongs(songs, idx)
                    showPlaylistDrawer = false
                }
            )
        }

        // Full-Screen Vinyl Player Overlay (Screenshot 5 + Screenshot 3 drawer inside)
        AnimatedVisibility(
            visible = showFullPlayer && playbackState.currentSong != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FullPlayerOverlay(
                playbackState = playbackState,
                allSongsFallback = catalogState.songs,
                showPlaylistDrawer = showPlaylistDrawer,
                onShowPlaylistDrawerChange = { showPlaylistDrawer = it },
                onDismiss = {
                    showPlaylistDrawer = false
                    showFullPlayer = false
                },
                onTogglePlayPause = viewModel.playerController::togglePlayPause,
                onSkipNext = viewModel.playerController::skipNext,
                onSkipPrevious = viewModel.playerController::skipPrevious,
                onSeekTo = viewModel.playerController::seekTo,
                onCyclePlaybackMode = cyclePlaybackMode,
                onPlayQueueIndex = viewModel.playerController::playAtQueueIndex,
                onPlayFallbackSongs = viewModel::playSongs,
                onToggleFavorite = viewModel::toggleFavorite,
                onDownloadSong = viewModel::downloadSong,
                onCancelDownload = viewModel::cancelDownload,
                onDeleteDownload = viewModel::deleteSongDownload
            )
        }

        if (!disclaimerAccepted) {
            FanDisclaimerDialog(onAccept = viewModel::dismissDisclaimer)
        }

        whatsNew?.let { notes ->
            WhatsNewDialog(
                version = com.example.BuildConfig.VERSION_NAME,
                notes = notes,
                onDismiss = viewModel::dismissWhatsNew
            )
        }

        UpdateDialog(
            state = updateState,
            onDownload = viewModel::downloadAndInstallUpdate,
            onDismiss = viewModel::dismissUpdate,
            onDismissMessage = viewModel::consumeUpdateMessage
        )
    }
}
