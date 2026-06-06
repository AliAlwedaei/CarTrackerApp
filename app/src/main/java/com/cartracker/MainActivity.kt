package com.cartracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.cartracker.ui.navigation.CarTrackerNavHost
import com.cartracker.ui.theme.CarTrackerTheme

class MainActivity : AppCompatActivity() {
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
