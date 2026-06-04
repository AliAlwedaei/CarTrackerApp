package com.cartracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.ReminderType
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as CarTrackerApp).repository
        val reminders = repository.allActiveReminders.first()
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        reminders.forEach { reminder ->
            val shouldNotify = when (reminder.type) {
                ReminderType.DATE -> reminder.targetDate?.let { it <= now + TimeUnit.DAYS.toMillis(3) } == true
                ReminderType.MILEAGE -> false // mileage-based checked on car update
            }
            if (shouldNotify) {
                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Maintenance Reminder")
                    .setContentText(reminder.title + (reminder.targetDate?.let { " · Due: ${sdf.format(Date(it))}" } ?: ""))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(reminder.id.toInt(), notification)
            }
        }
        return Result.success()
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        val channel = NotificationChannel(CHANNEL_ID, "Maintenance Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Upcoming vehicle maintenance alerts"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "car_tracker_reminders"

        fun scheduleDailyCheck(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "reminder_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
