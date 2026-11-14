package app.ji.data.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences and privacy settings.
 */
object JiSettings {
    private const val PREFS_NAME = "ji_settings_prefs"
    private const val KEY_HIDE_REMINDER_TEXT = "hide_reminder_text"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Settings toggle: "Hide reminder text on lock screen and widget".
     * Default is ON (true) as required by SPEC.
     */
    fun isHideReminderText(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HIDE_REMINDER_TEXT, true)
    }

    fun setHideReminderText(context: Context, hide: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HIDE_REMINDER_TEXT, hide).apply()
    }
}

