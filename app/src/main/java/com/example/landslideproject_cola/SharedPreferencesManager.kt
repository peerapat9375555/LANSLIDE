package com.example.landslideproject_cola

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences("earthquake_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ROLE = "role"
    }

    fun saveLoginStatus(isLoggedIn: Boolean, userId: String, name: String, email: String, role: String) {
        preferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getSavedUserId(): String = preferences.getString(KEY_USER_ID, "") ?: ""
    fun getSavedUserName(): String = preferences.getString(KEY_USER_NAME, "") ?: ""
    fun getSavedEmail(): String = preferences.getString(KEY_USER_EMAIL, "") ?: ""
    fun getSavedRole(): String = preferences.getString(KEY_ROLE, "user") ?: "user"

    fun logout() {
        preferences.edit().clear().apply()
    }
}
