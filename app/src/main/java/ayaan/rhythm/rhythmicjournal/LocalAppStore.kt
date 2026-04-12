package ayaan.rhythm.rhythmicjournal

import android.content.Context

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

class LocalAppStore(context: Context) {
    private val prefs = context.getSharedPreferences("rhythmic_journal_prefs", Context.MODE_PRIVATE)

    fun loadThemeMode(): ThemeMode {
        return when (prefs.getString("theme_mode", ThemeMode.SYSTEM.name)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun saveThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString("theme_mode", themeMode.name).apply()
    }
}