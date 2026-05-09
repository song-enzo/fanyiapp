package com.ocrtranslator.utils

import android.content.Context

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("ocr_translator", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var baseUrl: String
        get() = prefs.getString("base_url", "https://generativelanguage.googleapis.com") ?: "https://generativelanguage.googleapis.com"
        set(value) = prefs.edit().putString("base_url", value.trimEnd('/')).apply()

    var model: String
        get() = prefs.getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) = prefs.edit().putString("model", value.ifBlank { "gemini-2.5-flash" }).apply()

    var isLeft: Boolean
        get() = prefs.getBoolean("is_left", false)
        set(value) = prefs.edit().putBoolean("is_left", value).apply()

    var opacity: Float
        get() = prefs.getFloat("opacity", 0.72f)
        set(value) = prefs.edit().putFloat("opacity", value.coerceIn(0.2f, 1f)).apply()

    var textSize: Float
        get() = prefs.getFloat("text_size", 24f)
        set(value) = prefs.edit().putFloat("text_size", value.coerceIn(16f, 36f)).apply()

    var bilingual: Boolean
        get() = prefs.getBoolean("bilingual", true)
        set(value) = prefs.edit().putBoolean("bilingual", value).apply()
}
