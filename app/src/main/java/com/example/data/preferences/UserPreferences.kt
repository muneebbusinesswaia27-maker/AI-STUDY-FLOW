package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_flow_user_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Alex") ?: "Alex"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var educationLevel: String
        get() = prefs.getString(KEY_EDUCATION_LEVEL, "Undergraduate") ?: "Undergraduate"
        set(value) = prefs.edit().putString(KEY_EDUCATION_LEVEL, value).apply()

    var dailyGoalHours: Float
        get() = prefs.getFloat(KEY_DAILY_GOAL_HOURS, 4.0f)
        set(value) = prefs.edit().putFloat(KEY_DAILY_GOAL_HOURS, value).apply()

    var preferredStudyHours: String
        get() = prefs.getString(KEY_PREFERRED_STUDY_HOURS, "Morning (08:00 - 12:00)") ?: "Morning (08:00 - 12:00)"
        set(value) = prefs.edit().putString(KEY_PREFERRED_STUDY_HOURS, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_THEME, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_EDUCATION_LEVEL = "education_level"
        private const val KEY_DAILY_GOAL_HOURS = "daily_goal_hours"
        private const val KEY_PREFERRED_STUDY_HOURS = "preferred_study_hours"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
