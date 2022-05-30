package com.android.share.manager.imported

import com.android.share.manager.imported.ImportedFilesManagerImpl.FilesState
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface ImportedFilesManager {

    val filesState: StateFlow<FilesState>

    suspend fun getLocalFiles()

    fun shareFile(file: File)

    suspend fun deleteFile(file: File)

    suspend fun renameFile(file: File, newName: String)
}