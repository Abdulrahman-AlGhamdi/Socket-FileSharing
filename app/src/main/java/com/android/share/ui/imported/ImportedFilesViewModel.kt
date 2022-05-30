package com.android.share.ui.imported

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.imported.ImportedFilesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImportedFilesViewModel @Inject constructor(
    private val importedFilesManager: ImportedFilesManager
) : ViewModel() {

    val localFilesState = importedFilesManager.filesState

    fun getLocalFiles() = viewModelScope.launch {
        importedFilesManager.getLocalFiles()
    }

    fun shareFile(file: File) = importedFilesManager.shareFile(file)

    fun deleteFile(file: File) = viewModelScope.launch {
        importedFilesManager.deleteFile(file)
        getLocalFiles()
    }

    fun renameFile(file: File, newName: String) = viewModelScope.launch {
        importedFilesManager.renameFile(file, newName)
        getLocalFiles()
    }
}