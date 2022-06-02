package com.android.share.manager.preference

import android.content.Context
import com.android.share.util.Constants

class PreferenceManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        Constants.KEY_PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )

    fun putString(key: String, value: String) = sharedPreferences.edit()?.apply {
        this.putString(key, value)
        this.apply()
    }

    fun getString(key: String) = sharedPreferences.getString(key, "") ?: ""

    fun clear() = sharedPreferences.edit().clear().apply()
}