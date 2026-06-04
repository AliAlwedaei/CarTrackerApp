package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val notes: String = ""
)
