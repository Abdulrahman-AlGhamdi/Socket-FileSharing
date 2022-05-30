package com.android.share.manager.imported

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.android.share.manager.imported.ImportedFilesManagerImpl.FilesState.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ImportedFilesManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImportedFilesManager {

    private val _filesState: MutableStateFlow<FilesState> = MutableStateFlow(FilesIdle)
    override val filesState = _filesState.asStateFlow()

    override suspend fun getLocalFiles() = withContext(Dispatchers.IO) {
        val importedFiles = context.filesDir.listFiles()

        if (importedFiles.isNullOrEmpty()) _filesState.value = FilesEmpty
        else _filesState.value = FilesSuccess(importedFiles.toList())
    }

    override fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileProvider", file)
        val type = context.contentResolver.getType(uri)

        context.startActivity(ShareCompat.IntentBuilder(context).apply {
            this.setStream(uri)
            this.setType(type)
        }.createChooserIntent().apply {
            this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override suspend fun deleteFile(file: File) {
        file.delete()
    }

    override suspend fun renameFile(file: File, newName: String) {
        val newFile = File(context.filesDir, newName)
        file.renameTo(newFile)
    }

    sealed class FilesState {
        object FilesIdle : FilesState()
        object FilesEmpty : FilesState()
        data class FilesSuccess(val localFiles: List<File>) : FilesState()
    }
}