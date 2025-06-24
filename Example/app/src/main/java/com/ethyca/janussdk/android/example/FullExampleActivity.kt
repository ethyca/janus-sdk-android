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

        // Setup detect region button
        binding.detectRegionButton.setOnClickListener {
            binding.ipRegionValueText.text = "Detecting..."
            janusManager.detectRegionByIP { success, region ->
                binding.ipRegionValueText.text = if (success && region.isNotEmpty())
                    region else "Detection Failed"
            }
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
            
            // Only update status text if not currently initializing
            val isCurrentlyInitializing = janusManager.isInitializing.value ?: false
            if (!isCurrentlyInitializing) {
                updateStatusText(binding.statusValueText, isInitialized, "Success ✅", "Failed ❌")
            }

            // If initialized, show the current region
            if (isInitialized) {
                // Get and display the current region being used
                val currentRegion = Janus.region
                binding.regionValueText.text = if (currentRegion.isNotEmpty())
                    currentRegion else "Not Detected"
            }
        }

        // Observe initializing state to show spinner
        janusManager.isInitializing.observe(this) { isInitializing ->
            if (isInitializing) {
                // Show progress bars and loading text
                binding.statusProgressBar.visibility = View.VISIBLE
                binding.hasExperienceProgressBar.visibility = View.VISIBLE
                binding.shouldShowExperienceProgressBar.visibility = View.VISIBLE
                
                binding.statusValueText.text = "Initializing..."
                binding.statusValueText.setTextColor(
                    ContextCompat.getColor(this, R.color.status_loading)
                )
                binding.hasExperienceValueText.text = "Loading..."
                binding.hasExperienceValueText.setTextColor(
                    ContextCompat.getColor(this, R.color.status_loading)
                )
                binding.shouldShowExperienceValueText.text = "Loading..."
                binding.shouldShowExperienceValueText.setTextColor(
                    ContextCompat.getColor(this, R.color.status_loading)
                )
            } else {
                // Hide progress bars
                binding.statusProgressBar.visibility = View.GONE
                binding.hasExperienceProgressBar.visibility = View.GONE
                binding.shouldShowExperienceProgressBar.visibility = View.GONE
                
                // Update with final status when initialization completes
                val isInitialized = janusManager.isInitialized.value ?: false
                updateStatusText(binding.statusValueText, isInitialized, "Success ✅", "Failed ❌")
                
                val hasExperience = janusManager.hasExperience.value ?: false
                updateStatusText(binding.hasExperienceValueText, hasExperience, "Available ✅", "Not Available ❌")
                
                val shouldShowExperience = janusManager.shouldShowExperience.value ?: false
                updateStatusText(binding.shouldShowExperienceValueText, shouldShowExperience, "Yes ✅", "No ❌")
            }
        }

        // Observe initialization errors
        janusManager.initializationError.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                // Show error toast but don't change the UI state - let the isInitialized observer handle that
                showToast("Initialization Error: $error")
            }
        }

        // Observe has experience
        janusManager.hasExperience.observe(this) { hasExperience ->
            // Only update if not currently initializing
            val isCurrentlyInitializing = janusManager.isInitializing.value ?: false
            if (!isCurrentlyInitializing) {
                updateStatusText(binding.hasExperienceValueText, hasExperience, "Available ✅", "Not Available ❌")
            }
            
            // Toggle visibility of copy button based on experience availability
            binding.copyExperienceButton.visibility = if (hasExperience) View.VISIBLE else View.GONE
            // Enable/disable show experience button based on experience availability
            binding.showExperienceButton.isEnabled = hasExperience
        }

        // Observe should show experience
        janusManager.shouldShowExperience.observe(this) { shouldShowExperience ->
            // Only update if not currently initializing
            val isCurrentlyInitializing = janusManager.isInitializing.value ?: false
            if (!isCurrentlyInitializing) {
                updateStatusText(binding.shouldShowExperienceValueText, shouldShowExperience, "Yes ✅", "No ❌")
            }
        }

        // Observe IP detected region
        janusManager.ipDetectedRegion.observe(this) { region ->
            binding.ipRegionValueText.text = if (region.isNotEmpty()) region else "Not Detected"
        }

        // Observe current region
        janusManager.currentRegion.observe(this) { region ->
            binding.regionValueText.text = if (region.isNotEmpty()) region else "Not Set"
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

            // Set version hash
            binding.versionHashValueText.text = if (metadata.versionHash.isNotEmpty()) {
                metadata.versionHash
            } else {
                "Not Available"
            }
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