package com.android.share.ui.scan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.share.R
import com.android.share.databinding.SenderUserItemBinding
import com.android.share.util.Constants

class ScanAdapter(
    private val scanAdapterCallback: ScanAdapterCallback
) : RecyclerView.Adapter<ScanAdapter.SenderViewHolder>() {

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
            val (name, device, address) = receiver.split(":")
            val randomColor = itemView.resources.getColor(Constants.colorList.random(), null)

            binding.name.text = name
            binding.address.text = address
            binding.color.setCardBackgroundColor(randomColor)
            binding.root.setOnClickListener { scanAdapterCallback.onReceiverClick(name, address) }
            if (device == Constants.PHONE_DEVICE) binding.device.setImageResource(R.drawable.icon_phone)
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