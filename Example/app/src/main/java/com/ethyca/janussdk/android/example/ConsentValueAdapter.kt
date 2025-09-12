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
    private var consentValues: List<Pair<String, Any>> = emptyList()
    
    /**
     * Update the consent values
     */
    fun updateValues(values: Map<String, Any>) {
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
        fun bind(item: Pair<String, Any>) {
            val context = binding.root.context
            
            // Set the key
            binding.consentKeyText.text = item.first
            
            // Handle both Boolean and String values
            when (val value = item.second) {
                is Boolean -> {
                    // Traditional boolean mode
                    binding.consentValueText.text = if (value) "Allowed ✓" else "Denied ✗"
                    binding.consentValueText.setTextColor(
                        ContextCompat.getColor(
                            context, 
                            if (value) R.color.status_success else R.color.status_error
                        )
                    )
                }
                is String -> {
                    // Consent mechanism mode
                    binding.consentValueText.text = when (value) {
                        "opt_in" -> "Opt In ✓"
                        "opt_out" -> "Opt Out ✗"
                        "acknowledge" -> "Acknowledged ℹ"
                        "not_applicable" -> "Not Applicable —"
                        else -> value // Fallback to raw string
                    }
                    binding.consentValueText.setTextColor(
                        ContextCompat.getColor(
                            context, 
                            when (value) {
                                "opt_in" -> R.color.status_success
                                "opt_out" -> R.color.status_error
                                "acknowledge" -> R.color.status_acknowledge
                                "not_applicable" -> R.color.status_not_applicable
                                else -> R.color.text_primary
                            }
                        )
                    )
                }
                else -> {
                    // Fallback for any other type
                    binding.consentValueText.text = value.toString()
                    binding.consentValueText.setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary)
                    )
                }
            }
        }
    }
} 
