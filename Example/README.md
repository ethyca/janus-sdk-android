# Janus SDK Example App

This is an example app demonstrating the use of the Janus SDK for Android.

## Dependencies

The Example requires the following dependencies in the app's build.gradle, however an actual app should use Maven in ordet to automatically manage transitive dependencies

```kotlin
dependencies {
    // Janus SDK
    implementation(files("path/to/JanusSDK.aar"))
    
    // Required dependencies
    implementation("com.google.code.gson:gson:2.10.1")
}
```

## Setup

1. Ensure you have the JanusSDK.aar file in your project
2. Add the required dependencies to your build.gradle
3. Initialize the SDK with your configuration:

```kotlin
val config = JanusConfiguration.Builder()
    .apiHost("https://your-api-host.com")
    .propertyId("your-property-id")
    .region("US-CA") // Optional - if not provided, IP geolocation will be used
    .webHost("https://your-website.com")
    .build()

Janus.initialize(context, config) { success, error ->
    if (success) {
        // SDK initialized successfully
    } else {
        // Handle initialization error
        Log.e("Janus", "Failed to initialize: ${error?.message}")
    }
}
```

## Features

This example app demonstrates:

1. SDK initialization with different configuration options
2. Displaying the privacy experience UI
3. Listening for consent events
4. Reading consent values
5. Clearing consent data

## Notes

- The SDK requires a valid application context for initialization
- Initialization is asynchronous, so wait for the callback before using SDK functionality
- Check the `JanusManager` class for a complete implementation example 