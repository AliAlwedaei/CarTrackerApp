package com.cartracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cartracker.data.db.dao.*
import com.cartracker.data.db.entities.*

@Database(
    entities = [Car::class, FuelLog::class, MaintenanceLog::class, Trip::class, Reminder::class, HealthCheck::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CarTrackerDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun tripDao(): TripDao
    abstract fun reminderDao(): ReminderDao
    abstract fun healthCheckDao(): HealthCheckDao

    companion object {
        @Volatile private var INSTANCE: CarTrackerDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `health_checks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `carId` INTEGER NOT NULL,
                        `checkType` TEXT NOT NULL,
                        `lastCheckedAt` INTEGER,
                        `intervalDays` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_checks_carId` ON `health_checks` (`carId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_health_checks_carId_checkType` ON `health_checks` (`carId`, `checkType`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceKm` INTEGER")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceDays` INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `health_checks` ADD COLUMN `intervalKm` INTEGER")
                db.execSQL("ALTER TABLE `health_checks` ADD COLUMN `lastCheckedAtOdometer` REAL")
                db.execSQL("ALTER TABLE `health_checks` ADD COLUMN `notes` TEXT")
            }
        }

        fun getDatabase(context: Context): CarTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CarTrackerDatabase::class.java,
                    "car_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
