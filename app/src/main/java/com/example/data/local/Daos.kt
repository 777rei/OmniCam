package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotionEventDao {
    @Query("SELECT * FROM motion_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<MotionEventEntity>>

    @Query("SELECT * FROM motion_events WHERE cameraName = :cameraName ORDER BY timestamp DESC")
    fun getEventsByCamera(cameraName: String): Flow<List<MotionEventEntity>>

    @Query("SELECT * FROM motion_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): MotionEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MotionEventEntity): Long

    @Update
    suspend fun updateEvent(event: MotionEventEntity)

    @Delete
    suspend fun deleteEvent(event: MotionEventEntity)

    @Query("DELETE FROM motion_events")
    suspend fun clearAllEvents()

    @Query("UPDATE motion_events SET isStarred = NOT isStarred WHERE id = :id")
    suspend fun toggleStar(id: Long)
}

@Dao
interface CameraDeviceDao {
    @Query("SELECT * FROM camera_devices ORDER BY name ASC")
    fun getAllCameras(): Flow<List<CameraDeviceEntity>>

    @Query("SELECT * FROM camera_devices WHERE id = :id LIMIT 1")
    suspend fun getCameraById(id: String): CameraDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCamera(camera: CameraDeviceEntity)

    @Delete
    suspend fun deleteCamera(camera: CameraDeviceEntity)
}
