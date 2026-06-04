package com.cartracker.util

import android.content.Context
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object SeedDataUtil {

    suspend fun seedIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("cartracker_dev", Context.MODE_PRIVATE)
        seedV2(context, prefs)
        seedV3(context, prefs)
    }

    private suspend fun seedV3(context: Context, prefs: android.content.SharedPreferences) {
        if (prefs.getBoolean("seed_done_v3", false)) return
        val repo = (context.applicationContext as CarTrackerApp).repository

        fun daysAgo(days: Int, hour: Int = 9): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun daysAhead(days: Int): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // ── Second Car: Honda Civic 2019 (city commuter, smaller tank, worse efficiency) ──
        val civicId = repo.insertCar(
            Car(name = "Civic", make = "Honda", model = "Civic", year = 2019,
                plateNumber = "XKP-2239", currentOdometer = 82310.0)
        )

        // Fuel logs — 40L tank, ~310 km range, ~7.8 km/L city
        repo.insertFuelLog(FuelLog(carId = civicId, date = daysAgo(20), odometer = 81700.0, liters = 38.5, costPerLiter = 1.91, totalCost = 73.54, fuelEfficiency = 0.0))
        repo.insertFuelLog(FuelLog(carId = civicId, date = daysAgo(14), odometer = 82000.0, liters = 39.1, costPerLiter = 1.88, totalCost = 73.51, fuelEfficiency = 7.67))
        repo.insertFuelLog(FuelLog(carId = civicId, date = daysAgo(7),  odometer = 82310.0, liters = 38.8, costPerLiter = 1.93, totalCost = 74.88, fuelEfficiency = 7.99))

        // Trips — shorter, more urban
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(20, 7),  startOdometer = 81700.0, endOdometer = 81712.0, distance = 12.0, purpose = TripPurpose.WORK, notes = "School drop-off"))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(20, 17), startOdometer = 81712.0, endOdometer = 81730.0, distance = 18.0, purpose = TripPurpose.PERSONAL, notes = "Grocery run"))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(18, 8),  startOdometer = 81730.0, endOdometer = 81758.0, distance = 28.0, purpose = TripPurpose.WORK, notes = ""))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(15, 8),  startOdometer = 81758.0, endOdometer = 81820.0, distance = 62.0, purpose = TripPurpose.PERSONAL, notes = "Errands + mall"))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(13, 7),  startOdometer = 81820.0, endOdometer = 81838.0, distance = 18.0, purpose = TripPurpose.WORK, notes = ""))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(12, 17), startOdometer = 81838.0, endOdometer = 81856.0, distance = 18.0, purpose = TripPurpose.WORK, notes = ""))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(10, 9),  startOdometer = 81856.0, endOdometer = 81940.0, distance = 84.0, purpose = TripPurpose.PERSONAL, notes = "Family visit"))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(7, 7),   startOdometer = 81940.0, endOdometer = 81958.0, distance = 18.0, purpose = TripPurpose.WORK, notes = ""))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(6, 8),   startOdometer = 81958.0, endOdometer = 81976.0, distance = 18.0, purpose = TripPurpose.WORK, notes = ""))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(5, 10),  startOdometer = 81976.0, endOdometer = 82058.0, distance = 82.0, purpose = TripPurpose.PERSONAL, notes = "Weekend outing"))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(3, 7),   startOdometer = 82058.0, endOdometer = 82076.0, distance = 18.0, purpose = TripPurpose.WORK, notes = ""))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(2, 7),   startOdometer = 82076.0, endOdometer = 82200.0, distance = 124.0, purpose = TripPurpose.PERSONAL, notes = "Day trip"))
        repo.insertTrip(Trip(carId = civicId, date = daysAgo(1, 7),   startOdometer = 82200.0, endOdometer = 82310.0, distance = 110.0, purpose = TripPurpose.WORK, notes = "Conference"))

        // Maintenance — brakes done recently
        repo.insertMaintenanceLog(MaintenanceLog(
            carId = civicId, category = MaintenanceCategory.BRAKES,
            serviceType = "Front Brake Pad Replacement", date = daysAgo(5),
            mileage = 81976.0, cost = 210.0, notes = "OEM pads, both sides"
        ))

        // Reminders
        repo.insertReminder(Reminder(carId = civicId, title = "Air Filter Replacement", type = ReminderType.MILEAGE, targetMileage = 85000.0, notes = "Every 15 000 km"))
        repo.insertReminder(Reminder(carId = civicId, title = "Insurance Renewal", type = ReminderType.DATE, targetDate = daysAhead(23), notes = "Policy #CV-8821-2024"))

        prefs.edit().putBoolean("seed_done_v3", true).apply()
    }

    private suspend fun seedV2(context: Context, prefs: android.content.SharedPreferences) {
        if (prefs.getBoolean("seed_done_v2", false)) return

        val repo = (context.applicationContext as CarTrackerApp).repository

        fun daysAgo(days: Int, hour: Int = 9): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun daysAhead(days: Int): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // ── Car ───────────────────────────────────────────────────────────
        val carId = repo.insertCar(
            Car(
                name = "My Camry",
                make = "Toyota",
                model = "Camry",
                year = 2022,
                plateNumber = "MKR-8421",
                currentOdometer = 46920.0
            )
        )

        // ── Fuel Logs (6 fillups, every ~3-4 days) ────────────────────────
        // eff = (nextOdo - thisOdo) / liters of NEXT fillup → computed as (odo - prevOdo) / liters
        // Fillup 1: no previous, eff = 0
        // Fillup 2: (45398 - 45000) / 44.1  = 9.02 km/L
        // Fillup 3: (45783 - 45398) / 43.8  = 8.79 km/L
        // Fillup 4: (46168 - 45783) / 44.5  = 8.65 km/L
        // Fillup 5: (46541 - 46168) / 43.2  = 8.63 km/L
        // Fillup 6: (46920 - 46541) / 45.0  = 8.42 km/L
        repo.insertFuelLog(FuelLog(carId = carId, date = daysAgo(21), odometer = 45000.0, liters = 46.3, costPerLiter = 1.89, totalCost = 87.51, fuelEfficiency = 0.0, notes = ""))
        repo.insertFuelLog(FuelLog(carId = carId, date = daysAgo(17), odometer = 45398.0, liters = 44.1, costPerLiter = 1.92, totalCost = 84.67, fuelEfficiency = 9.02, notes = ""))
        repo.insertFuelLog(FuelLog(carId = carId, date = daysAgo(12), odometer = 45783.0, liters = 43.8, costPerLiter = 1.87, totalCost = 81.91, fuelEfficiency = 8.79, notes = ""))
        repo.insertFuelLog(FuelLog(carId = carId, date = daysAgo(7),  odometer = 46168.0, liters = 44.5, costPerLiter = 1.91, totalCost = 85.00, fuelEfficiency = 8.65, notes = "Shell station"))
        repo.insertFuelLog(FuelLog(carId = carId, date = daysAgo(3),  odometer = 46541.0, liters = 43.2, costPerLiter = 1.94, totalCost = 83.81, fuelEfficiency = 8.63, notes = ""))
        repo.insertFuelLog(FuelLog(carId = carId, date = daysAgo(0),  odometer = 46920.0, liters = 45.0, costPerLiter = 1.88, totalCost = 84.60, fuelEfficiency = 8.42, notes = "Full tank"))

        // ── Trips (24 trips across 3 weeks) ───────────────────────────────
        // Week 1: May 14–20 (days 21–15 ago)
        repo.insertTrip(Trip(carId = carId, date = daysAgo(21, 7),  startOdometer = 45000.0, endOdometer = 45022.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = "Morning commute"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(21, 17), startOdometer = 45022.0, endOdometer = 45044.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(20, 7),  startOdometer = 45044.0, endOdometer = 45066.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(20, 17), startOdometer = 45066.0, endOdometer = 45088.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(19, 7),  startOdometer = 45088.0, endOdometer = 45165.0, distance = 77.0,  purpose = TripPurpose.WORK,     notes = "Client visit"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(18, 10), startOdometer = 45165.0, endOdometer = 45260.0, distance = 95.0,  purpose = TripPurpose.PERSONAL, notes = "Weekend drive"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(18, 15), startOdometer = 45260.0, endOdometer = 45398.0, distance = 138.0, purpose = TripPurpose.PERSONAL, notes = ""))

        // Week 2: May 21–27 (days 14–8 ago)
        repo.insertTrip(Trip(carId = carId, date = daysAgo(14, 7),  startOdometer = 45398.0, endOdometer = 45420.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(14, 17), startOdometer = 45420.0, endOdometer = 45442.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(13, 7),  startOdometer = 45442.0, endOdometer = 45486.0, distance = 44.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(12, 7),  startOdometer = 45486.0, endOdometer = 45600.0, distance = 114.0, purpose = TripPurpose.WORK,     notes = "Airport drop-off"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(11, 9),  startOdometer = 45600.0, endOdometer = 45783.0, distance = 183.0, purpose = TripPurpose.PERSONAL, notes = "Day trip to the coast"))

        // Week 3: May 28–Jun 4 (days 7–0 ago)
        repo.insertTrip(Trip(carId = carId, date = daysAgo(7, 7),   startOdometer = 45783.0, endOdometer = 45805.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(7, 17),  startOdometer = 45805.0, endOdometer = 45827.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(6, 7),   startOdometer = 45827.0, endOdometer = 45960.0, distance = 133.0, purpose = TripPurpose.WORK,     notes = "Training across town"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(5, 7),   startOdometer = 45960.0, endOdometer = 45982.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(4, 10),  startOdometer = 45982.0, endOdometer = 46168.0, distance = 186.0, purpose = TripPurpose.PERSONAL, notes = "Beach trip"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(3, 7),   startOdometer = 46168.0, endOdometer = 46190.0, distance = 22.0,  purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(3, 17),  startOdometer = 46190.0, endOdometer = 46360.0, distance = 170.0, purpose = TripPurpose.WORK,     notes = "Off-site meeting"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(2, 7),   startOdometer = 46360.0, endOdometer = 46541.0, distance = 181.0, purpose = TripPurpose.PERSONAL, notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(1, 7),   startOdometer = 46541.0, endOdometer = 46720.0, distance = 179.0, purpose = TripPurpose.WORK,     notes = ""))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(1, 17),  startOdometer = 46720.0, endOdometer = 46780.0, distance = 60.0,  purpose = TripPurpose.PERSONAL, notes = "Grocery run"))
        repo.insertTrip(Trip(carId = carId, date = daysAgo(0, 8),   startOdometer = 46780.0, endOdometer = 46920.0, distance = 140.0, purpose = TripPurpose.PERSONAL, notes = "Fill-up + errands"))

        // ── Maintenance (2 services mid-week-2) ────────────────────────────
        repo.insertMaintenanceLog(MaintenanceLog(
            carId = carId, category = MaintenanceCategory.OIL_CHANGE,
            serviceType = "Full Synthetic Oil Change", date = daysAgo(11),
            mileage = 45600.0, cost = 89.0, notes = "Mobil 1 5W-30, filter replaced"
        ))
        repo.insertMaintenanceLog(MaintenanceLog(
            carId = carId, category = MaintenanceCategory.TIRES,
            serviceType = "Tire Rotation", date = daysAgo(11),
            mileage = 45600.0, cost = 35.0, notes = "Rotated all 4, PSI set to 34"
        ))

        // ── Reminders (2 upcoming, 1 completed) ───────────────────────────
        repo.insertReminder(Reminder(
            carId = carId, title = "Next Oil Change", type = ReminderType.MILEAGE,
            targetMileage = 50600.0, notes = "Full synthetic every 5 000 km"
        ))
        repo.insertReminder(Reminder(
            carId = carId, title = "Replace Wiper Blades", type = ReminderType.DATE,
            targetDate = daysAhead(18), notes = "Front wipers streaking in rain"
        ))
        repo.insertReminder(Reminder(
            carId = carId, title = "Annual Registration Renewal", type = ReminderType.DATE,
            targetDate = daysAhead(47), notes = "Bring insurance + ownership docs"
        ))
        repo.insertReminder(Reminder(
            carId = carId, title = "Check Tire Pressure", type = ReminderType.DATE,
            targetDate = daysAgo(2), notes = "Before the long drive", isCompleted = true
        ))

        prefs.edit().putBoolean("seed_done_v2", true).apply()
    }
}
