package com.android.share.ui.imported

import java.io.File

interface ImportedFilesCallback {
    fun onFileShare(file: File)
    fun onFileLongCLick(file: File)
    fun renameInternalFile(file: File)
}