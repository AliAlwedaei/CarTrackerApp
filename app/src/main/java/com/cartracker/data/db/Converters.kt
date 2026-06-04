package com.cartracker.data.db

import androidx.room.TypeConverter
import com.cartracker.data.db.entities.HealthCheckType
import com.cartracker.data.db.entities.MaintenanceCategory
import com.cartracker.data.db.entities.ReminderType
import com.cartracker.data.db.entities.TripPurpose

class Converters {
    @TypeConverter
    fun fromMaintenanceCategory(value: MaintenanceCategory): String = value.name

    @TypeConverter
    fun toMaintenanceCategory(value: String): MaintenanceCategory =
        MaintenanceCategory.valueOf(value)

    @TypeConverter
    fun fromTripPurpose(value: TripPurpose): String = value.name

    @TypeConverter
    fun toTripPurpose(value: String): TripPurpose = TripPurpose.valueOf(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromHealthCheckType(value: HealthCheckType): String = value.name

    @TypeConverter
    fun toHealthCheckType(value: String): HealthCheckType = HealthCheckType.valueOf(value)
}
