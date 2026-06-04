package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE carId = :carId ORDER BY date DESC")
    fun getTripsForCar(carId: Long): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE carId = :carId ORDER BY date DESC LIMIT 1")
    suspend fun getLastTripForCar(carId: Long): Trip?

    @Query("SELECT SUM(distance) FROM trips WHERE carId = :carId")
    suspend fun getTotalMileage(carId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)
}
