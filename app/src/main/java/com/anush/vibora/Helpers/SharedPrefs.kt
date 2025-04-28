package com.anush.vibora.Helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.isInitialized

object SharedPrefs {

    private const val PREF_NAME = "ViboraChatsPref"
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun ensureInitialized() {
        check(::sharedPreferences.isInitialized) { "SharedPrefsHelper is not initialized. Call init(context) before using it." }
    }

    fun putString(key: String, value: String) {
        ensureInitialized()
        sharedPreferences.edit { putString(key, value) }
    }

    fun getString(key: String): String? {
        ensureInitialized()
        return sharedPreferences.getString(key, null)
    }

    fun putBoolean(key: String, value: Boolean) {
        ensureInitialized()
        sharedPreferences.edit { putBoolean(key, value) }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        ensureInitialized()
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        ensureInitialized()
        sharedPreferences.edit { putInt(key, value) }
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        ensureInitialized()
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun clear() {
        ensureInitialized()
        sharedPreferences.edit { clear() }
    }
}
