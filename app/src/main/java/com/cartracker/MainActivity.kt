package com.cartracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.cartracker.ui.navigation.CarTrackerNavHost
import com.cartracker.ui.theme.CarTrackerTheme
import com.cartracker.worker.ReminderWorker

class MainActivity : AppCompatActivity() {

    private var pendingRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force-set the theme before super.onCreate() to prevent AppCompat from
        // losing the theme attributes when the activity is recreated after a
        // locale change via AppCompatDelegate.setApplicationLocales().
        setTheme(R.style.Theme_CarTracker)
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(ReminderWorker.EXTRA_NAVIGATE_TO)
        enableEdgeToEdge()
        setContent {
            CarTrackerTheme {
                CarTrackerNavHost(pendingRoute = pendingRoute)
                pendingRoute = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(ReminderWorker.EXTRA_NAVIGATE_TO)
    }
}
