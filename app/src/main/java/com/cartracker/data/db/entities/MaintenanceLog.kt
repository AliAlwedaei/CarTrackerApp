package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MaintenanceCategory(val displayName: String) {
    OIL_CHANGE("Oil Change"),
    TIRES("Tires"),
    BRAKES("Brakes"),
    BATTERY("Battery"),
    FILTERS("Filters"),
    WIPERS("Wipers"),
    INSURANCE("Insurance"),
    REGISTRATION("Registration"),
    TRANSMISSION("Transmission"),
    AC_SERVICE("AC Service"),
    SPARK_PLUGS("Spark Plugs"),
    ALIGNMENT("Alignment"),
    TIMING_BELT("Timing Belt"),
    OTHER("Other")
}

@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [ForeignKey(
        entity = Car::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class MaintenanceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val category: MaintenanceCategory,
    val serviceType: String,
    val date: Long,
    val mileage: Double,
    val cost: Double,
    val garage: String = "",
    val nextServiceKm: Double? = null,
    val warrantyExpiryDate: Long? = null,
    val warrantyNotes: String = "",
    val notes: String = ""
)
