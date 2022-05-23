package com.android.share.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun View.showSnackBar(
    message: String,
    length: Int = Snackbar.LENGTH_SHORT,
    anchorView: Int? = null,
    actionMessage: String? = null,
    action: (View) -> Unit = {}
) {
    Snackbar.make(this, message, length).apply {
        actionMessage?.let { this.setAction(actionMessage) { action(it) } }
        anchorView?.let { this.setAnchorView(anchorView) }
    }.show()
}

fun NavController.navigateTo(action: NavDirections, fragmentId: Int) {
    if (this.currentDestination == this.graph.findNode(fragmentId))
        this.navigate(action)
}

fun Uri.bitmapToString(context: Context): String {
    val inputStream = context.contentResolver.openInputStream(this)
    val outputStream = ByteArrayOutputStream()
    val image = BitmapFactory.decodeStream(inputStream)

    image.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

fun String.toBitmap(): Bitmap {
    val bytes = Base64.decode(this, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun Activity.keepScreenOn(keep: Boolean) {
    if (keep) this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    else this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun File.getSize(fileSize: Long): String {
    val fileSizeInKB = fileSize.div(1024)
    val fileSizeInMB = fileSize.div(1024 * 1024)

    return when {
        fileSizeInKB < 1 -> "$fileSize B"
        fileSizeInMB < 1 -> "$fileSizeInKB KB"
        else -> "$fileSizeInMB MB"
    }
}

fun Fragment.checkPermission(permission: String, isGranted: (Boolean) -> Unit) {
    if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED)
        isGranted(true) else isGranted(false)
}

fun Context.clearApplicationUserData() {
    val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.clearApplicationUserData()
}

fun InputStream.readStringFromStream(metadataSize: Int = 0): String {
    val stringSize = if (metadataSize == 0) this.read() else metadataSize
    val metadataByteArraySize = ByteArray(stringSize)
    val metadataBytesLength = this.read(metadataByteArraySize)

    val byteArrayOutputStream = ByteArrayOutputStream()
    byteArrayOutputStream.write(metadataByteArraySize, 0, metadataBytesLength)
    byteArrayOutputStream.close()

    return byteArrayOutputStream.toString("UTF-8")
}

fun OutputStream.writeStringAsStream(string: String) {
    val stringByteArray = string.toByteArray()
    this.write(stringByteArray.size)
    this.write(stringByteArray)
}