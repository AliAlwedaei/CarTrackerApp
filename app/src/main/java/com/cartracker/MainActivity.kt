package com.cartracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.cartracker.ui.navigation.CarTrackerNavHost
import com.cartracker.ui.theme.CarTrackerTheme
import com.cartracker.worker.ReminderWorker

class MainActivity : AppCompatActivity() {

    private var pendingRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(ReminderWorker.EXTRA_NAVIGATE_TO)
        enableEdgeToEdge()
        setContent {
            CarTrackerTheme {
                val route = pendingRoute
                CarTrackerNavHost(pendingRoute = route)
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
