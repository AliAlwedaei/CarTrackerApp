package com.cartracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cartracker.ui.navigation.CarTrackerNavHost
import com.cartracker.ui.theme.CarTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarTrackerTheme {
                CarTrackerNavHost()
            }
        }
    }
}
