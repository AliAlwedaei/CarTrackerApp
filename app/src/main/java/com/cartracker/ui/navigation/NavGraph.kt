package com.cartracker.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartracker.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.cartracker.ui.screens.cars.CarsScreen
import com.cartracker.ui.screens.dashboard.DashboardScreen
import com.cartracker.ui.screens.expenses.ExpenseScreen
import com.cartracker.ui.screens.fuellog.FuelLogScreen
import com.cartracker.ui.screens.history.HistoryScreen
import com.cartracker.ui.screens.maintenance.MaintenanceScreen
import com.cartracker.ui.screens.reminders.RemindersScreen
import com.cartracker.ui.screens.reports.ReportsScreen
import com.cartracker.ui.screens.settings.SettingsScreen
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.CarsViewModel
import com.cartracker.ui.viewmodel.CarsViewModelFactory

sealed class Screen(val route: String, val label: String) {
    object Dashboard   : Screen("dashboard",   "Home")
    object FuelLog     : Screen("fuel_log",    "Fuel")
    object Maintenance : Screen("maintenance", "Service")
    object Expenses    : Screen("expenses",    "Expenses")
    object Reminders   : Screen("reminders",   "Alerts")
    object Cars        : Screen("cars",        "Cars")
    object Settings    : Screen("settings",    "Settings")
    object Reports     : Screen("reports",     "Reports")
    object History     : Screen("history",     "History")
}

@Composable
fun CarTrackerNavHost(pendingRoute: String? = null) {
    val context = LocalContext.current
    val carsViewModel: CarsViewModel = viewModel(
        factory = CarsViewModelFactory(context.applicationContext as android.app.Application)
    )
    val cars by carsViewModel.cars.observeAsState(emptyList())
    val selectedCarId by carsViewModel.selectedCarId.observeAsState()
    val activeCarId = selectedCarId ?: cars.firstOrNull()?.id

    val navController = rememberNavController()

    // Speed-dial FAB state
    var fabExpanded by remember { mutableStateOf(false) }

    // Auto-open sheet flags hoisted here so NavGraph controls them
    var autoOpenFuel      by remember { mutableStateOf(false) }
    var autoOpenMaint     by remember { mutableStateOf(false) }
    var autoOpenExpense   by remember { mutableStateOf(false) }

    // Deep-link from notification
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                launchSingleTop = true
            }
        }
    }

    val navItems = listOf(
        Triple(Screen.Dashboard,   Icons.Filled.Home,             R.string.nav_home),
        Triple(Screen.FuelLog,     Icons.Filled.LocalGasStation,  R.string.nav_fuel),
        Triple(Screen.Maintenance, Icons.Filled.Build,            R.string.nav_service),
        Triple(Screen.Expenses,    Icons.Filled.Receipt,          R.string.nav_expenses),
        Triple(Screen.Reminders,   Icons.Filled.Notifications,    R.string.nav_alerts),
        Triple(Screen.Cars,        Icons.Filled.Garage,           R.string.nav_cars),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show global FAB only on screens that don't have their own add sheet
    val showGlobalFab = currentRoute in listOf(
        Screen.Dashboard.route, Screen.Cars.route, Screen.Reports.route, Screen.History.route
    )

    // Dismiss FAB on navigation
    LaunchedEffect(currentRoute) { fabExpanded = false }

    Scaffold(
        containerColor = TrueBlack,
        floatingActionButton = {
            if (showGlobalFab) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Mini FABs (visible when expanded)
                    AnimatedVisibility(visible = fabExpanded, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SpeedDialItem(label = "Expense", icon = Icons.Filled.Receipt) {
                                fabExpanded = false
                                autoOpenExpense = true
                                navController.navigate(Screen.Expenses.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                            SpeedDialItem(label = "Service", icon = Icons.Filled.Build) {
                                fabExpanded = false
                                autoOpenMaint = true
                                navController.navigate(Screen.Maintenance.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                            SpeedDialItem(label = "Fuel", icon = Icons.Filled.LocalGasStation) {
                                fabExpanded = false
                                autoOpenFuel = true
                                navController.navigate(Screen.FuelLog.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        }
                    }
                    // Main FAB
                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = NeonCyan,
                        contentColor = TrueBlack,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = if (fabExpanded) "Close" else "Quick add"
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                NavigationBar(containerColor = SurfaceContainer, tonalElevation = 0.dp) {
                    val currentDestination = navBackStackEntry?.destination
                    navItems.forEach { (screen, icon, labelRes) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        val label = stringResource(labelRes)
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(label, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan, selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyanGlow,
                                unselectedIconColor = OnSurfaceSecondary, unselectedTextColor = OnSurfaceSecondary
                            ),
                            onClick = {
                                fabExpanded = false
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    carId = activeCarId, cars = cars,
                    onCarSelected = { carsViewModel.selectCar(it) },
                    onAddFuel = {
                        navController.navigate(Screen.FuelLog.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onAddService = {
                        navController.navigate(Screen.Maintenance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onViewReports = { navController.navigate(Screen.Reports.route) },
                    onViewAlerts = {
                        navController.navigate(Screen.Reminders.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
            }
            composable(Screen.FuelLog.route) {
                FuelLogScreen(
                    carId = activeCarId, cars = cars, onCarSelected = { carsViewModel.selectCar(it) },
                    autoOpenSheet = autoOpenFuel, onSheetHandled = { autoOpenFuel = false }
                )
            }
            composable(Screen.Maintenance.route) {
                MaintenanceScreen(
                    carId = activeCarId, cars = cars, onCarSelected = { carsViewModel.selectCar(it) },
                    autoOpenSheet = autoOpenMaint, onSheetHandled = { autoOpenMaint = false }
                )
            }
            composable(Screen.Expenses.route) {
                ExpenseScreen(
                    carId = activeCarId, cars = cars, onCarSelected = { carsViewModel.selectCar(it) },
                    autoOpenSheet = autoOpenExpense, onSheetHandled = { autoOpenExpense = false }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(carId = activeCarId, cars = cars, onCarSelected = { carsViewModel.selectCar(it) })
            }
            composable(Screen.Reminders.route) {
                RemindersScreen(carId = activeCarId, cars = cars, onCarSelected = { carsViewModel.selectCar(it) })
            }
            composable(Screen.Cars.route) {
                CarsScreen(carsViewModel = carsViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onViewReports = { navController.navigate(Screen.Reports.route) }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(carId = activeCarId, cars = cars, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun SpeedDialItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = SurfaceContainerHigh,
            shadowElevation = 2.dp
        ) {
            Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = OnSurfacePrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = SurfaceContainerHigh,
            contentColor = NeonCyan,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}
