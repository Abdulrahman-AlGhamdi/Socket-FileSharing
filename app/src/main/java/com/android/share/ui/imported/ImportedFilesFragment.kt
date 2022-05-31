package com.android.share.ui.imported

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.share.R
import com.android.share.databinding.FragmentImportedFilesBinding
import com.android.share.manager.imported.ImportedFilesManagerImpl.FilesState
import com.android.share.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ImportedFilesFragment : Fragment(R.layout.fragment_imported_files) {

    private val binding by viewBinding(FragmentImportedFilesBinding::bind)
    private val viewModel by viewModels<ImportedFilesViewModel>()

    private lateinit var filesJob: Job
    private lateinit var filesJobResult: Job

    private lateinit var importedAdapter: ImportedFilesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        filesJob = viewModel.getLocalFiles()
        filesJobResult = getLocalFiles()
    }

    private fun init() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = DividerItemDecoration(requireContext(), layoutManager.orientation)
        binding.localList.layoutManager = layoutManager
        binding.localList.addItemDecoration(itemDecoration)

        importedAdapter = ImportedFilesAdapter(object : ImportedFilesCallback {
            override fun onFileShare(file: File) {
                viewModel.shareFile(file)
            }

            override fun onFileLongCLick(file: File) {
                deleteDocument(file)
            }

            override fun renameInternalFile(file: File) {
                renameFile(file)
            }
        })

        binding.localList.adapter = importedAdapter
    }

    private fun getLocalFiles() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.localFilesState.collect {
            when (it) {
                FilesState.FilesEmpty -> {
                    binding.localList.visibility = View.GONE
                    binding.empty.visibility = View.VISIBLE
                }
                is FilesState.FilesSuccess -> {
                    importedAdapter.setImportedFilesList(it.localFiles)
                    binding.empty.visibility = View.GONE
                    binding.localList.visibility = View.VISIBLE
                }
                else -> Unit
            }
        }
    }

    private fun deleteDocument(file: File) {
        AlertDialog.Builder(requireActivity()).apply {
            this.setTitle("Delete File")
            this.setMessage("Are you sure you want to delete this files")
            this.setNegativeButton("Cancel", null)
            this.setPositiveButton("Yes") { _, _ ->
                viewModel.deleteFile(file)
            }
        }.create().show()
    }

    private fun renameFile(file: File) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.file_name)

        val renameDialog = AlertDialog.Builder(requireActivity()).apply {
            this.setTitle("Rename File")
            this.setView(dialogView)
            this.setNegativeButton("Cancel", null)
            this.setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    viewModel.renameFile(file, newName)
                }
            }
        }.create()

        renameDialog.setOnShowListener {
            editText.setText(file.name)
            editText.requestFocus()
            editText.selectAll()
        }

        renameDialog.show()
    }

    override fun onDestroyView() {
        if (::filesJob.isInitialized) filesJob.cancel()
        if (::filesJobResult.isInitialized) filesJobResult.cancel()
        super.onDestroyView()
    }
}