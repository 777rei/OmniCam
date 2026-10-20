package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motion_events")
data class MotionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cameraName: String,
    val eventType: String, // "Motion Detected", "Intruder Alert", "Audio Spike", "Tamper Alert"
    val confidence: Float, // 0.0 to 1.0
    val snapshotUri: String? = null,
    val durationSeconds: Int = 3,
    val isStarred: Boolean = false,
    val isSyncedToCloud: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "camera_devices")
data class CameraDeviceEntity(
    @PrimaryKey
    val id: String, // e.g. "cam-local", "cam-front-door"
    val name: String,
    val streamUrl: String,
    val location: String,
    val isOnline: Boolean = true,
    val batteryPercent: Int = 94,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val resolution: String = "720p HD",
    val encryptionKey: String = "AES-256-DEFAULT"
)
