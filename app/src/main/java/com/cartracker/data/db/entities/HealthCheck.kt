package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class HealthCheckType(
    val displayName: String,
    val description: String,
    val defaultIntervalDays: Int,
    val maintenanceCategory: MaintenanceCategory? = null,
    val serviceLabel: String = "Log Service"
) {
    ENGINE_OIL(
        "Engine Oil",
        "Check dipstick — top up if below MIN mark",
        90,
        MaintenanceCategory.OIL_CHANGE,
        "Log Oil Change"
    ),
    TYRE_PRESSURE(
        "Tyre Pressure",
        "Check all 4 tyres and the spare with a gauge",
        30
    ),
    COOLANT(
        "Coolant Level",
        "Check reservoir is between MIN and MAX lines",
        30
    ),
    WASHER_FLUID(
        "Washer Fluid",
        "Top up windshield washer reservoir",
        30
    ),
    LIGHTS(
        "Lights Check",
        "Test headlights, brake lights, and indicators",
        30
    ),
    BRAKE_FLUID(
        "Brake Fluid",
        "Check master cylinder reservoir is above MIN",
        180,
        MaintenanceCategory.BRAKES,
        "Log Brake Service"
    ),
    BATTERY(
        "Battery",
        "Inspect terminals for corrosion and secure connections",
        365,
        MaintenanceCategory.BATTERY,
        "Log Battery Service"
    ),
    AIR_FILTER(
        "Air Filter",
        "Visual inspection — replace if heavily soiled",
        365,
        MaintenanceCategory.FILTERS,
        "Log Filter Replacement"
    ),
    WIPER_BLADES(
        "Wiper Blades",
        "Check for streaking, cracking, or poor contact",
        180,
        MaintenanceCategory.WIPERS,
        "Log Wiper Replacement"
    )
}

@Entity(
    tableName = "health_checks",
    foreignKeys = [ForeignKey(
        entity = Car::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId"), Index(value = ["carId", "checkType"], unique = true)]
)
data class HealthCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val checkType: HealthCheckType,
    val lastCheckedAt: Long? = null,
    val intervalDays: Int = checkType.defaultIntervalDays,
    val intervalKm: Int? = null,
    val lastCheckedAtOdometer: Double? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
