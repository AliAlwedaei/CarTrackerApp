package com.cartracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.HealthCheckType
import com.cartracker.data.db.entities.ReminderType
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as CarTrackerApp).repository
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // ── Reminders (date + mileage) ───────────────────────────────────────
        repository.allActiveReminders.first().forEach { reminder ->
            val shouldNotify = when (reminder.type) {
                ReminderType.DATE -> reminder.targetDate != null &&
                    reminder.targetDate <= now + TimeUnit.DAYS.toMillis(3)
                ReminderType.MILEAGE -> reminder.targetMileage != null && run {
                    val odo = repository.getCarById(reminder.carId)?.currentOdometer ?: 0.0
                    odo >= reminder.targetMileage - 500  // within 500 km of target
                }
            }
            if (shouldNotify) {
                val text = when (reminder.type) {
                    ReminderType.DATE -> reminder.targetDate?.let { "Due ${sdf.format(Date(it))}" } ?: reminder.title
                    ReminderType.MILEAGE -> reminder.targetMileage?.let { String.format(Locale.US, "At %.0f km", it) } ?: reminder.title
                }
                notify(notificationManager, id = reminder.id.toInt(),
                    channelId = CHANNEL_REMINDERS, title = reminder.title, text = text)
            }
        }

        // ── Health check overdue notifications ───────────────────────────────
        val cars = repository.getAllCarsOnce()
        val dayMs = 1000L * 60 * 60 * 24

        cars.forEach { car ->
            val checks = repository.getHealthChecksForCar(car.id).first()
            val maintenanceLogs = repository.getMaintenanceLogsForCar(car.id).first()
            val latestByCategory = maintenanceLogs
                .groupBy { it.category }
                .mapValues { (_, logs) -> logs.maxByOrNull { it.date } }

            checks.forEach { check ->
                val backingLog = check.checkType.maintenanceCategory?.let { latestByCategory[it] }
                val lastAt = backingLog?.date ?: check.lastCheckedAt
                val lastOdo = backingLog?.mileage ?: check.lastCheckedAtOdometer

                val daysSince = lastAt?.let { (now - it) / dayMs }
                val daysUntilDue = if (daysSince != null) check.intervalDays - daysSince
                                   else -check.intervalDays.toLong()

                val kmSince = if (check.intervalKm != null && lastOdo != null)
                    car.currentOdometer - lastOdo else null
                val kmUntilDue = if (check.intervalKm != null && kmSince != null)
                    check.intervalKm - kmSince else null

                val isOverdue = (lastAt == null) || (daysUntilDue < 0) || (kmUntilDue != null && kmUntilDue < 0)
                if (isOverdue) {
                    val reason = when {
                        lastAt == null -> "never done"
                        kmUntilDue != null && kmUntilDue < 0 -> "${(-kmUntilDue).toInt()} km overdue"
                        else -> "${(-daysUntilDue)}d overdue"
                    }
                    notify(
                        notificationManager,
                        id = (car.id * 100 + check.checkType.ordinal).toInt(),
                        channelId = CHANNEL_HEALTH,
                        title = "${car.name} — ${check.checkType.displayName}",
                        text = "${check.checkType.displayName} is $reason"
                    )
                }
            }
        }

        return Result.success()
    }

    private fun notify(manager: NotificationManager, id: Int, channelId: String, title: String, text: String) {
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    private fun createChannels(manager: NotificationManager) {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Maintenance Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Upcoming maintenance and date reminders"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_HEALTH, "Health Check Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Overdue vehicle health checks"
            }
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "car_tracker_reminders"
        const val CHANNEL_HEALTH    = "car_tracker_health"

        fun scheduleDailyCheck(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "reminder_check", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
