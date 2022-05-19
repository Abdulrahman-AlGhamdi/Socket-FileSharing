package com.android.share.ui.sender

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.share.databinding.SenderUserItemBinding
import java.util.*

class SenderAdapter(
    private val senderAdapterListeners: SenderAdapterListeners
) : RecyclerView.Adapter<SenderAdapter.SenderViewHolder>() {

    private val receiversList = mutableListOf<String>()

    fun setReceiversList(list: List<String>) {
        receiversList.clear()
        receiversList.addAll(list)
        notifyDataSetChanged()
    }

    inner class SenderViewHolder(
        private val binding: SenderUserItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(receiver: String) {
            val uniqueNumber = receiver.substringAfterLast(".")
            val rnd = Random()
            val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))

            binding.name.text = uniqueNumber
            binding.background.setBackgroundColor(color)
            binding.root.setOnClickListener { senderAdapterListeners.onReceiverClick(receiver) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SenderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SenderViewHolder(SenderUserItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: SenderViewHolder, position: Int) {
        holder.bind(receiversList[position])
    }

    override fun getItemCount(): Int = receiversList.size
}