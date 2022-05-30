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

    // File Extension
    val IMAGE_EXTENSION = listOf("jpg", "png", "webp", "jpeg", "heif", "heic")
    val VIDEO_EXTENSION = listOf("mp4", "3gp", "mkv", "webm", "mov")
    val SOUND_EXTENSION = listOf("opus", "mp3", "wav", "m4a")
    val TXT_EXTENSION = listOf("txt", "rtf", "rtx", "vcf")
    val WORD_EXTENSION = listOf("docx", "dotx")
    val EXCEL_EXTENSION = listOf("xlsx", "xlsm", "xlsb", "xltx")
    val POWERPOINT_EXTENSION = listOf("ppt", "pptx")
    const val PDF_EXTENSION = "pdf"
    const val OFUQ_EXTENSION = "ofuq"

    // List Of Colors
    val colorList = listOf(
        R.color.blue,
        R.color.red,
        R.color.green,
        R.color.yellow,
        R.color.purple
    )
}