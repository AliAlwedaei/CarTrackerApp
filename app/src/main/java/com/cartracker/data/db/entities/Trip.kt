package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TripPurpose(val displayName: String) {
    PERSONAL("Personal"),
    WORK("Work")
}

@Entity(
    tableName = "trips",
    foreignKeys = [ForeignKey(
        entity = Car::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: Long,
    val startOdometer: Double,
    val endOdometer: Double,
    val distance: Double,
    val purpose: TripPurpose,
    val notes: String = ""
)
