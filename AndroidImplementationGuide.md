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
    implementation("com.ethyca.janussdk:android:1.4.1")
}
```

If you are using a `libs.versions.toml` file, add the following entry:

```toml
[libraries]
janus-sdk = { module = "com.ethyca.janussdk:android", version = "1.4.1" }
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
    implementation 'com.ethyca.janussdk:android:1.4.1'
}
```

### Custom Logging

The Janus SDK supports custom logging implementations through the `JanusLogger` interface. This is useful for debugging, monitoring, and integrating with your app's existing logging infrastructure.

#### JanusLogger Interface

```kotlin
interface JanusLogger {
    fun log(
        level: JanusLogLevel,
        message: String,
        metadata: Map<String, String>? = null,
        error: Throwable? = null
    )
}

enum class JanusLogLevel {
    VERBOSE, DEBUG, INFO, WARNING, ERROR
}
```

#### Setting a Custom Logger

If you have implemented your own custom logger implementation, be sure to call setLogger() prior to initialize() in order to receive logs that occur during the initialization of the SDK.

```kotlin
// Set custom logger BEFORE initializing Janus
val myCustomLogger = MyCustomJanusLogger()
Janus.setLogger(myCustomLogger)

// Now initialize Janus - logs during initialization will use your custom logger
val config = JanusConfiguration.Builder()
    .apiHost("https://privacy-plus.yourhost.com")
    .propertyId("FDS-A0B1C2")
    .build()
    
Janus.initialize(this, config) { success, error ->
    // Handle initialization result
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
            .apiHost("https://privacy-plus.yourhost.com")
            .privacyCenterHost("https://privacy-center.yourhost.com")
            .propertyId("FDS-A0B1C2")
            .ipLocation(true)
            .region("US-CA")
            .fidesEvents(true)
            .autoShowExperience(true)
            .consentFlagType(ConsentFlagType.BOOLEAN)
            .consentNonApplicableFlagMode(ConsentNonApplicableFlagMode.OMIT)
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
                                .privacyCenterHost(config.privacyCenterHost)
                                .propertyId(config.propertyId)
                                .ipLocation(false)
                                .region(selectedRegion)
                                .fidesEvents(config.fidesEvents)
                                .autoShowExperience(config.autoShowExperience)
                                .consentFlagType(config.consentFlagType)
                                .consentNonApplicableFlagMode(config.consentNonApplicableFlagMode)
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
    .apiHost("https://privacy-plus.yourhost.com")             // 🌎 FidesPlus API server base URL (REQUIRED)
    .privacyCenterHost("https://privacy-center.yourhost.com") // 🏢 Privacy Center host URL - if not provided, Janus will use the apiHost
    .propertyId("FDS-A0B1C2")                                 // 🏢 Property identifier for this app
    .ipLocation(true)                                         // 📍 Use IP-based geolocation (default true)
    .region("US-CA")                                          // 🌎 Provide if geolocation is false or fails
    .fidesEvents(true)                                        // 🔄 Map JanusEvents to FidesJS events in WebViews (default true)
    .autoShowExperience(true)                                 // 🚀 Automatically show privacy experience after initialization (default true)
    .saveUserPreferencesToFides(true)                         // 💾 Save user preferences to Fides via privacy-preferences API (default true)
    .saveNoticesServedToFides(true)                           // 💾 Save notices served to Fides via notices-served API (default true)
    .consentFlagType(ConsentFlagType.BOOLEAN)                 // 🎯 Format for consent values (default boolean)
    .consentNonApplicableFlagMode(ConsentNonApplicableFlagMode.OMIT)  // 🔄 Handle non-applicable notices (default omit)
    .enableBannerMode(true)                                   // 🪧 Show banner_and_modal experiences as a compact banner first (default false)
    .enableAgeSignal(false)                                   // 🧒 Read the age bracket from Google Play Age Signals during init (default false)
    .ageBracket(null)                                         // 🧒 Age bracket your app already knows; wins over Play Age Signals (default null)
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

// Get Fides string in format TC_STRING,AC_STRING,GPP_STRING,NC_STRING
// TC_STRING: IAB TCF string, AC_STRING: Google Additional Consent, 
// GPP_STRING: IAB GPP string, NC_STRING: Base64 Notice Consent preferences
val fidesString = Janus.fidesString
```

### Programmatic Consent Updates

Use `setConsent()` to write consent values directly to the SDK from native code — without showing the privacy experience UI. This is intended for two scenarios:

1. **Bidirectional WebView sync** — your app hosts a FidesJS page in a custom `WebView` and needs to push web-originated consent into native storage via a JS bridge.
2. **Legacy consent migration** — users already consented through an older flow; carry their choice into the SDK so they aren't re-prompted. Call `setConsent()` after `initialize` completes and before the experience shows, with `autoShowExperience` turned off. `setConsent()` only saves to Fides when an experience is loaded, so a call made before then stores consent on the device and never sends it to Fides. Pass the external provider consent method so Fides records the consent as carried over.

```kotlin
// Minimal call — local persist only, no server sync
Janus.setConsent(values = mapOf("analytics" to true, "marketing" to false))

// With a fides string (e.g. from a FidesJS FidesUpdated event)
Janus.setConsent(
    values = mapOf("analytics" to true),
    fidesString = "CPz4OMAP...",
    consentMethod = ConsentMethod.FIDES_JS_UPDATE
)

// With server sync — requires an experience to be loaded
// Silently no-ops the network call if no experience is available
Janus.setConsent(
    values = mapOf("analytics" to true),
    consentMethod = ConsentMethod.SAVE,
    saveToFides = true
)

// Migrating consent recorded before Janus. Call after initialize completes: the Fides save needs a loaded experience
// The method lets Fides tell it apart from a choice made in the experience
Janus.setConsent(
    values = mapOf("data_sales_and_sharing" to false),
    consentMethod = ConsentMethod.EXTERNAL_PROVIDER,
    saveToFides = true
)
```

A `JanusEvent` (`EXPERIENCE_SELECTION_UPDATED`) is fired to all registered listeners after consent is written, identical to the event fired when a user submits the privacy experience UI.

`clearConsent()` resets all stored consent values and optionally clears metadata timestamps:

```kotlin
Janus.clearConsent()                       // clear values, keep timestamps
Janus.clearConsent(clearMetadata = true)   // clear everything
```

### External User ID

Use `setExternalId()` to attach your application's own user identifier (e.g. an Auth0 `sub` or database UUID) to every consent and notices-served request the SDK sends to Fides. Fides stores this on the `BrowserIdentity` of each request, letting it link consent records to a known user — which enables DSR (data subject request) correlation and consent merging across a user's devices.

Call it after the user logs in. Pass `null` to clear the identifier on logout. The value is held in memory for the lifetime of the SDK and applied to all *subsequent* API calls; it does not retroactively update records already sent.

```kotlin
// After login — associate consent with your app's user ID
Janus.setExternalId("auth0|abc123")

// On logout — stop associating consent with the user
Janus.setExternalId(null)
```

### Region Detection and Access

JanusSDK provides methods to work directly with region detection and access the current region:

```kotlin
// Get the user's region by IP geolocation
Janus.getLocationByIPAddress { success, locationData, error ->
    if (success && locationData != null) {
        // Use the full location data
        val isoRegion = locationData.location // Format: "US-CA"
        val country = locationData.country    // Format: "US"
        val subRegion = locationData.region   // Format: "CA"
        val ipAddress = locationData.ip       // Format: "192.168.1.1"
        
        // Update UI with region information
        updateRegionUI(isoRegion)
    } else if (error != null) {
        // Handle specific errors
        when (error) {
            is JanusError.NetworkError -> showNetworkError()
            is JanusError.IPLocationFailed -> showLocationDetectionFailed()
            else -> showGenericError()
        }
    }
}

// Access the current region being used by the SDK (after initialization)
val currentRegion = Janus.region
regionTextView.text = "Current Region: $currentRegion"
```

The `getLocationByIPAddress` method is particularly useful when:
- You want to show region information to users before showing a privacy experience
- You need to implement custom region selection UI based on detected region
- You want to give users the option to correct their detected region

The `region` property returns the region code that the SDK is currently using, which may come from:
- The region specified in the configuration during initialization
- The region detected via IP geolocation
- Empty string if no region has been determined yet

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

⚠️ **WebViewClient Warning:**
If you need to implement your own WebViewClient on a WebView created by Janus, you must ensure that the onPageFinished() method from the original client is still called.
Janus injects critical JavaScript after each page load — overriding the WebViewClient without preserving this behavior will break consent synchronization.
✅ To safely implement a custom client, use the following pattern:

```kotlin
val webView = Janus.createConsentWebView(context)

// Store a reference to the original Janus WebViewClient before replacing it
val originalWebViewClient = webView.webViewClient

// Set your custom WebViewClient that delegates to the original one
webView.webViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        // Call the original Janus WebViewClient for this event
        originalWebViewClient.onPageFinished(view, url)
    }
}
```

#### Bidirectional Consent Sync & Loop Prevention

Janus WebViews are **bidirectional**: consent changes flow both ways between native storage and any FidesJS-powered page.

- **Native → Web**: When consent changes in native code (via the privacy experience, `clearConsent()`, or `setConsent()`), Janus injects an `UpdateConsent` message into all managed WebViews so FidesJS reflects the new state.
- **Web → Native**: When a user interacts with a FidesJS UI inside a managed WebView, the JS bridge (`window.JanusSDKBridge`) forwards the consent values back to native storage and fires a `JanusEvent` to all registered listeners.

**How loops are prevented:**

1. **Source exclusion** — When a consent update originates *from* a WebView, Janus propagates the change to all *other* managed WebViews but skips the source WebView. Programmatic calls (e.g. `setConsent()`) have no source WebView and propagate to all. The exception is a save the minor age signal turned back: Janus then sends its values to every managed WebView, the source included, and the bridge ignores the `FidesUpdated` events these Janus-sourced updates cause.
2. **FidesJS idempotency** — FidesJS does not re-dispatch a `FidesUpdated` event when the incoming consent values are identical to the current state. This prevents an echo from triggering a second round-trip back to native.

These two layers work together so that a WebView-initiated consent change does not bounce indefinitely between FidesJS and native code.

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
            .consentFlagType(ConsentFlagType.BOOLEAN)
            .consentNonApplicableFlagMode(ConsentNonApplicableFlagMode.OMIT)
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

### Controlling Privacy Experience Display

By default, Janus will automatically show the privacy experience after successful initialization if `shouldShowExperience` returns true. You can control this behavior with the `autoShowExperience` configuration parameter.

#### Option 1: Automatic display (default)

```kotlin
// With autoShowExperience set to true (default), Janus will automatically
// show the privacy experience after initialization if shouldShowExperience is true
val config = JanusConfiguration.Builder()
    .apiHost("https://privacy-plus.yourhost.com")
    // Other parameters...
    .autoShowExperience(true) // Default behavior
    .build()
```

#### Option 2: Manual control

```kotlin
// Disable automatic display by setting autoShowExperience to false
val config = JanusConfiguration.Builder()
    .apiHost("https://privacy-plus.yourhost.com")
    // Other parameters...
    .autoShowExperience(false) // Prevent automatic display
    .consentFlagType(ConsentFlagType.BOOLEAN)
    .consentNonApplicableFlagMode(ConsentNonApplicableFlagMode.OMIT)
    .build()

// Initialize Janus without showing the privacy experience immediately
Janus.initialize(activity, config) { success, error ->
    if (success) {
        // You can now decide when to show the experience
        
        // Check if the experience should be shown (based on consent status, etc.)
        if (Janus.shouldShowExperience) {
            // Show at the appropriate time in your app flow
            Handler(Looper.getMainLooper()).postDelayed({
                Janus.showExperience(activity)
            }, 2000) // 2-second delay example
        }
    }
}
```

### Banner Mode

By default, Janus shows every non-TCF experience as a full-screen modal. Call `.enableBannerMode(true)` on the builder (or set `enableBannerMode = true` in the DSL) to show `banner_and_modal` experiences as a compact banner first, matching the layered banner on the web. The banner offers Reject All, Accept All, and a Manage preferences button that opens the full modal.

```kotlin
val config = JanusConfiguration.Builder()
    .apiHost("https://privacy-plus.yourhost.com")
    .propertyId("FDS-A0B1C2")
    .enableBannerMode(true)
    .build()
```

When banner mode is on:

1. **The banner appears only while consent is still needed**, i.e. when `shouldShowExperience` is true. Once the user has made a choice, `showExperience` opens the full modal directly, so a "Privacy settings" link in your app skips the banner. Dismissing the banner (close button or back) saves each notice's default as implied consent (consent method `dismiss`) when the user has no saved consent yet. Implied consent doesn't count as a choice, so the banner resurfaces on the next `showExperience`, as it does on the web.
2. **Text and labels come from the experience config.** The banner uses `banner_title` and `banner_description`, falling back to `title` and `description`, and the button labels from the same translation.
3. **`layer1_button_options` picks the buttons.** `acknowledge`, or an experience whose notices are all notice-only, shows a single Acknowledge button. `opt_in_opt_out` and `gpc_conditional` show Reject All and Accept All (apps have no GPC signal).
4. **Notices served are recorded with `serving_component: banner`**, and the modal opened from the banner records `modal`.
5. **Back and close dismiss only when the experience is dismissable.** A non-dismissable banner stays until the user makes a choice.

Other component types, including `modal` and TCF experiences, are unaffected by this flag.

### Minor Age Signal

App store age laws (California's AB 1043 from 2027, plus Texas, Utah, and Louisiana) send each app an age bracket for its user. Once an app has that signal, the CCPA treats it as knowing the user's age, so it must not sell or share a minor's data for targeted ads without opt-in. Janus can apply this for you: when the user is under 16, it opts every targeted-ads notice out before it shows an experience or reports consent.

Janus treats a notice as a targeted-ads notice when it has the GPC flag (`has_gpc_flag`) and is not notice-only. You choose these notices through your existing notice configuration.

#### Where the bracket comes from

There are two sources:

1. **Google Play Age Signals.** Set `enableAgeSignal(true)`, and Janus asks Play for the user's age range during `initialize`, before its first auto-show. Play may show its age-sharing prompt. Janus waits up to 5 seconds. If Play fails, times out, the user declines to share, or the library is missing, the bracket is `unknown` and initialization completes normally. If Play's answer arrives after the 5 seconds, Janus still applies it for this launch. If the Activity you pass to `initialize` is finishing, Janus asks from the next resumed Activity instead, and a failed request is retried once from the next resumed Activity.
2. **Your app.** If you already know the user's age range, pass `ageBracket` at init, or call `Janus.setAgeSignal()` when it arrives later. A bracket from your app wins over the Play result, except `UNKNOWN`, which defers to Play. When your app passes a known bracket before `initialize`, Janus doesn't ask Play at all, so the user never sees a prompt whose answer would be ignored. If your app later calls `setAgeSignal(AgeBracket.UNKNOWN)` with `enableAgeSignal` on, Janus asks Play then, from the current resumed Activity, and applies the answer live.

The Play library is an optional dependency, so apps that don't enable the age signal don't pull it in. To use `enableAgeSignal`, add it yourself (it requires `minSdk` 23):

```kotlin
dependencies {
    implementation("com.google.android.play:age-signals:0.0.4")
}
```

```kotlin
// Source 1: Janus reads Play Age Signals during initialize
val config = JanusConfiguration.Builder()
    .apiHost("https://privacy-plus.yourhost.com")
    .propertyId("FDS-A0B1C2")
    .enableAgeSignal(true)
    .build()

// Source 2: your app already has the bracket
val config = JanusConfiguration.Builder()
    .apiHost("https://privacy-plus.yourhost.com")
    .propertyId("FDS-A0B1C2")
    .ageBracket(AgeBracket.AGE_13_TO_15)
    .build()

// Or, for a bracket that arrives after initialize
Janus.setAgeSignal(AgeBracket.AGE_13_TO_15)
```

A bracket passed through `setAgeSignal()` after `initialize` can arrive after an experience has already shown. To close that gap, set `autoShowExperience(false)` and call `showExperience()` once you have called `setAgeSignal()`.

Janus maps Play's age range to a bracket with gates at 13 and 16. If you set custom age ranges in the Play Console, a range that spans a gate maps to the youngest bracket it covers.

#### What each bracket does

| Bracket | Auto opt-out | Toggle in the Janus UI | Accept All | Opting back in |
|---|---|---|---|---|
| `UNDER_13` | Yes | Locked off (a parent's opt-in can still be turned off) | Skips these notices | Your app records parental consent |
| `AGE_13_TO_15` | Yes | Unlocked | Skips these notices | The user flips the individual toggle |
| `AGE_16_TO_17`, `ADULT`, `UNKNOWN` | No | Normal | Normal | n/a |

1. **The opt-out applies once per notice.** The first time a minor bracket meets a notice, Janus turns it off even if it was on, and remembers the notice key on the device. After that, Janus leaves the notice alone, so a 13 to 15 year old's opt-back-in survives relaunch. If the notice later has no stored value at all, Janus turns it off again.
2. **The signal only ever turns consent off.** When the bracket changes to a non-minor value (for example, the user turns 16), notices unlock and keep their current values.
3. **Opt-outs are saved to Fides** with consent method `age_signal` when `saveUserPreferencesToFides` is on. An older Fides server that doesn't know `age_signal` or `parental_consent` rejects the save with a 422, so Janus retries it once without a consent method.
4. **The bracket never leaves the device.** Janus keeps it in memory only. It never stores it, never sends it to Fides, and never writes it to a log.

#### Parental consent for users under 13

An under-13 notice can only be turned on through your app, after your app has verified the parent's consent. Janus clamps every other path, including `setConsent()` with any other method, Accept All, and WebView messages. In a WebView, only the user's own choices in the FidesJS modal (saving it, or a single notice's toggle) can opt a 13 to 15 year old in; Accept All, a dismissed banner, a `fides_string`, the page's own `Fides.updateConsent` call, migrated consent, and GPC cannot, even with an older FidesJS that doesn't know the age signal, while that user's individual choices in the FidesJS modal still save. A parent's opt-in survives later saves, and the child can still turn the notice off.

```kotlin
Janus.setConsent(
    values = mapOf("data_sales_and_sharing" to true),
    consentMethod = ConsentMethod.PARENTAL_CONSENT,
    saveToFides = true
)
```

#### WebViews

Janus passes the bracket to FidesJS as the `fides_age_signal` option in every WebView it manages, including the TCF experience and WebViews created with `createConsentWebView()`. It updates the option live when the bracket changes, and it re-applies the option whenever FidesJS initializes, so the page's own `fides_overrides` cannot replace it.

The bracket reaches FidesJS through `window.fides_overrides`, so any script on the page you load can read it. Don't load third-party scripts, such as ad or analytics tags, that read `fides_overrides` on pages you show in Janus WebViews. Janus keeps the bracket out of embedded frames, but it can't hide it from the page itself.

To set the option before the page's scripts run, add `androidx.webkit` to your app. Janus treats it as an optional dependency:

```kotlin
dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
```

Without it, or on a WebView too old for document-start scripts, Janus sets the option when the page finishes loading and re-initializes FidesJS if FidesJS may have read a different value.

A page's FidesJS saves its own consent to Fides, under the page's own `fides_user_device_id`. When Janus turns back an opt-in from a WebView (for example, one saved by an older FidesJS, or before the page knew the bracket), it also saves that correction to Fides with consent method `age_signal`, under the page's device ID, so the record the page wrote is overwritten. It is sent only when `saveUserPreferencesToFides` is on, from its own retry queue, which never replaces a pending save of your app's own consent. If the page saves again and keeps a notice on that Janus allows (a 13 to 15 year old's own choice), a correction for that notice that hasn't been sent yet is dropped.

#### Your own WebViews

If you show FidesJS in a WebView Janus doesn't manage, pass it the bracket yourself: set `fides_age_signal` in `window.fides_overrides` to `Janus.webViewAgeSignal?.value` before FidesJS loads, and re-initialize FidesJS with the new value on `ageSignalUpdated`. `webViewAgeSignal` is the bracket Janus gives its own WebViews, or `null` while the age signal is off. The event carries no bracket, so forwarding Janus events to analytics doesn't share it.

```kotlin
val listenerId = Janus.addConsentEventListener { event ->
    if (event.eventType == JanusEventType.AGE_SIGNAL_UPDATED) {
        val signal = Janus.webViewAgeSignal?.value  // null: age signal off
        // Pass `signal` to your page as fides_overrides.fides_age_signal
    }
}
```

When the user presses Back while the TCF experience is saving, Janus waits up to 5 seconds for FidesJS to finish the save before closing, so its correction always reaches Fides after the save it corrects.

### TCF Experience Detection

The Janus SDK provides a property to check if the current privacy experience is a TCF (Transparency and Consent Framework) experience:

```kotlin
// Check if the current experience is a TCF experience
val isTCF = Janus.isTCFExperience

if (isTCF) {
    // Handle TCF-specific logic
    Log.d("Janus", "This is a TCF experience")
} else {
    // Handle non-TCF experience
    Log.d("Janus", "This is a standard privacy experience")
}
```
