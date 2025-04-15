package com.ethyca.janussdk.android.example

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.webkit.WebView
import com.ethyca.janussdk.android.webview.WebViewManager
import androidx.activity.OnBackPressedCallback

/**
 * Activity for displaying a full-screen WebView with Janus SDK integration
 */
class WebViewActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private lateinit var toolbar: Toolbar
    
    // URL to load in the WebView - passed as an extra
    private var websiteUrl: String? = null
    
    // Whether to auto-sync consent on start - passed as an extra
    private var autoSyncOnStart: Boolean = true
    
    companion object {
        const val EXTRA_WEBSITE_URL = "extra_website_url"
        const val EXTRA_AUTO_SYNC = "extra_auto_sync"
        private const val MENU_ITEM_SHOW_MODAL = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        
        // Get extras from intent
        websiteUrl = intent.getStringExtra(EXTRA_WEBSITE_URL)
        autoSyncOnStart = intent.getBooleanExtra(EXTRA_AUTO_SYNC, true)
        
        // Set up toolbar
        setupToolbar()
        
        // Initialize and display the WebView
        initializeWebView()
        
        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                releaseWebView()
                finish()
            }
        })
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Add a menu item for showing the modal
        menu.add(Menu.NONE, MENU_ITEM_SHOW_MODAL, Menu.NONE, "Privacy Settings")
            .setIcon(android.R.drawable.ic_menu_preferences)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            MENU_ITEM_SHOW_MODAL -> {
                showPrivacyModal()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // Enable back button
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "WebView"
        }
    }
    
    private fun showPrivacyModal() {
        // Execute JavaScript to show the Fides modal
        webView?.evaluateJavascript("window.Fides?.showModal()", null)
        Toast.makeText(this, "Show modal requested", Toast.LENGTH_SHORT).show()
    }
    
    private fun initializeWebView() {
        // Create WebView using the WebViewManager
        val consentWebView = WebViewManager.createConsentWebView(
            context = this,
            autoSyncOnStart = autoSyncOnStart
        )
        
        // Store reference
        webView = consentWebView
        
        // Add to the container
        val container = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.webViewContainer)
        container.addView(consentWebView)
        
        // Load the website URL
        websiteUrl?.let { url ->
            if (url.isNotEmpty()) {
                consentWebView.loadUrl(url)
                println("WebViewActivity: Loading URL: $url")
            } else {
                showError("Empty URL provided")
            }
        } ?: run {
            // Show error message if no URL
            showError("No URL provided")
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun releaseWebView() {
        webView?.let {
            WebViewManager.releaseConsentWebView(it)
            webView = null
        }
    }
    
    override fun onDestroy() {
        releaseWebView()
        super.onDestroy()
    }
} 