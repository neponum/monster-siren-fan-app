package com.example.widget

import android.content.Context

data class WidgetSnapshot(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkPath: String? = null,
    val isPlaying: Boolean = false
)

/**
 * Small persistent snapshot of the player state so the home-screen widget can
 * render instantly without binding to the media session.
 */
object WidgetStateStore {

    private const val PREFS = "msr_widget_state"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_ALBUM = "album"
    private const val KEY_ARTWORK = "artwork"
    private const val KEY_PLAYING = "playing"

    fun save(context: Context, snapshot: WidgetSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TITLE, snapshot.title)
            .putString(KEY_ARTIST, snapshot.artist)
            .putString(KEY_ALBUM, snapshot.album)
            .putString(KEY_ARTWORK, snapshot.artworkPath)
            .putBoolean(KEY_PLAYING, snapshot.isPlaying)
            .apply()
    }

    fun load(context: Context): WidgetSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetSnapshot(
            title = prefs.getString(KEY_TITLE, "").orEmpty(),
            artist = prefs.getString(KEY_ARTIST, "").orEmpty(),
            album = prefs.getString(KEY_ALBUM, "").orEmpty(),
            artworkPath = prefs.getString(KEY_ARTWORK, null),
            isPlaying = prefs.getBoolean(KEY_PLAYING, false)
        )
    }

    /**
     * Playback was explicitly stopped (task swiped away or service destroyed).
     */
    fun clear(context: Context) {
        save(context, WidgetSnapshot())
    }
}
