package com.eventcalendar.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    /** Default view mode for the statistics page: "recent" or "cumulative" */
    var defaultStatisticsMode: String
        get() = prefs.getString(KEY_STATISTICS_MODE, "recent") ?: "recent"
        set(value) {
            prefs.edit().putString(KEY_STATISTICS_MODE, value).apply()
        }

    companion object {
        private const val KEY_STATISTICS_MODE = "statistics_default_mode"
    }
}
