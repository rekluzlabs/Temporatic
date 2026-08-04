package com.rekluzlabs.temporatic.utils

import android.content.Context

object FirstRunManager {
    private const val PREFS_NAME = "temporatic_first_run"
    private const val KEY_SETUP_COMPLETED = "setup_completed"

    fun isSetupCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun markSetupCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .apply()
    }

    fun resetSetup(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SETUP_COMPLETED)
            .apply()
    }
}
