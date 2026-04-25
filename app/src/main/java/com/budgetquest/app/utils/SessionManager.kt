package com.budgetquest.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the logged-in user session using SharedPreferences.
 * Stores userId and username so every screen knows who is logged in.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "budget_quest_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val NO_USER = -1
    }

    fun saveSession(userId: Int, username: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, NO_USER)
    }

    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return prefs.getInt(KEY_USER_ID, NO_USER) != NO_USER
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}