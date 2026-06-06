package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_health_checks",
    foreignKeys = [ForeignKey(
        entity = Car::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class CustomHealthCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val name: String,
    val description: String = "",
    val intervalDays: Int = 30,
    val intervalKm: Int? = null,
    val lastCheckedAt: Long? = null,
    val lastCheckedAtOdometer: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
