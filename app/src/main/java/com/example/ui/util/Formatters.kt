package com.example.ui.util

import java.util.Locale

object Formatters {

    fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSeconds = ms / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 МБ"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f ГБ", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f МБ", mb)
            else -> String.format(Locale.US, "%.0f КБ", kb.coerceAtLeast(1.0))
        }
    }
}
