package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderType(val displayName: String) {
    MILEAGE("By Mileage"),
    DATE("By Date")
}

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = Car::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val title: String,
    val type: ReminderType,
    val targetMileage: Double? = null,
    val targetDate: Long? = null,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val recurrenceKm: Int? = null,
    val recurrenceDays: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
