package com.example.okdrivers.ui.community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.okdrivers.R

class ResponderAdapter : ListAdapter<ResponderUiModel, ResponderAdapter.ResponderViewHolder>(DiffCallback) {

    class ResponderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tvResponderInitials)
        val tvName: TextView = view.findViewById(R.id.tvResponderName)
        val tvDistanceEta: TextView = view.findViewById(R.id.tvResponderDistanceEta)
        val tvStatusRep: TextView = view.findViewById(R.id.tvResponderStatusRep)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResponderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_responder, parent, false)
        return ResponderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResponderViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvInitials.text = item.initials
        holder.tvName.text = item.name
        holder.tvDistanceEta.text = "${item.distanceText}  •  ${item.etaText}"
        holder.tvStatusRep.text = "●  ${item.statusText}  •  Reputation ${item.reputationText}"
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ResponderUiModel>() {
            override fun areItemsTheSame(oldItem: ResponderUiModel, newItem: ResponderUiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ResponderUiModel, newItem: ResponderUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
