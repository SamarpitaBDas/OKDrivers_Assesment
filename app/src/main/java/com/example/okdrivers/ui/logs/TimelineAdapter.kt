package com.example.okdrivers.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.okdrivers.R

class TimelineAdapter : ListAdapter<TimelineUiModel, TimelineAdapter.TimelineViewHolder>(DiffCallback) {

    class TimelineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTimelineTime)
        val tvReason: TextView = view.findViewById(R.id.tvTimelineReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline, parent, false)
        return TimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvTime.text = item.timeText
        holder.tvReason.text = item.reasonText
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TimelineUiModel>() {
            override fun areItemsTheSame(oldItem: TimelineUiModel, newItem: TimelineUiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TimelineUiModel, newItem: TimelineUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
