package com.ethyca.janussdk.android.example

import android.util.Log
import com.ethyca.janussdk.android.JanusLogger
import com.ethyca.janussdk.android.LogLevel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTPLogger implementation that sends logs to a remote HTTP endpoint
 * This is a sample implementation for demonstration purposes
 */
class HTTPLogger(
    private val endpoint: String,
    private val authToken: String,
    private val source: String = "AndroidExampleApp",
    private val enableConsoleErrors: Boolean = false
) : JanusLogger {
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    override fun log(message: String, level: LogLevel, metadata: Map<String, String>?, error: Throwable?) {
        val logData = createLogPayload(message, level, metadata, error)
        sendLogRequest(logData)
    }
    
    private fun createLogPayload(
        message: String, 
        level: LogLevel, 
        metadata: Map<String, String>?, 
        error: Throwable?
    ): Map<String, Any> {
        val logEntry = mutableMapOf<String, Any>(
            "log_level" to levelString(level),
            "message" to message
        )
        
        if (metadata != null) {
            val encodedData = encodeMetadata(metadata)
            logEntry["data"] = encodedData
        }
        
        if (error != null) {
            logEntry["error"] = mapOf(
                "description" to (error.message ?: error.javaClass.simpleName),
                "type" to error.javaClass.simpleName,
                "stackTrace" to error.stackTraceToString()
            )
        }
        
        return mapOf(
            "logs" to listOf(
                mapOf("log" to logEntry)
            ),
            "source" to source
        )
    }
    
    private fun levelString(level: LogLevel): String {
        return when (level) {
            LogLevel.VERBOSE -> "VERBOSE"
            LogLevel.DEBUG -> "DEBUG"
            LogLevel.INFO -> "INFO"
            LogLevel.WARNING -> "WARNING"
            LogLevel.ERROR -> "ERROR"
        }
    }
    
    private fun encodeMetadata(metadata: Map<String, String>): String {
        return try {
            // Try JSON serialization first
            gson.toJson(metadata)
        } catch (e: Exception) {
            // Fallback to string representation
            metadata.toString()
        }
    }
    
    private fun sendLogRequest(logData: Map<String, Any>) {
        try {
            val jsonString = gson.toJson(logData)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (enableConsoleErrors) {
                        Log.e("HTTPLogger", "Network error: ${e.message}")
                    }
                }
                
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            if (enableConsoleErrors) {
                                Log.e("HTTPLogger", "HTTP error - Status code: ${response.code}")
                                response.body?.string()?.let { responseBody ->
                                    Log.e("HTTPLogger", "Response body: $responseBody")
                                }
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            if (enableConsoleErrors) {
                Log.e("HTTPLogger", "Failed to serialize log data: ${e.message}")
                Log.e("HTTPLogger", "Log data that failed: $logData")
            }
        }
    }
} 