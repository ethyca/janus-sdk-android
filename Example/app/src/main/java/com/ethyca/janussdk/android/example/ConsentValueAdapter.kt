package com.ethyca.janussdk.android.example

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ethyca.janussdk.android.example.databinding.ItemConsentValueBinding

/**
 * Adapter for displaying consent key-value pairs in a RecyclerView
 */
class ConsentValueAdapter : RecyclerView.Adapter<ConsentValueAdapter.ViewHolder>() {
    private var consentValues: List<Pair<String, Boolean>> = emptyList()
    
    /**
     * Update the consent values
     */
    fun updateValues(values: Map<String, Boolean>) {
        consentValues = values.map { Pair(it.key, it.value) }.sortedBy { it.first }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConsentValueBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = consentValues[position]
        holder.bind(item)
    }
    
    override fun getItemCount(): Int = consentValues.size
    
    inner class ViewHolder(private val binding: ItemConsentValueBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Pair<String, Boolean>) {
            val isAllowed = item.second
            val context = binding.root.context
            
            // Set the key
            binding.consentKeyText.text = item.first
            
            // Set the value with appropriate styling
            binding.consentValueText.text = if (isAllowed) "Allowed ✓" else "Denied ✗"
            binding.consentValueText.setTextColor(
                ContextCompat.getColor(
                    context, 
                    if (isAllowed) R.color.status_success else R.color.status_error
                )
            )
        }
    }
} 