package com.ethyca.janussdk.android.example

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private var events: List<EventItem> = emptyList(),
    private val onEventClick: (EventItem) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val dateFormat = SimpleDateFormat("h:mm:ss a", Locale.US)

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventTypeText: TextView = itemView.findViewById(R.id.eventTypeText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        
        holder.eventTypeText.text = event.eventType
        holder.timestampText.text = dateFormat.format(Date(event.timestamp))
        
        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<EventItem>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
