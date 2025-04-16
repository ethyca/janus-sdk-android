# Janus SDK 
## Android Implementation Guide

### Installation

Add the JanusSDK maven repository to your project's `build.gradle.kts` file:

```kotlin
maven {
    url = uri("https://ethyca.github.io/janus-sdk-android")
}
```

#### Gradle (Kotlin DSL)

Add the JanusSDK dependency to your app's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.ethyca.janussdk:android:1.0.2")
}
```

If you are using a `libs.versions.toml` file, add the following entry:

```toml
[libraries]
janus-sdk = { module = "com.ethyca.janussdk:android", version = "1.0.2" }
```

Then in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.janus.sdk)
}
```

#### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.ethyca.janussdk:android:1.0.2'
}
```

### Initialization

📌 Initialize the SDK in your Application class or first Activity

Before using Janus, initialize it with a reference to an Activity. Janus must be fully initialized before any of its functions are available for use. All code that interacts with Janus should wait for the callback from `initialize()` to execute.

> **IMPORTANT:** Janus requires an Activity reference (not just a Context) for initialization. This is the activity that will be used to show the privacy experience when needed.

In addition, most of the errors from initialization will come back on this callback as an error event (see `JanusError` in the main documentation). Errors should be handled gracefully (i.e., if the region could not be determined, presenting a region selector to the user) and `initialize()` should be called again with new configuration data.

### Error Handling

The SDK provides specific error types through the `JanusError` enum that help you understand what went wrong during initialization. Handling these errors appropriately is crucial for a good user experience. For example:

- If `noRegionProvided` occurs, show a region selector to the user and reinitialize
- For `networkError`, provide a retry option
- With `invalidConfiguration`, check your configuration values for correctness

Here's a complete example of initialization with proper error handling:

```kotlin
import android.app.Activity
import com.ethyca.janussdk.android.Janus
import com.ethyca.janussdk.android.JanusConfiguration
import com.ethyca.janussdk.android.JanusError

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Janus in your first activity
        initializeJanus(this)
    }
    
    private fun initializeJanus(activity: Activity) {
        // Configure Janus
        val config = JanusConfiguration.Builder()
            .apiHost("https://privacy-center.yourhost.com")
            .propertyId("FDS-A0B1C2")
            .ipLocation(true)
            .region("US-CA")
            .fidesEvents(true)
            .webHost("https://yourhost.com")
            .build()
        
        // Initialize Janus with the activity reference
        Janus.initialize(activity, config) { success, error ->
            if (success) {
                // ✅ Initialization complete, Janus is now ready to use
                // If shouldShowExperience is true, privacy experience will automatically show
            } else if (error is JanusError) {
                // Handle specific error types
                when (error) {
                    is JanusError.NoRegionProvided -> {
                        // Show region selector to user, then reinitialize with selected region
                        presentRegionSelector { selectedRegion ->
                            val newConfig = JanusConfiguration.Builder()
                                .apiHost(config.apiHost)
                                .propertyId(config.propertyId)
                                .ipLocation(false)
                                .region(selectedRegion)
                                .fidesEvents(config.fidesEvents)
                                .webHost(config.webHost)
                                .build()
                            Janus.initialize(activity, newConfig) { /* handle result */ }
                        }
                    }
                    is JanusError.NetworkError -> {
                        // Show network error and retry option
                        presentNetworkError(error.cause) {
                            Janus.initialize(activity, config) { /* handle result */ }
                        }
                    }
                    is JanusError.InvalidConfiguration -> {
                        // Log the error and check configuration values
                        Log.e("Janus", "Invalid configuration provided: $config")
                    }
                    is JanusError.ApiError -> {
                        // Handle API-specific errors
                        Log.e("Janus", "API error occurred: ${error.message}")
                    }
                    is JanusError.InvalidRegion -> {
                        // Handle invalid region code
                        Log.e("Janus", "Invalid region code provided: ${config.region}")
                    }
                    is JanusError.InvalidExperience -> {
                        // Handle missing or invalid experience data
                        Log.e("Janus", "Invalid or missing privacy experience data")
                    }
                    else -> {
                        // Generic error handling
                        Log.e("Janus", "An unexpected error occurred: ${error.message}")
                    }
                }
            }
        }
    }
    
    // Example UI helper methods (not part of JanusSDK)
    private fun presentRegionSelector(callback: (String) -> Unit) {
        // Your implementation to present a region selector UI
    }
    
    private fun presentNetworkError(error: Throwable?, retryCallback: () -> Unit) {
        // Your implementation to present a network error UI with retry
    }
}
```

> Note: The `presentRegionSelector` and `presentNetworkError` functions in the example above are placeholders for your app's UI components and are not part of the JanusSDK.

📌 Sample Configuration

```kotlin
// Configure Janus with required credentials and settings
val config = JanusConfiguration.Builder()
    .apiHost("https://privacy-center.yourhost.com")  // 🌎 Fides base URL
    .propertyId("FDS-A0B1C2")                        // 🏢 Property identifier for this app
    .ipLocation(true)                                // 📍 Use IP-based geolocation
    .region("US-CA")                                 // 🌎 Provide if geolocation is false or fails
    .fidesEvents(true)                               // 🔄 Map JanusEvents to FidesJS events in WebViews
    .webHost("https://yourhost.com")                 // 🇪🇺 Required for TCF (optional for non-TCF)
    .build()

// Initialize with an Activity reference
Janus.initialize(yourActivity, config) { success, error ->
    // Handle initialization result
}
```

### Display Privacy Notice

📌 Subscribe to Consent Events

```kotlin
val listenerId = Janus.addConsentEventListener { event ->
    // ✅ Handle consent event by event.type
    // additional properties may be available on event.detail
}

// ✅ Remove the event listener when no longer needed
Janus.removeConsentEventListener(listenerId)
```

📌 Show the Privacy Notice

```kotlin
// Example of conditionally showing a button based on hasExperience
// In a Fragment or Activity:
if (Janus.hasExperience) {
    privacyButton.visibility = View.VISIBLE
    privacyButton.setOnClickListener {
        Janus.showExperience(this) // 'this' is your Activity
    }
} else {
    privacyButton.visibility = View.GONE
}

// The showExperience method already checks hasExperience internally,
// so you don't need to check it again before calling the method:
fun showPrivacySettings() {
    Janus.showExperience(this) // 'this' should be your Activity
}
```

### Check Consent Status

```kotlin
// Get a single consent value
val analyticsConsent = Janus.consent["analytics"] ?: false

// Get all the user's consent choices
val consent = Janus.consent

// Get Fides string (List of IAB strings like CPzHq4APzHq4AAMABBENAUEAALAAAEOAAAAAAEAEACACAAAA,1~61.70)
val fidesString = Janus.fidesString
```

### WebView Integration

```kotlin
// Get an auto-syncing WebView instance
val webView = Janus.createConsentWebView(context)

// Load the WebView with an application that includes FidesJS
webView.loadUrl("https://your-fides-enabled-url.com")

// IMPORTANT: Release the WebView when you're done with it to prevent memory leaks
// This is typically done in onDestroy or when the view is being destroyed
override fun onDestroy() {
    super.onDestroy()
    Janus.releaseConsentWebView(webView)
}
```

⚠️ **Important:** Always call `releaseConsentWebView()` when you're done with a WebView to prevent memory leaks. WebView's JavaScript interfaces require explicit cleanup, and failing to release the WebView properly can lead to resource issues.

### Jetpack Compose Integration

If your app uses Jetpack Compose, you can use the WebView integration as follows:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ConsentWebView(url: String) {
    val context = LocalContext.current
    val webView = remember { Janus.createConsentWebView(context) }
    
    DisposableEffect(key1 = webView) {
        webView.loadUrl(url)
        
        onDispose {
            // Clean up when the composable is disposed
            Janus.releaseConsentWebView(webView)
        }
    }
    
    AndroidView(
        factory = { webView },
        update = { /* Updates can be handled here if needed */ }
    )
}
```

### Full Example

Here's a complete example showing how to integrate JanusSDK in a typical Android activity:

```kotlin
class MainActivity : AppCompatActivity() {
    private var listenerId: String? = null
    private var consentWebView: WebView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Janus with this activity
        val config = JanusConfiguration.Builder()
            .apiHost("https://privacy-center.yourhost.com")
            .propertyId("FDS-A0B1C2")
            .ipLocation(true)
            .build()
            
        Janus.initialize(this, config) { success, error ->
            if (success) {
                // Set up UI based on consent status
                updateConsentUI()
                
                // Set up privacy button
                findViewById<Button>(R.id.privacy_button).apply {
                    visibility = if (Janus.hasExperience) View.VISIBLE else View.GONE
                    setOnClickListener { 
                        Janus.showExperience(this@MainActivity) 
                    }
                }
                
                // Listen for consent events
                listenerId = Janus.addConsentEventListener { event ->
                    when (event.eventType) {
                        JanusEventType.EXPERIENCE_SELECTION_UPDATED -> {
                            // Update UI when consent changes
                            updateConsentUI()
                        }
                        // Handle other events...
                    }
                }
                
                // Set up a WebView that syncs with consent
                setupConsentWebView()
            } else {
                // Handle initialization error
                Toast.makeText(this, "Error: ${error?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateConsentUI() {
        // Example: Update UI based on specific consent values
        findViewById<TextView>(R.id.analytics_status).text = 
            "Analytics consent: ${Janus.consent["analytics"] ?: false}"
    }
    
    private fun setupConsentWebView() {
        consentWebView = Janus.createConsentWebView(this).apply {
            // Configure WebView settings if needed
            settings.javaScriptEnabled = true
            
            // Load a URL that includes FidesJS
            loadUrl("https://your-fides-enabled-url.com")
        }
        
        // Add WebView to your layout
        val webViewContainer = findViewById<FrameLayout>(R.id.webview_container)
        webViewContainer.addView(consentWebView)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        listenerId?.let { Janus.removeConsentEventListener(it) }
        consentWebView?.let { Janus.releaseConsentWebView(it) }
    }
}
```