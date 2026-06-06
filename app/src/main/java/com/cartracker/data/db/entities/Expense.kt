package com.cartracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory(val displayName: String) {
    INSURANCE("Insurance"),
    REGISTRATION("Registration / Road Tax"),
    PARKING("Parking"),
    TOLL("Toll / Fines"),
    CAR_WASH("Car Wash"),
    ACCESSORIES("Accessories"),
    LOAN_PAYMENT("Loan Payment"),
    OTHER("Other")
}

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = Car::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val category: ExpenseCategory,
    val description: String,
    val date: Long,
    val amount: Double,
    val isRecurring: Boolean = false,
    val recurrenceDays: Int? = null,
    val notes: String = ""
)
