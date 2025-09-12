package com.ethyca.janussdk.android.example

data class EventItem(
    val eventType: String,
    val eventDetail: String,
    val timestamp: Long = System.currentTimeMillis()
)
