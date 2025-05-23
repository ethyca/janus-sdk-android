package com.ethyca.janussdk.android.example

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration types for the Janus SDK
 */
enum class ConfigurationType {
    ETHYCA,
    ETHYCA_EMPTY,
    COOKIE_HOUSE,
    COOKIE_HOUSE_NIGHTLY,
    CUSTOM
}

/**
 * Configuration for the Janus SDK
 */
data class JanusConfig(
    var type: ConfigurationType = ConfigurationType.ETHYCA,
    var apiHost: String = "https://privacy.ethyca.com",
    var privacyCenterHost: String = "",
    var website: String = "https://ethyca.com",
    var propertyId: String? = "FDS-KSB4MF",
    var region: String? = null,
    var autoShowExperience: Boolean = true
) {
    companion object {
        private const val PREFS_NAME = "janus_example_prefs"
        private const val KEY_API_HOST = "api_host"
        private const val KEY_PRIVACY_CENTER_HOST = "privacy_center_host"
        private const val KEY_WEBSITE = "website"
        private const val KEY_PROPERTY_ID = "property_id"
        private const val KEY_REGION = "region"
        private const val KEY_AUTO_SHOW_EXPERIENCE = "auto_show_experience"
        
        /**
         * Create a configuration for the given type
         */
        fun forType(type: ConfigurationType, context: Context): JanusConfig {
            return when (type) {
                ConfigurationType.ETHYCA -> 
                    JanusConfig()
                ConfigurationType.ETHYCA_EMPTY -> 
                    JanusConfig(
                        type = ConfigurationType.ETHYCA_EMPTY,
                        apiHost = "https://privacy.ethyca.com",
                        privacyCenterHost = "",
                        website = "https://ethyca.com",
                        propertyId = null,
                        region = null,
                        autoShowExperience = true
                    )
                ConfigurationType.COOKIE_HOUSE ->
                    JanusConfig(
                        type = ConfigurationType.COOKIE_HOUSE,
                        apiHost = "https://privacy-plus-rc.fides-staging.ethyca.com",
                        privacyCenterHost = "",
                        website = "https://cookiehouse-plus-rc.fides-staging.ethyca.com",
                        propertyId = null,
                        region = null,
                        autoShowExperience = true
                    )
                ConfigurationType.COOKIE_HOUSE_NIGHTLY ->
                    JanusConfig(
                        type = ConfigurationType.COOKIE_HOUSE_NIGHTLY,
                        apiHost = "https://privacy-plus-nightly.fides-staging.ethyca.com",
                        privacyCenterHost = "",
                        website = "https://cookiehouse-plus-nightly.fides-staging.ethyca.com",
                        propertyId = null,
                        region = null,
                        autoShowExperience = true
                    )
                ConfigurationType.CUSTOM -> 
                    loadCustomConfig(context)
            }
        }
        
        /**
         * Load a custom configuration from shared preferences
         */
        private fun loadCustomConfig(context: Context): JanusConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return JanusConfig(
                type = ConfigurationType.CUSTOM,
                apiHost = prefs.getString(KEY_API_HOST, "") ?: "",
                privacyCenterHost = prefs.getString(KEY_PRIVACY_CENTER_HOST, "") ?: "",
                website = prefs.getString(KEY_WEBSITE, "") ?: "",
                propertyId = prefs.getString(KEY_PROPERTY_ID, "")?.takeUnless { it.isEmpty() },
                region = prefs.getString(KEY_REGION, "")?.takeUnless { it.isEmpty() },
                autoShowExperience = prefs.getBoolean(KEY_AUTO_SHOW_EXPERIENCE, true)
            )
        }
    }
    
    /**
     * Save the configuration to shared preferences
     */
    fun save(context: Context) {
        if (type == ConfigurationType.CUSTOM) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(KEY_API_HOST, apiHost)
                putString(KEY_PRIVACY_CENTER_HOST, privacyCenterHost)
                putString(KEY_WEBSITE, website)
                putString(KEY_PROPERTY_ID, propertyId ?: "")
                putString(KEY_REGION, region ?: "")
                putBoolean(KEY_AUTO_SHOW_EXPERIENCE, autoShowExperience)
                apply()
            }
        }
    }
} 