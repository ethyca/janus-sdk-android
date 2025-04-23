package com.ethyca.janussdk.android.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethyca.janussdk.android.Janus
import java.text.SimpleDateFormat
import java.util.*
import com.ethyca.janussdk.android.example.databinding.ActivityFullExampleBinding

class FullExampleActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFullExampleBinding
    private val janusManager: JanusManager = JanusManager.getInstance()
    private lateinit var consentValueAdapter: ConsentValueAdapter
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy, h:mm a", Locale.US)
    
    // Add these variables for WebView management
    private var webView: WebView? = null
    private var webViewContainer: FrameLayout? = null
    private var progressBar: ProgressBar? = null
    private var autoSyncSwitch: Switch? = null
    private val backgroundWebViews: MutableList<WebView> = mutableListOf()
    private var webViewVisible = false
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, FullExampleActivity::class.java)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullExampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize UI elements
        initializeUI()
        
        // Initialize WebView controls
        initializeWebViewControls()
        
        // Observe Janus state
        observeJanusState()
    }
    
    private fun initializeUI() {
        // Setup RecyclerView for consent values
        consentValueAdapter = ConsentValueAdapter()
        binding.consentValuesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FullExampleActivity)
            adapter = consentValueAdapter
        }
        
        // Setup show experience button
        binding.showExperienceButton.setOnClickListener {
            janusManager.showPrivacyExperience(this)
        }
        
        // Setup clear events button
        binding.clearEventsButton.setOnClickListener {
            janusManager.clearEventLog()
        }
        
        // Setup copy experience button
        binding.copyExperienceButton.setOnClickListener {
            if (janusManager.copyExperienceJSON(this)) {
                showToast("Privacy experience copied to clipboard")
            } else {
                showToast("Failed to copy privacy experience")
            }
        }
    }
    
    private fun initializeWebViewControls() {
        // Initialize WebView container and controls
        webViewContainer = binding.webViewContainer
        progressBar = binding.progressBar
        autoSyncSwitch = binding.autoSyncSwitch

        // Set up buttons
        binding.showWebViewButton.setOnClickListener {
            showWebView()
        }
        
        binding.addBackgroundWebViewButton.setOnClickListener {
            addBackgroundWebView()
        }
        
        binding.clearBackgroundWebViewsButton.setOnClickListener {
            removeAllBackgroundWebViews()
        }
        
        // Update the background WebView count display
        updateBackgroundWebViewCount()
    }
    
    private fun observeJanusState() {
        // Debug: Check current value
        println("FullExampleActivity: Current isInitialized value: ${janusManager.isInitialized.value}")
        
        // Observe initialization state
        janusManager.isInitialized.observe(this) { isInitialized ->
            println("FullExampleActivity: isInitialized updated to: $isInitialized")
            binding.statusCard.visibility = View.VISIBLE
            updateStatusText(binding.statusValueText, isInitialized, "Success ✅", "Not Initialized")
        }
        
        // Observe has experience
        janusManager.hasExperience.observe(this) { hasExperience ->
            updateStatusText(binding.hasExperienceValueText, hasExperience, "Available ✅", "Not Available ❌")
            // Toggle visibility of copy button based on experience availability
            binding.copyExperienceButton.visibility = if (hasExperience) View.VISIBLE else View.GONE
            // Enable/disable show experience button based on experience availability
            binding.showExperienceButton.isEnabled = hasExperience
        }
        
        // Observe events
        janusManager.events.observe(this) { events ->
            binding.noEventsText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            binding.eventsTextView.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
            binding.eventsTextView.text = events.joinToString("\n\n")
        }
        
        // Observe consent values
        janusManager.consentValues.observe(this) { consentValues ->
            if (consentValues.isEmpty()) {
                binding.noConsentValuesText.visibility = View.VISIBLE
                binding.consentValuesRecyclerView.visibility = View.GONE
                binding.consentValuesCount.text = "(0)"
            } else {
                binding.noConsentValuesText.visibility = View.GONE
                binding.consentValuesRecyclerView.visibility = View.VISIBLE
                binding.consentValuesCount.text = "(${consentValues.size})"
                consentValueAdapter.updateValues(consentValues)
            }
        }
        
        // Observe fides string
        janusManager.fidesString.observe(this) { fidesString ->
            // Only show fides string container if it has a value
            binding.fidesStringContainer.visibility = if (fidesString.isEmpty()) View.GONE else View.VISIBLE
            binding.fidesStringValueText.text = fidesString
        }
        
        // Observe consent method
        janusManager.consentMethod.observe(this) { consentMethod ->
            binding.consentMethodValueText.text = if (consentMethod.isEmpty()) "Not Set" else consentMethod
        }
        
        // Observe consent metadata timestamps
        janusManager.consentMetadata.observe(this) { metadata ->
            // Format Created timestamp
            val createdText = if (metadata.createdAt != null) {
                dateFormat.format(metadata.createdAt!!)
            } else {
                "Not Set"
            }
            binding.createdValueText.text = createdText
            
            // Format Updated timestamp
            val updatedText = if (metadata.updatedAt != null) {
                dateFormat.format(metadata.updatedAt!!)
            } else {
                "Not Set"
            }
            binding.updatedValueText.text = updatedText
        }
    }
    
    /**
     * Helper function to update status text with color and emoji indicators
     */
    private fun updateStatusText(textView: TextView, isActive: Boolean, activeText: String, inactiveText: String) {
        textView.text = if (isActive) activeText else inactiveText
        textView.setTextColor(
            ContextCompat.getColor(
                this, 
                if (isActive) R.color.status_success else R.color.status_error
            )
        )
    }
    
    /**
     * Helper function to show a toast message
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Creates and displays a WebView in a new activity that takes up the full screen
     */
    private fun showWebView() {
        // Get auto-sync setting from the toggle
        val autoSyncOnStart = autoSyncSwitch?.isChecked ?: true
        
        // Create intent for WebViewActivity
        val intent = Intent(this, WebViewActivity::class.java).apply {
            // Pass URL to load
            putExtra(WebViewActivity.EXTRA_WEBSITE_URL, janusManager.websiteURL)
            // Pass auto-sync option
            putExtra(WebViewActivity.EXTRA_AUTO_SYNC, autoSyncOnStart)
        }
        
        // Launch the activity
        startActivity(intent)
        
        Toast.makeText(this, "Launching WebView with autoSync=$autoSyncOnStart", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * WebView is now managed in WebViewActivity, so this method is no longer needed
     */
    private fun closeWebView() {
        // No longer needed as WebView is in a separate activity
        Toast.makeText(this, "WebView is managed by WebViewActivity", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Creates a background WebView that's not shown to the user
     */
    private fun addBackgroundWebView() {
        // Get autoSync value from the toggle
        val autoSyncOnStart = autoSyncSwitch?.isChecked ?: true
        
        // Create a WebView that won't be shown on screen
        val backgroundWebView = Janus.createConsentWebView(this, autoSyncOnStart)
        
        // Add to our list of background WebViews
        backgroundWebViews.add(backgroundWebView)
        
        // Load the website URL
        backgroundWebView.loadUrl(janusManager.websiteURL)
        
        // Update the displayed count
        updateBackgroundWebViewCount()
        
        Toast.makeText(
            this, 
            "Background WebView added (total: ${backgroundWebViews.size})", 
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * Updates the text showing how many background WebViews exist
     */
    private fun updateBackgroundWebViewCount() {
        val countTextView = binding.backgroundWebViewCount
        countTextView.text = "Background WebViews: ${backgroundWebViews.size}"
    }
    
    /**
     * Removes all background WebViews
     */
    private fun removeAllBackgroundWebViews() {
        // Release each WebView
        for (backgroundWebView in backgroundWebViews) {
            Janus.releaseConsentWebView(backgroundWebView)
        }
        
        // Clear the list
        backgroundWebViews.clear()
        
        // Update the display
        updateBackgroundWebViewCount()
        
        Toast.makeText(this, "All background WebViews removed", Toast.LENGTH_SHORT).show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up the foreground WebView
        webView?.let {
            Janus.releaseConsentWebView(it)
            webView = null
        }
        
        // Clean up all background WebViews
        removeAllBackgroundWebViews()
    }
} 