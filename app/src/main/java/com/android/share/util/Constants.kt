package com.android.share.util

import com.android.share.R

object Constants {
    // Shared Preferences Constants
    const val KEY_PREFERENCE_NAME = "shareFilesPreference"
    const val USERNAME = "receiver_name"

    // Socket Constants
    const val SOCKET_SCAN = "scan"
    const val SOCKET_SHARE = "share"
    const val SOCKET_ACCEPT = "accept"
    const val SOCKET_REFUSE = "refuse"

    // Device Type
    const val PHONE_DEVICE = "phone"

    // List Of Colors
    val colorList = listOf(
        R.color.blue,
        R.color.red,
        R.color.green,
        R.color.yellow,
        R.color.purple
    )
}