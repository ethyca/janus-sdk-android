package com.ethyca.janussdk.android.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ethyca.janussdk.android.example.databinding.ActivityEventDetailBinding
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.*

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy, h:mm:ss a", Locale.US)

    companion object {
        private const val EXTRA_EVENT_TYPE = "extra_event_type"
        private const val EXTRA_EVENT_DETAIL = "extra_event_detail"
        private const val EXTRA_EVENT_TIMESTAMP = "extra_event_timestamp"

        fun createIntent(context: Context, eventType: String, eventDetail: String, timestamp: Long): Intent {
            return Intent(context, EventDetailActivity::class.java).apply {
                putExtra(EXTRA_EVENT_TYPE, eventType)
                putExtra(EXTRA_EVENT_DETAIL, eventDetail)
                putExtra(EXTRA_EVENT_TIMESTAMP, timestamp)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Event Details"

        // Get data from intent
        val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: "Unknown Event"
        val eventDetail = intent.getStringExtra(EXTRA_EVENT_DETAIL) ?: "{}"
        val timestamp = intent.getLongExtra(EXTRA_EVENT_TIMESTAMP, System.currentTimeMillis())

        // Display event information
        displayEventDetails(eventType, eventDetail, timestamp)

        // Set up copy button
        binding.copyButton.setOnClickListener {
            copyEventToClipboard(eventType, eventDetail, timestamp)
        }
    }

    private fun displayEventDetails(eventType: String, eventDetail: String, timestamp: Long) {
        // Set basic info
        binding.eventTypeText.text = eventType
        binding.timestampText.text = dateFormat.format(Date(timestamp))

        // Format and display JSON
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonElement = JsonParser.parseString(eventDetail)
            val formattedJson = gson.toJson(jsonElement)
            binding.eventDetailText.text = formattedJson
        } catch (e: Exception) {
            // If it's not valid JSON, display as-is
            binding.eventDetailText.text = eventDetail
        }
    }

    private fun copyEventToClipboard(eventType: String, eventDetail: String, timestamp: Long) {
        try {
            val eventInfo = buildString {
                appendLine("Event Type: $eventType")
                appendLine("Timestamp: ${dateFormat.format(Date(timestamp))}")
                appendLine("Details:")
                appendLine(eventDetail)
            }

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Event Details", eventInfo)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Event details copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to copy event details", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
