package com.cartracker.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.cartracker.ui.screens.cars.CarsScreen
import com.cartracker.ui.screens.dashboard.DashboardScreen
import com.cartracker.ui.screens.fuellog.FuelLogScreen
import com.cartracker.ui.screens.maintenance.MaintenanceScreen
import com.cartracker.ui.screens.reminders.RemindersScreen
import com.cartracker.ui.screens.trips.TripsScreen
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.CarsViewModel
import com.cartracker.ui.viewmodel.CarsViewModelFactory

sealed class Screen(val route: String, val label: String) {
    object Dashboard : Screen("dashboard", "Home")
    object FuelLog : Screen("fuel_log", "Fuel")
    object Maintenance : Screen("maintenance", "Service")
    object Trips : Screen("trips", "Trips")
    object Reminders : Screen("reminders", "Alerts")
    object Cars : Screen("cars", "Cars")
}

@Composable
fun CarTrackerNavHost() {
    val context = LocalContext.current
    val carsViewModel: CarsViewModel = viewModel(
        factory = CarsViewModelFactory(context.applicationContext as android.app.Application)
    )
    val cars by carsViewModel.cars.observeAsState(emptyList())
    val selectedCarId by carsViewModel.selectedCarId.observeAsState()
    val activeCarId = selectedCarId ?: cars.firstOrNull()?.id

    val navController = rememberNavController()

    val navItems = listOf(
        Triple(Screen.Dashboard, Icons.Filled.Home, "Home"),
        Triple(Screen.FuelLog, Icons.Filled.LocalGasStation, "Fuel"),
        Triple(Screen.Maintenance, Icons.Filled.Build, "Service"),
        Triple(Screen.Trips, Icons.Filled.DirectionsCar, "Trips"),
        Triple(Screen.Reminders, Icons.Filled.Notifications, "Alerts"),
        Triple(Screen.Cars, Icons.Filled.DirectionsCar, "Cars"),
    )

    Scaffold(
        containerColor = TrueBlack,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = GlassBorder,
                    thickness = 0.5.dp
                )
                NavigationBar(
                    containerColor = SurfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    navItems.forEach { (screen, icon, label) ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyanGlow,
                                unselectedIconColor = OnSurfaceSecondary,
                                unselectedTextColor = OnSurfaceSecondary
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    carId = activeCarId,
                    cars = cars,
                    onCarSelected = { carsViewModel.selectCar(it) },
                    onAddFuel = {
                        navController.navigate(Screen.FuelLog.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddTrip = {
                        navController.navigate(Screen.Trips.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddService = {
                        navController.navigate(Screen.Maintenance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.FuelLog.route) {
                FuelLogScreen(carId = activeCarId)
            }
            composable(Screen.Maintenance.route) {
                MaintenanceScreen(carId = activeCarId)
            }
            composable(Screen.Trips.route) {
                TripsScreen(carId = activeCarId)
            }
            composable(Screen.Reminders.route) {
                RemindersScreen(carId = activeCarId)
            }
            composable(Screen.Cars.route) {
                CarsScreen(carsViewModel = carsViewModel)
            }
        }
    }
}
