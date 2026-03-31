package com.github.kasper_gram.somefetcher.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.databinding.ItemDigestTimeBinding

class DigestTimeAdapter(
    private val onEdit: (Pair<Int, Int>) -> Unit,
    private val onDelete: (Pair<Int, Int>) -> Unit
) : ListAdapter<Pair<Int, Int>, DigestTimeAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDigestTimeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDigestTimeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(time: Pair<Int, Int>) {
            val (hour, minute) = time
            val ctx = binding.root.context
            binding.textTime.text = ctx.getString(R.string.time_format, hour, minute)
            binding.buttonDeleteTime.contentDescription =
                ctx.getString(R.string.delete_time_description, hour, minute)
            binding.root.setOnClickListener { onEdit(time) }
            binding.buttonDeleteTime.setOnClickListener { onDelete(time) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Pair<Int, Int>>() {
            override fun areItemsTheSame(oldItem: Pair<Int, Int>, newItem: Pair<Int, Int>) =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: Pair<Int, Int>, newItem: Pair<Int, Int>) =
                oldItem == newItem
        }
    }
}
