package com.ethyca.janussdk.android.example

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ethyca.janussdk.android.Janus
import com.ethyca.janussdk.android.JanusConfiguration
import com.ethyca.janussdk.android.JanusEventType
import com.ethyca.janussdk.android.consent.ConsentMetadata
import java.util.*
import android.app.Activity

/**
 * ViewModel that manages interactions with the Janus SDK
 * This is implemented as a singleton to ensure the same instance is used across activities
 */
class JanusManager : ViewModel() {
    
    // Main thread handler for updating UI
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Initialization state
    private val _isInitialized = MutableLiveData<Boolean>(false)
    val isInitialized: LiveData<Boolean> = _isInitialized
    
    private val _isInitializing = MutableLiveData<Boolean>(false)
    val isInitializing: LiveData<Boolean> = _isInitializing
    
    private val _initializationError = MutableLiveData<String?>(null)
    val initializationError: LiveData<String?> = _initializationError
    
    // Event listening
    private val _listenerId = MutableLiveData<String?>()
    val listenerId: LiveData<String?> = _listenerId
    
    private val _isListening = MutableLiveData<Boolean>(false)
    val isListening: LiveData<Boolean> = _isListening
    
    // Consent data
    private val _consentValues = MutableLiveData<Map<String, Boolean>>(emptyMap())
    val consentValues: LiveData<Map<String, Boolean>> = _consentValues
    
    private val _consentMetadata = MutableLiveData<ConsentMetadata>(ConsentMetadata())
    val consentMetadata: LiveData<ConsentMetadata> = _consentMetadata
    
    private val _fidesString = MutableLiveData<String>("")
    val fidesString: LiveData<String> = _fidesString
    
    private val _consentMethod = MutableLiveData<String>("")
    val consentMethod: LiveData<String> = _consentMethod
    
    // Experience
    private val _hasExperience = MutableLiveData<Boolean>(false)
    val hasExperience: LiveData<Boolean> = _hasExperience
    
    private val _shouldShowExperience = MutableLiveData<Boolean>(false)
    val shouldShowExperience: LiveData<Boolean> = _shouldShowExperience
    
    // Current privacy experience
    private val _currentExperience = MutableLiveData<Any?>(null) // Will be PrivacyExperienceItem when available
    val currentExperience: LiveData<Any?> = _currentExperience
    
    // Event log
    private val _events = MutableLiveData<List<String>>(emptyList())
    val events: LiveData<List<String>> = _events
    
    // Configuration
    private var currentConfig: JanusConfig? = null
    
    // Context reference for initialization
    private var appContext: Context? = null
    
    /**
     * Set the configuration and initialize the SDK
     */
    fun setConfig(config: JanusConfig, activity: Activity) {
        currentConfig = config
        appContext = activity.applicationContext
        setupJanus(activity)
    }
    
    /**
     * Initialize the Janus SDK with the current configuration
     */
    private fun setupJanus(activity: Activity) {
        val config = currentConfig ?: return
        
        // Set initializing state immediately
        mainHandler.post {
            _isInitializing.value = true
            _isInitialized.value = false
            _initializationError.value = null
            _hasExperience.value = false
            _shouldShowExperience.value = false
            _currentExperience.value = null
        }
        
        // Create the JanusConfiguration using the builder pattern
        val janusConfig = JanusConfiguration.Builder()
            .apiHost(config.apiHost)
            .propertyId(config.propertyId ?: "")
            .ipLocation(config.region == null) // Only use IP location if no region is provided
            .region(config.region)
            .webHost(config.website)
            .build()
        
        try {
            // Initialize the SDK with a single call
            Janus.initialize(activity, janusConfig) { success, error ->
                // Debug logging
                println("JanusManager: initialize callback with success=$success, error=$error")
                
                // Ensure we're on the main thread for LiveData updates
                mainHandler.post {
                    // Set these values regardless of success/failure
                    _isInitializing.value = false
                    _isInitialized.value = success
                    
                    if (error != null) {
                        println("JanusManager: Error during initialization: ${error.message}")
                        _initializationError.value = error.message
                    } else if (success) {
                        println("JanusManager: Successfully initialized, refreshing values")
                        refreshConsentValues()
                        addEventListeners()
                        _hasExperience.value = Janus.hasExperience
                    }
                }
            }
        } catch (e: Exception) {
            println("JanusManager: Exception during initialization: ${e.message}")
            mainHandler.post {
                _isInitializing.value = false
                _initializationError.value = e.message ?: "Initialization failed"
            }
        }
    }
    
    /**
     * Helper to get the configured website URL
     */
    val websiteURL: String
        get() = currentConfig?.website ?: "https://ethyca.com"
    
    /**
     * Refresh consent values from the SDK
     */
    private fun refreshConsentValues() {
        _consentValues.value = Janus.consent
        _consentMetadata.value = Janus.consentMetadata
        _hasExperience.value = Janus.hasExperience
        _shouldShowExperience.value = Janus.shouldShowExperience
        _currentExperience.value = try { Janus.currentExperience } catch (e: Exception) { null }
        _fidesString.value = Janus.fidesString
        _consentMethod.value = Janus.consentMetadata.consentMethod
    }
    
    /**
     * Show the privacy experience
     */
    fun showPrivacyExperience(activity: android.app.Activity) {
        Janus.showExperience(activity)
    }
    
    /**
     * Add event listeners to the SDK
     */
    private fun addEventListeners() {
        // Set hasExperience value
        _hasExperience.value = Janus.hasExperience
        
        val id = Janus.addConsentEventListener { event ->
            // Create a basic event description
            val eventDescription = "Event: ${event.eventType}"
            
            // Add more details as needed based on event type
            
            // Add the event to our log
            val currentEvents = _events.value ?: emptyList()
            
            // Use main thread for LiveData updates
            mainHandler.post {
                _events.value = currentEvents + eventDescription
                
                // Refresh consent values for specific events
                if (event.eventType == JanusEventType.CONSENT_UPDATED_FROM_WEBVIEW || 
                    event.eventType == JanusEventType.EXPERIENCE_SELECTION_UPDATED) {
                    refreshConsentValues()
                }
            }
        }
        
        _listenerId.value = id
        _isListening.value = true
    }
    
    /**
     * Remove event listeners from the SDK
     */
    private fun removeEventListeners() {
        val id = _listenerId.value
        if (id != null) {
            Janus.removeConsentEventListener(id)
            _listenerId.value = null
            _isListening.value = false
        }
    }
    
    /**
     * Clear the event log
     */
    fun clearEventLog() {
        _events.value = emptyList()
    }
    
    /**
     * Clear all local storage
     */
    fun clearLocalStorage(context: Context) {
        // Clear WebView storage
        android.webkit.WebStorage.getInstance().deleteAllData()
        
        // Clear cookies
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        
        // Clear Janus SDK storage
        Janus.clearConsent(clearMetadata = true)
        
        // Reset consent values
        _consentValues.value = emptyMap()
        _fidesString.value = ""
        
        // Clear events
        _events.value = emptyList()
    }
    
    /**
     * Copy the current privacy experience to the clipboard
     */
    fun copyExperienceJSON(context: Context): Boolean {
        try {
            val currentExperience = Janus.currentExperience ?: return false
            
            // Convert experience to JSON string
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(currentExperience)
            
            // Copy to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Privacy Experience", jsonString)
            clipboard.setPrimaryClip(clip)
            
            return true
        } catch (e: Exception) {
            println("Error copying experience: ${e.message}")
            return false
        }
    }
    
    /**
     * Clean up resources when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        removeEventListeners()
    }
    
    companion object {
        // Singleton instance
        private var instance: JanusManager? = null
        
        fun getInstance(): JanusManager {
            return instance ?: JanusManager().also { 
                instance = it 
            }
        }
    }
} 