package com.ethyca.janussdk.android.example

import android.content.Context
import android.content.SharedPreferences
import com.ethyca.janussdk.android.models.ConsentFlagType
import com.ethyca.janussdk.android.models.ConsentNonApplicableFlagMode

/**
 * Configuration types for the Janus SDK
 */
enum class ConfigurationType {
    ETHYCA,
    ETHYCA_EMPTY,
    LOCAL_SLIM,
    LOCAL_DEMO,
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
    var autoShowExperience: Boolean = true,
    var consentFlagType: ConsentFlagType = ConsentFlagType.BOOLEAN,
    var consentNonApplicableFlagMode: ConsentNonApplicableFlagMode = ConsentNonApplicableFlagMode.OMIT
) {
    companion object {
        private const val PREFS_NAME = "janus_example_prefs"
        private const val KEY_API_HOST = "api_host"
        private const val KEY_PRIVACY_CENTER_HOST = "privacy_center_host"
        private const val KEY_WEBSITE = "website"
        private const val KEY_PROPERTY_ID = "property_id"
        private const val KEY_REGION = "region"
        private const val KEY_AUTO_SHOW_EXPERIENCE = "auto_show_experience"
        private const val KEY_CONSENT_FLAG_TYPE = "consent_flag_type"
        private const val KEY_CONSENT_NON_APPLICABLE_FLAG_MODE = "consent_non_applicable_flag_mode"
        
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
                        autoShowExperience = true,
                        consentFlagType = ConsentFlagType.BOOLEAN,
                        consentNonApplicableFlagMode = ConsentNonApplicableFlagMode.OMIT
                    )
                ConfigurationType.LOCAL_SLIM ->
                    JanusConfig(
                        type = ConfigurationType.LOCAL_SLIM,
                        apiHost = "http://10.0.2.2:3000",
                        privacyCenterHost = "http://10.0.2.2:3001",
                        website = "http://10.0.2.2:3001",
                        propertyId = null,
                        region = null,
                        autoShowExperience = true,
                        consentFlagType = ConsentFlagType.BOOLEAN,
                        consentNonApplicableFlagMode = ConsentNonApplicableFlagMode.OMIT
                    )
                ConfigurationType.LOCAL_DEMO ->
                    JanusConfig(
                        type = ConfigurationType.LOCAL_DEMO,
                        apiHost = "http://10.0.2.2:8080",
                        privacyCenterHost = "http://10.0.2.2:3001",
                        website = "http://10.0.2.2:3000",
                        propertyId = "FDS-DY5EAX",
                        region = null,
                        autoShowExperience = true,
                        consentFlagType = ConsentFlagType.BOOLEAN,
                        consentNonApplicableFlagMode = ConsentNonApplicableFlagMode.OMIT
                    )
                ConfigurationType.COOKIE_HOUSE ->
                    JanusConfig(
                        type = ConfigurationType.COOKIE_HOUSE,
                        apiHost = "https://privacy-plus-rc.fides-staging.ethyca.com",
                        privacyCenterHost = "",
                        website = "https://cookiehouse-plus-rc.fides-staging.ethyca.com",
                        propertyId = null,
                        region = null,
                        autoShowExperience = true,
                        consentFlagType = ConsentFlagType.BOOLEAN,
                        consentNonApplicableFlagMode = ConsentNonApplicableFlagMode.OMIT
                    )
                ConfigurationType.COOKIE_HOUSE_NIGHTLY ->
                    JanusConfig(
                        type = ConfigurationType.COOKIE_HOUSE_NIGHTLY,
                        apiHost = "https://privacy-plus-nightly.fides-staging.ethyca.com",
                        privacyCenterHost = "",
                        website = "https://cookiehouse-plus-nightly.fides-staging.ethyca.com",
                        propertyId = null,
                        region = null,
                        autoShowExperience = true,
                        consentFlagType = ConsentFlagType.BOOLEAN,
                        consentNonApplicableFlagMode = ConsentNonApplicableFlagMode.OMIT
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
            val consentFlagTypeString = prefs.getString(KEY_CONSENT_FLAG_TYPE, "BOOLEAN") ?: "BOOLEAN"
            val consentFlagType = ConsentFlagType.fromString(consentFlagTypeString)
            val consentNonApplicableFlagModeString = prefs.getString(KEY_CONSENT_NON_APPLICABLE_FLAG_MODE, "OMIT") ?: "OMIT"
            val consentNonApplicableFlagMode = ConsentNonApplicableFlagMode.fromString(consentNonApplicableFlagModeString)
            return JanusConfig(
                type = ConfigurationType.CUSTOM,
                apiHost = prefs.getString(KEY_API_HOST, "") ?: "",
                privacyCenterHost = prefs.getString(KEY_PRIVACY_CENTER_HOST, "") ?: "",
                website = prefs.getString(KEY_WEBSITE, "") ?: "",
                propertyId = prefs.getString(KEY_PROPERTY_ID, "")?.takeUnless { it.isEmpty() },
                region = prefs.getString(KEY_REGION, "")?.takeUnless { it.isEmpty() },
                autoShowExperience = prefs.getBoolean(KEY_AUTO_SHOW_EXPERIENCE, true),
                consentFlagType = consentFlagType,
                consentNonApplicableFlagMode = consentNonApplicableFlagMode
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
                putString(KEY_CONSENT_FLAG_TYPE, consentFlagType.toString())
                putString(KEY_CONSENT_NON_APPLICABLE_FLAG_MODE, consentNonApplicableFlagMode.toString())
                apply()
            }
        }
    }
} 
