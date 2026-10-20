package com.example.data.repository

import android.content.Context
import com.example.data.local.CameraDeviceDao
import com.example.data.local.CameraDeviceEntity
import com.example.data.local.CctvDatabase
import com.example.data.local.MotionEventDao
import com.example.data.local.MotionEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CctvRepository(
    private val motionEventDao: MotionEventDao,
    private val cameraDeviceDao: CameraDeviceDao
) {
    val allEvents: Flow<List<MotionEventEntity>> = motionEventDao.getAllEvents()
    val allCameras: Flow<List<CameraDeviceEntity>> = cameraDeviceDao.getAllCameras()

    suspend fun logMotionEvent(
        cameraName: String,
        eventType: String,
        confidence: Float,
        snapshotUri: String? = null,
        notes: String = ""
    ): Long {
        val event = MotionEventEntity(
            cameraName = cameraName,
            eventType = eventType,
            confidence = confidence,
            snapshotUri = snapshotUri,
            durationSeconds = (confidence * 6).toInt().coerceAtLeast(2),
            isStarred = confidence > 0.75f,
            isSyncedToCloud = true,
            notes = notes
        )
        return motionEventDao.insertEvent(event)
    }

    suspend fun toggleStar(id: Long) = motionEventDao.toggleStar(id)
    suspend fun deleteEvent(event: MotionEventEntity) = motionEventDao.deleteEvent(event)
    suspend fun clearAllEvents() = motionEventDao.clearAllEvents()

    suspend fun saveCamera(camera: CameraDeviceEntity) = cameraDeviceDao.insertOrUpdateCamera(camera)
    suspend fun removeCamera(camera: CameraDeviceEntity) = cameraDeviceDao.deleteCamera(camera)

    suspend fun deleteCameraById(id: String) {
        val cam = cameraDeviceDao.getCameraById(id)
        if (cam != null) {
            cameraDeviceDao.deleteCamera(cam)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        val currentCameras = cameraDeviceDao.getAllCameras().first()
        
        // Remove old dummy multi-room cameras if present so user has clean single node grid
        val dummyIds = listOf("cam-front-door", "cam-backyard", "cam-garage")
        currentCameras.filter { it.id in dummyIds }.forEach {
            cameraDeviceDao.deleteCamera(it)
        }

        val updatedCameras = cameraDeviceDao.getAllCameras().first()
        if (updatedCameras.isEmpty()) {
            val primaryCamera = CameraDeviceEntity(
                id = "cam-node-local",
                name = "OmniCam Security Node (Local)",
                streamUrl = "http://localhost:8080/live",
                location = "Primary Station",
                isOnline = true,
                batteryPercent = 98,
                resolution = "1080p FHD"
            )
            cameraDeviceDao.insertOrUpdateCamera(primaryCamera)

            // Add sample motion detections so user can immediately inspect the alerts feed
            val sampleEvents = listOf(
                MotionEventEntity(
                    cameraName = "Front Door Camera",
                    eventType = "Person Detected",
                    confidence = 0.94f,
                    snapshotUri = null,
                    durationSeconds = 6,
                    isStarred = true,
                    isSyncedToCloud = true,
                    notes = "Delivery driver approached porch area"
                ),
                MotionEventEntity(
                    cameraName = "Backyard & Patio",
                    eventType = "Motion Detected",
                    confidence = 0.72f,
                    snapshotUri = null,
                    durationSeconds = 4,
                    isStarred = false,
                    isSyncedToCloud = true,
                    notes = "Rapid movement near fence border"
                ),
                MotionEventEntity(
                    cameraName = "This Phone (Local Node)",
                    eventType = "System Armed",
                    confidence = 1.0f,
                    snapshotUri = null,
                    durationSeconds = 1,
                    isStarred = false,
                    isSyncedToCloud = true,
                    notes = "Surveillance active with zone masking"
                )
            )
            sampleEvents.forEach { motionEventDao.insertEvent(it) }
        }
    }

    companion object {
        fun create(context: Context): CctvRepository {
            val db = CctvDatabase.getDatabase(context)
            val repo = CctvRepository(db.motionEventDao(), db.cameraDeviceDao())
            CoroutineScope(Dispatchers.IO).launch {
                repo.seedInitialDataIfEmpty()
            }
            return repo
        }
    }
}
