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
        
        // Setup input fields
        setupInputFields()
        
        // Setup buttons
        setupButtons()
        
        // Observe initialization state
        observeJanusState()
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
    
    private fun setupInputFields() {
        // Set initial values
        updateInputFields()
        
        // Setup text change listeners
        binding.apiHostInput.doOnTextChanged { text, _, _, _ ->
            if (currentConfig.type == ConfigurationType.CUSTOM) {
                currentConfig.apiHost = text.toString()
            }
        }
        
        binding.websiteInput.doOnTextChanged { text, _, _, _ ->
            if (currentConfig.type == ConfigurationType.CUSTOM) {
                currentConfig.website = text.toString()
            }
        }
        
        binding.propertyIdInput.doOnTextChanged { text, _, _, _ ->
            if (currentConfig.type == ConfigurationType.CUSTOM) {
                currentConfig.propertyId = text.toString().ifEmpty { null }
            }
        }
        
        binding.regionInput.doOnTextChanged { text, _, _, _ ->
            currentConfig.region = text.toString().ifEmpty { null }
        }
        
        // Set initial enabled state
        updateInputFieldsEnabled()
    }
    
    private fun updateInputFields() {
        binding.apiHostInput.setText(currentConfig.apiHost)
        binding.websiteInput.setText(currentConfig.website)
        binding.propertyIdInput.setText(currentConfig.propertyId ?: "")
        binding.regionInput.setText(currentConfig.region ?: "")
    }
    
    private fun updateInputFieldsEnabled() {
        val isCustom = currentConfig.type == ConfigurationType.CUSTOM
        
        binding.apiHostInput.isEnabled = isCustom
        binding.websiteInput.isEnabled = isCustom
        binding.propertyIdInput.isEnabled = isCustom
        // Region is always editable to override IP location
        binding.regionInput.isEnabled = true
    }
    
    private fun setupButtons() {
        binding.launchButton.setOnClickListener {
            if (isLaunchEnabled()) {
                // Save config if it's custom
                currentConfig.save(this)
                
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
    
    private fun observeJanusState() {
        // Hide the loading indicator initially
        binding.loadingProgressBar.visibility = View.GONE
        
        // Observe initialization state
        janusManager.isInitializing.observe(this) { isInitializing ->
            binding.loadingProgressBar.visibility = if (isInitializing) View.VISIBLE else View.GONE
        }
        
        // Observe initialization result
        janusManager.isInitialized.observe(this) { isInitialized ->
            if (isInitialized) {
                // Navigate to details screen
                startActivity(FullExampleActivity.createIntent(this))
            }
        }
        
        // Observe initialization error
        janusManager.initializationError.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }
}