package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "msr_preferences")

class UserPreferences(private val context: Context) {

    private object Keys {
        val OFFLINE_ONLY_MODE = booleanPreferencesKey("offline_only_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val LAST_SONG_CID = stringPreferencesKey("last_song_cid")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val UPDATE_DISMISSED_TAG = stringPreferencesKey("update_dismissed_tag")
        val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
        val PLAYBACK_QUEUE = stringPreferencesKey("playback_queue")
        val PLAYBACK_QUEUE_INDEX = intPreferencesKey("playback_queue_index")
    }

    val offlineOnlyMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.OFFLINE_ONLY_MODE] ?: false
    }

    val shuffleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHUFFLE_ENABLED] ?: false
    }

    val repeatMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REPEAT_MODE] ?: 0
    }

    val lastSongCid: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SONG_CID]
    }

    val disclaimerAccepted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISCLAIMER_ACCEPTED] ?: false
    }

    val updateDismissedTag: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.UPDATE_DISMISSED_TAG]
    }

    val lastSeenVersion: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SEEN_VERSION]
    }

    val playbackQueue: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.PLAYBACK_QUEUE]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
    }

    val playbackQueueIndex: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.PLAYBACK_QUEUE_INDEX] ?: 0
    }

    suspend fun setOfflineOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_ONLY_MODE] = enabled }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode }
    }

    suspend fun setLastSongCid(cid: String) {
        context.dataStore.edit { it[Keys.LAST_SONG_CID] = cid }
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { it[Keys.DISCLAIMER_ACCEPTED] = accepted }
    }

    suspend fun setUpdateDismissedTag(tag: String?) {
        context.dataStore.edit { prefs ->
            if (tag == null) prefs.remove(Keys.UPDATE_DISMISSED_TAG)
            else prefs[Keys.UPDATE_DISMISSED_TAG] = tag
        }
    }

    suspend fun setLastSeenVersion(version: String) {
        context.dataStore.edit { it[Keys.LAST_SEEN_VERSION] = version }
    }

    suspend fun setPlaybackQueue(cids: List<String>, currentIndex: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAYBACK_QUEUE] = cids.joinToString(",")
            prefs[Keys.PLAYBACK_QUEUE_INDEX] = currentIndex
        }
    }
}
