package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String,
    val model: String,
    val year: Int,
    val plateNumber: String,
    val currentOdometer: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
