package com.oneshotcam

import android.content.Context
import android.content.SharedPreferences

object ApiKeyManager {
    private const val PREFS_NAME = " oneshotcam_prefs\
 private const val KEY_API_KEY = \api_key\
 private const val DEFAULT_KEY = \YOUR_GEMINI_API_KEY\

 fun getKey(context: Context): String {
 val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
 return prefs.getString(KEY_API_KEY, DEFAULT_KEY) ?: DEFAULT_KEY
 }

 fun setKey(context: Context, key: String) {
 val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
 prefs.edit().putString(KEY_API_KEY, key).apply()
 }
}

