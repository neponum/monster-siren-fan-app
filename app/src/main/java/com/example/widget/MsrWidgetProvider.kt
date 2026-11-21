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
import java.io.File

/**
 * Home-screen widget: artwork, title/artist and previous/play-pause/next controls.
 * It reads a small snapshot stored by [com.example.player.PlayerController] and
 * forwards control actions to the app's in-process player controller.
 */
class MsrWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.widget.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.widget.ACTION_PREVIOUS"
        const val ACTION_REFRESH = "com.example.widget.ACTION_REFRESH"

        private const val ARTWORK_TARGET_PX = 128

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MsrWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id -> updateWidget(context, manager, id) }
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
            views.setInt(
                R.id.widget_accent,
                "setBackgroundColor",
                if (state.isPlaying) 0xFFFFFFFF.toInt() else 0xFF4A4A4A.toInt()
            )

            val artwork = loadArtwork(state.artworkPath)
            if (artwork != null) {
                views.setImageViewBitmap(R.id.widget_artwork, artwork)
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.mipmap.ic_launcher)
            }

            views.setImageViewResource(
                R.id.widget_toggle,
                if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            views.setContentDescription(
                R.id.widget_toggle,
                context.getString(
                    if (state.isPlaying) R.string.widget_pause else R.string.widget_play
                )
            )

            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                actionIntent(context, ACTION_PREVIOUS)
            )
            views.setOnClickPendingIntent(
                R.id.widget_toggle,
                actionIntent(context, ACTION_TOGGLE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                actionIntent(context, ACTION_NEXT)
            )

            manager.updateAppWidget(appWidgetId, views)
        }

        private fun actionIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MsrWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
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
                handleAction(context, intent.action.orEmpty())
                updateAll(context)
            }
            ACTION_REFRESH -> updateAll(context)
        }
    }

    private fun handleAction(context: Context, action: String) {
        val controller = (context.applicationContext as? MonsterSirenApplication)
            ?.playerController
        val hasSong = controller?.uiState?.value?.currentSong != null

        if (controller == null || !hasSong) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            return
        }

        when (action) {
            ACTION_TOGGLE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_PREVIOUS -> controller.skipPrevious()
        }
    }
}
