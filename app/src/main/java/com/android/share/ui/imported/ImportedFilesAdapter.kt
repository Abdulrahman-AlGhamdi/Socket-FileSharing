package com.android.share.ui.imported

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.android.share.R
import com.android.share.databinding.ImportedFileItemBinding
import com.android.share.util.getReadableSize
import java.io.File

class ImportedFilesAdapter(
    private val importedClickListeners: ImportedFilesCallback
) : Adapter<ImportedFilesAdapter.LocalFilesViewHolder>() {

    private val documentFilesList = mutableListOf<File>()

    fun setImportedFilesList(list: List<File>) {
        documentFilesList.clear()
        documentFilesList.addAll(list)
        notifyDataSetChanged()
    }

    inner class LocalFilesViewHolder(
        private val binding: ImportedFileItemBinding
    ) : ViewHolder(binding.root) {

        fun bind(file: File) {
            binding.name.text = file.name
            binding.size.text = file.length().getReadableSize()
            binding.icon.setImageResource(R.drawable.icon_file)
            binding.share.setOnClickListener { importedClickListeners.onFileShare(file) }

            itemView.setOnCreateContextMenuListener { menu, _, _ ->
                menu.setHeaderTitle("Select an option")

                menu.add("Rename").setOnMenuItemClickListener {
                    importedClickListeners.renameInternalFile(file)
                    true
                }
                menu.add("Delete").setOnMenuItemClickListener {
                    importedClickListeners.onFileLongCLick(file)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalFilesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return LocalFilesViewHolder(ImportedFileItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: LocalFilesViewHolder, position: Int) {
        holder.bind(documentFilesList[position])
    }

    override fun getItemCount() = documentFilesList.size
}