package com.cartracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.cartracker.CarTrackerApp
import com.cartracker.MainActivity
import com.cartracker.data.db.entities.HealthCheckType
import com.cartracker.data.db.entities.ReminderType
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
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
                    odo >= reminder.targetMileage - 500
                }
            }
            if (shouldNotify) {
                val text = when (reminder.type) {
                    ReminderType.DATE -> reminder.targetDate?.let { "Due ${sdf.format(Date(it))}" } ?: reminder.title
                    ReminderType.MILEAGE -> reminder.targetMileage?.let { String.format(Locale.US, "At %.0f km", it) } ?: reminder.title
                }
                notify(
                    manager = notificationManager,
                    id = reminder.id.toInt(),
                    channelId = CHANNEL_REMINDERS,
                    title = reminder.title,
                    text = text,
                    deepLinkRoute = ROUTE_REMINDERS
                )
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
                val daysUntilDue = if (daysSince != null) check.intervalDays - daysSince else -check.intervalDays.toLong()

                val kmSince = if (check.intervalKm != null && lastOdo != null) car.currentOdometer - lastOdo else null
                val kmUntilDue = if (check.intervalKm != null && kmSince != null) check.intervalKm - kmSince else null

                val isOverdue = (lastAt == null) || (daysUntilDue < 0) || (kmUntilDue != null && kmUntilDue < 0)
                val isDueSoon = !isOverdue && (daysUntilDue in 0L..7L || (kmUntilDue != null && kmUntilDue >= 0.0 && kmUntilDue <= 500.0))
                if (isOverdue || isDueSoon) {
                    val reason = when {
                        lastAt == null                              -> "never checked"
                        isOverdue && kmUntilDue != null && kmUntilDue < 0 -> "${(-kmUntilDue).toInt()} km overdue"
                        isOverdue                                  -> "${(-daysUntilDue)}d overdue"
                        kmUntilDue != null                         -> "${kmUntilDue.toInt()} km remaining"
                        else                                       -> "${daysUntilDue}d remaining"
                    }
                    notify(
                        manager = notificationManager,
                        id = (car.id * 100 + check.checkType.ordinal).toInt(),
                        channelId = CHANNEL_HEALTH,
                        title = if (isOverdue) "${car.name} — ${check.checkType.displayName} overdue"
                                else "${car.name} — ${check.checkType.displayName} due soon",
                        text = reason,
                        deepLinkRoute = ROUTE_HEALTH
                    )
                }
            }
        }

        return Result.success()
    }

    private fun notify(
        manager: NotificationManager,
        id: Int,
        channelId: String,
        title: String,
        text: String,
        deepLinkRoute: String
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, deepLinkRoute)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
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
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val ROUTE_REMINDERS   = "reminders"
        const val ROUTE_HEALTH      = "reminders" // same screen, Health tab opens first

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
