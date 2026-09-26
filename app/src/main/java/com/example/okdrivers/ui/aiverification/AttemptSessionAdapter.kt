package com.example.okdrivers.ui.aiverification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.okdrivers.R
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.ResponseClassification

class AttemptSessionAdapter(
    private var sessions: List<AIConversationSession> = emptyList()
) : RecyclerView.Adapter<AttemptSessionAdapter.ViewHolder>() {

    fun updateSessions(newSessions: List<AIConversationSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attempt_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount(): Int = sessions.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAttemptTitle: TextView = itemView.findViewById(R.id.tvAttemptTitle)
        private val tvClassificationBadge: TextView = itemView.findViewById(R.id.tvClassificationBadge)
        private val tvPrompt: TextView = itemView.findViewById(R.id.tvPrompt)
        private val tvResponse: TextView = itemView.findViewById(R.id.tvResponse)
        private val tvLatency: TextView = itemView.findViewById(R.id.tvLatency)

        fun bind(session: AIConversationSession) {
            tvAttemptTitle.text = "Attempt ${session.attemptCount}"
            tvPrompt.text = "Prompt: ${session.prompt}"

            val responseText = when {
                !session.completed -> "In Progress..."
                session.response.isNullOrBlank() -> "No response / Speech silent"
                else -> "\"${session.response}\""
            }
            tvResponse.text = "Transcript: $responseText"

            val classification = session.responseClassification
            val classificationText = when {
                !session.completed -> "LISTENING"
                classification == null -> "PENDING"
                else -> classification.name
            }
            tvClassificationBadge.text = classificationText

            val color = when {
                !session.completed -> ContextCompat.getColor(itemView.context, R.color.ok_blue)
                classification == ResponseClassification.RESPONSIVE -> ContextCompat.getColor(itemView.context, R.color.ok_green)
                classification == ResponseClassification.IMPAIRED -> ContextCompat.getColor(itemView.context, R.color.ok_orange)
                else -> ContextCompat.getColor(itemView.context, R.color.ok_red)
            }
            tvClassificationBadge.setTextColor(color)

            val latencyText = if (session.responseLatencyMs != null && session.responseLatencyMs > 0) {
                "${session.responseLatencyMs} ms"
            } else {
                "—"
            }
            tvLatency.text = latencyText
        }
    }
}
