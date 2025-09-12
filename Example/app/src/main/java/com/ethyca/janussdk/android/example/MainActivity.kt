package com.ethyca.janussdk.android.example

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.ethyca.janussdk.android.example.databinding.ActivityMainBinding
import com.ethyca.janussdk.android.models.ConsentFlagType
import com.ethyca.janussdk.android.models.ConsentNonApplicableFlagMode

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val janusManager: JanusManager = JanusManager.getInstance()
    private var currentConfig = JanusConfig()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        
        // Setup configuration type spinner
        setupConfigSpinner()
        
        // Setup consent flag type spinner
        setupConsentFlagTypeSpinner()
        
        // Setup consent non-applicable flag mode spinner
        setupConsentNonApplicableFlagModeSpinner()
        
        // Setup input fields
        setupInputFields()
        
        // Setup buttons
        setupButtons()
    }
    
    private fun setupConfigSpinner() {
        val configTypes = ConfigurationType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, configTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.configTypeSpinner.adapter = adapter
        binding.configTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedType = ConfigurationType.values()[position]
                currentConfig = JanusConfig.forType(selectedType, this@MainActivity)
                updateInputFields()
                updateInputFieldsEnabled()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }
    
    private fun setupConsentFlagTypeSpinner() {
        val consentFlagTypes = ConsentFlagType.values().map { flagType ->
            when (flagType) {
                ConsentFlagType.BOOLEAN -> "Boolean"
                ConsentFlagType.CONSENT_MECHANISM -> "Consent Mechanism"
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, consentFlagTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.consentFlagTypeSpinner.adapter = adapter
        binding.consentFlagTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFlagType = ConsentFlagType.values()[position]
                currentConfig.consentFlagType = selectedFlagType
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }
    
    private fun setupConsentNonApplicableFlagModeSpinner() {
        val consentNonApplicableFlagModes = ConsentNonApplicableFlagMode.values().map { flagMode ->
            when (flagMode) {
                ConsentNonApplicableFlagMode.OMIT -> "Omit"
                ConsentNonApplicableFlagMode.INCLUDE -> "Include"
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, consentNonApplicableFlagModes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.consentNonApplicableFlagModeSpinner.adapter = adapter
        binding.consentNonApplicableFlagModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFlagMode = ConsentNonApplicableFlagMode.values()[position]
                currentConfig.consentNonApplicableFlagMode = selectedFlagMode
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }
    
    private fun setupInputFields() {
        // Set initial values
        updateInputFields()
        
        // Setup text change listeners
        binding.apiHostInput.doOnTextChanged { text, _, _, _ ->
            if (currentConfig.type == ConfigurationType.CUSTOM) {
                currentConfig.apiHost = text.toString()
            }
        }
        
        binding.privacyCenterHostInput.doOnTextChanged { text, _, _, _ ->
            if (currentConfig.type == ConfigurationType.CUSTOM) {
                currentConfig.privacyCenterHost = text.toString()
            }
        }
        
        binding.websiteInput.doOnTextChanged { text, _, _, _ ->
            if (currentConfig.type == ConfigurationType.CUSTOM) {
                currentConfig.website = text.toString()
            }
        }
        
        binding.propertyIdInput.doOnTextChanged { text, _, _, _ ->
            currentConfig.propertyId = text.toString().ifEmpty { null }
        }
        
        binding.regionInput.doOnTextChanged { text, _, _, _ ->
            currentConfig.region = text.toString().ifEmpty { null }
        }
        
        // Setup autoShowExperience switch listener
        binding.autoShowExperienceSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentConfig.autoShowExperience = isChecked
        }
        
        // Set initial enabled state
        updateInputFieldsEnabled()
    }
    
    private fun updateInputFields() {
        binding.apiHostInput.setText(currentConfig.apiHost)
        binding.privacyCenterHostInput.setText(currentConfig.privacyCenterHost)
        binding.websiteInput.setText(currentConfig.website)
        binding.propertyIdInput.setText(currentConfig.propertyId ?: "")
        binding.regionInput.setText(currentConfig.region ?: "")
        binding.autoShowExperienceSwitch.isChecked = currentConfig.autoShowExperience
        
        // Set consent flag type spinner selection
        val consentFlagTypeIndex = ConsentFlagType.values().indexOf(currentConfig.consentFlagType)
        if (consentFlagTypeIndex >= 0) {
            binding.consentFlagTypeSpinner.setSelection(consentFlagTypeIndex)
        }
        
        // Set consent non-applicable flag mode spinner selection
        val consentNonApplicableFlagModeIndex = ConsentNonApplicableFlagMode.values().indexOf(currentConfig.consentNonApplicableFlagMode)
        if (consentNonApplicableFlagModeIndex >= 0) {
            binding.consentNonApplicableFlagModeSpinner.setSelection(consentNonApplicableFlagModeIndex)
        }
    }
    
    private fun updateInputFieldsEnabled() {
        val isCustom = currentConfig.type == ConfigurationType.CUSTOM
        
        binding.apiHostInput.isEnabled = isCustom
        binding.privacyCenterHostInput.isEnabled = isCustom
        binding.websiteInput.isEnabled = isCustom
        // Property ID is always editable to override static configuration
        binding.propertyIdInput.isEnabled = true
        // Region is always editable to override IP location
        binding.regionInput.isEnabled = true
    }
    
    private fun setupButtons() {
        binding.launchButton.setOnClickListener {
            if (isLaunchEnabled()) {
                // Save config if it's custom
                currentConfig.save(this)
                
                // Hide the loading indicator initially
                binding.loadingProgressBar.visibility = View.GONE
                
                // Set up observers before initializing
                setupJanusObservers()
                
                // Initialize Janus with context
                janusManager.setConfig(currentConfig, this)
            } else {
                Toast.makeText(this, "Please provide API Host and Website URL", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.clearButton.setOnClickListener {
            janusManager.clearLocalStorage(this)
            Toast.makeText(this, "Storage cleared", Toast.LENGTH_SHORT).show()
        }
        
        binding.getLocationByIPButton.setOnClickListener {
            // Set loading state, button remains clickable
            binding.getLocationByIPButton.isEnabled = false
            binding.getLocationByIPButton.text = "Loading..."
            
            janusManager.getIPLocationDetails { response ->
                // Set the response text and make it visible
                binding.ipLocationResponseText.text = response
                binding.ipLocationResponseText.visibility = View.VISIBLE
                
                // Reset button state
                binding.getLocationByIPButton.isEnabled = true
                binding.getLocationByIPButton.text = "Get Location by IP Address"
            }
        }
    }
    
    private fun isLaunchEnabled(): Boolean {
        return if (currentConfig.type == ConfigurationType.CUSTOM) {
            currentConfig.apiHost.isNotEmpty() && currentConfig.website.isNotEmpty()
        } else {
            true
        }
    }
    
    private fun setupJanusObservers() {
        // Flag to track if we've already navigated
        var hasNavigated = false
        
        // Observe initialization state
        janusManager.isInitializing.observe(this) { isInitializing ->
            binding.loadingProgressBar.visibility = if (isInitializing) View.VISIBLE else View.GONE
            
            // When initialization finishes (with success or failure), navigate only once
            if (!isInitializing && !hasNavigated) {
                // We only navigate here if initialization has finished (regardless of result)
                hasNavigated = true
                startActivity(FullExampleActivity.createIntent(this))
            }
        }
        
        // Observe initialization error (just for showing toast, no navigation)
        janusManager.initializationError.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }
}
