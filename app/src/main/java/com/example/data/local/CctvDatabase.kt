package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MotionEventEntity::class, CameraDeviceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CctvDatabase : RoomDatabase() {
    abstract fun motionEventDao(): MotionEventDao
    abstract fun cameraDeviceDao(): CameraDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: CctvDatabase? = null

        fun getDatabase(context: Context): CctvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CctvDatabase::class.java,
                    "cctv_omnicam_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
