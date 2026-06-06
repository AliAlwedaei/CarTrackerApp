package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FuelType(val displayName: String) {
    REGULAR("Regular"),
    PREMIUM("Premium (95)"),
    SUPER("Super (98)"),
    DIESEL("Diesel"),
    E10("E10"),
    OTHER("Other")
}

@Entity(
    tableName = "fuel_logs",
    foreignKeys = [ForeignKey(
        entity = Car::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: Long,
    val odometer: Double,
    val liters: Double,
    val costPerLiter: Double,
    val totalCost: Double,
    val fuelEfficiency: Double = 0.0,
    val isFullTank: Boolean = true,
    val fuelType: FuelType = FuelType.REGULAR,
    val notes: String = ""
)
