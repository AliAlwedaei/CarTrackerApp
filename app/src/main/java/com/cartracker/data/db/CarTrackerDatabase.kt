package com.cartracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cartracker.data.db.dao.*
import com.cartracker.data.db.entities.*

@Database(
    entities = [Car::class, FuelLog::class, MaintenanceLog::class, Trip::class, Reminder::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CarTrackerDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun tripDao(): TripDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: CarTrackerDatabase? = null

        fun getDatabase(context: Context): CarTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CarTrackerDatabase::class.java,
                    "car_tracker_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
