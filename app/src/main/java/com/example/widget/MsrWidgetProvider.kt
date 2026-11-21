package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.MonsterSirenApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Compact 4x2 home-screen music player: artwork, track/artist and
 * previous / play-pause / next controls. Actions are forwarded to the
 * app's PlayerController, which restores its queue on demand.
 */
class MsrWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.widget.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.widget.ACTION_PREVIOUS"
        const val ACTION_REFRESH = "com.example.widget.ACTION_REFRESH"

        private const val ARTWORK_TARGET_PX = 256

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MsrWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val state = WidgetStateStore.load(context)
            val views = RemoteViews(context.packageName, R.layout.msr_widget)
            val hasSong = state.title.isNotBlank()

            views.setTextViewText(
                R.id.widget_title,
                state.title.ifBlank { context.getString(R.string.app_name) }.uppercase()
            )
            views.setTextViewText(
                R.id.widget_artist,
                state.artist
                    .ifBlank { context.getString(R.string.widget_default_artist) }
                    .uppercase()
            )
            views.setTextViewText(
                R.id.widget_status,
                context.getString(
                    when {
                        !hasSong -> R.string.widget_status_idle
                        state.isPlaying -> R.string.widget_status_playing
                        else -> R.string.widget_status_paused
                    }
                )
            )

            val artwork = loadArtwork(state.artworkPath)
            if (artwork != null) {
                views.setImageViewBitmap(R.id.widget_artwork, artwork)
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_msr_cover_placeholder)
            }

            views.setImageViewResource(
                R.id.widget_toggle,
                if (state.isPlaying) R.drawable.ic_widget_pause_dark
                else R.drawable.ic_widget_play_dark
            )
            views.setContentDescription(
                R.id.widget_toggle,
                context.getString(
                    if (state.isPlaying) R.string.widget_pause else R.string.widget_play
                )
            )

            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                actionPendingIntent(context, ACTION_PREVIOUS)
            )
            views.setOnClickPendingIntent(
                R.id.widget_toggle,
                actionPendingIntent(context, ACTION_TOGGLE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                actionPendingIntent(context, ACTION_NEXT)
            )

            manager.updateAppWidget(appWidgetId, views)
        }

        private fun actionPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MsrWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openAppPendingIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openAppIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun openAppIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        private fun loadArtwork(path: String?): Bitmap? {
            if (path.isNullOrBlank()) return null
            val file = File(path)
            if (!file.exists()) return null
            return runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)

                var sampleSize = 1
                var width = bounds.outWidth
                var height = bounds.outHeight
                while (width / (sampleSize * 2) >= ARTWORK_TARGET_PX &&
                    height / (sampleSize * 2) >= ARTWORK_TARGET_PX
                ) {
                    sampleSize *= 2
                    width = bounds.outWidth / sampleSize
                    height = bounds.outHeight / sampleSize
                }

                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeFile(path, options)
            }.getOrNull()
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE, ACTION_NEXT, ACTION_PREVIOUS -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                    try {
                        runCatching { handleAction(context, intent.action.orEmpty()) }
                        updateAll(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_REFRESH -> updateAll(context)
        }
    }

    private suspend fun handleAction(context: Context, action: String) {
        val controller = (context.applicationContext as? MonsterSirenApplication)
            ?.playerController
        if (controller == null) {
            context.startActivity(openAppIntent(context))
            return
        }

        controller.awaitQueueRestored()
        val state = controller.uiState.value
        if (state.queue.isEmpty() && state.currentSong == null) {
            context.startActivity(openAppIntent(context))
            return
        }

        when (action) {
            ACTION_TOGGLE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_PREVIOUS -> controller.skipPrevious()
        }
    }
}
