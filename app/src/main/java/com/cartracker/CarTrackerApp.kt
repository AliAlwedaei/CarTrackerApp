package com.cartracker

import android.app.Application
import com.cartracker.data.db.CarTrackerDatabase
import com.cartracker.data.repository.CarTrackerRepository
import com.cartracker.util.SeedDataUtil
import com.cartracker.worker.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CarTrackerApp : Application() {
    val database by lazy { CarTrackerDatabase.getDatabase(this) }
    val repository by lazy {
        CarTrackerRepository(
            database.carDao(),
            database.fuelLogDao(),
            database.maintenanceLogDao(),
            database.tripDao(),
            database.reminderDao(),
            database.healthCheckDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        ReminderWorker.scheduleDailyCheck(this)
        CoroutineScope(Dispatchers.IO).launch { SeedDataUtil.seedIfEmpty(this@CarTrackerApp) }
    }
}
