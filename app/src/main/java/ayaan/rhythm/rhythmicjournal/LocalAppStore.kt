package ayaan.rhythm.rhythmicjournal

import android.content.Context

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class LocalAppStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("rhythmic_journal_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }
}