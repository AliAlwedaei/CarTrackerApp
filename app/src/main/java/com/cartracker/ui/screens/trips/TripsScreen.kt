package com.cartracker.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartracker.data.db.entities.Car
import com.cartracker.data.db.entities.Trip
import com.cartracker.data.db.entities.TripPurpose
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.ui.screens.fuellog.DatePickerField
import com.cartracker.ui.screens.fuellog.sheetFieldColors
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.TripsViewModel
import com.cartracker.ui.viewmodel.TripsViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(carId: Long?, cars: List<Car> = emptyList(), onCarSelected: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: TripsViewModel = viewModel(
        factory = TripsViewModelFactory(context.applicationContext as android.app.Application)
    )
    val trips by viewModel.trips.observeAsState(emptyList())
    val totalMileage by viewModel.totalMileage.observeAsState(0.0)
    val lastTrip by viewModel.lastTrip.observeAsState(null)

    val selectedCar = cars.firstOrNull { it.id == carId }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Trip?>(null) }
    var showCarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Trips & Mileage", fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
                        if (selectedCar != null) {
                            Text(selectedCar.name, color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer, titleContentColor = OnSurfacePrimary),
                actions = {
                    if (totalMileage > 0) {
                        Box(
                            modifier = Modifier.padding(end = if (cars.size > 1) 0.dp else 12.dp)
                                .clip(RoundedCornerShape(8.dp)).background(NeonCyanGlow)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("%.0f km total".format(totalMileage), color = NeonCyan,
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (cars.size > 1) {
                        IconButton(onClick = { showCarPicker = true }) {
                            Icon(Icons.Filled.SwapHoriz, "Switch car", tint = NeonCyan)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (carId != null) {
                FloatingActionButton(onClick = { editingTrip = null; showSheet = true },
                    containerColor = NeonCyan, contentColor = TrueBlack, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Add, "Add Trip")
                }
            }
        }
    ) { padding ->
        if (carId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a car to view trips", color = OnSurfaceSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (trips.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.DirectionsCar, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                                Text("No trips logged yet", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                                Text("Tap + to log a trip", color = OnSurfaceSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
                items(trips, key = { it.id }) { trip ->
                    TripCard(trip = trip,
                        onEdit = { editingTrip = trip; showSheet = true },
                        onDelete = { deleteTarget = trip })
                }
            }
        }
    }

    if (showSheet && carId != null) {
        TripSheet(
            existingTrip = editingTrip,
            lastEndOdometer = if (editingTrip == null) lastTrip?.endOdometer else null,
            onDismiss = { showSheet = false; editingTrip = null },
            onSave = { date, startOdo, endOdo, purpose, notes ->
                if (editingTrip != null) viewModel.updateTrip(editingTrip!!, date, startOdo, endOdo, purpose, notes)
                else viewModel.addTrip(carId, date, startOdo, endOdo, purpose, notes)
                showSheet = false; editingTrip = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete trip?", color = OnSurfacePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This cannot be undone.", color = OnSurfaceSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTrip(target); deleteTarget = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = OnSurfaceSecondary) } }
        )
    }

    if (showCarPicker && cars.size > 1) {
        CarPickerSheet(cars = cars, selectedCarId = carId, onSelect = { onCarSelected(it); showCarPicker = false }, onDismiss = { showCarPicker = false })
    }
}

// ─── Card ─────────────────────────────────────────────────────────────────────

@Composable
private fun TripCard(trip: Trip, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val purposeColor = if (trip.purpose == TripPurpose.WORK) WarnAmber else NeonCyan
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(purposeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text(trip.purpose.displayName, color = purposeColor,
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Text(sdf.format(Date(trip.date)), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, "Edit", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("%.1f km".format(trip.distance), color = OnSurfacePrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("driven", color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text("Start", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("%.0f km".format(trip.startOdometer), color = OnSurfacePrimary, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("End", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("%.0f km".format(trip.endOdometer), color = OnSurfacePrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (trip.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(trip.notes, color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ─── Bottom Sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripSheet(
    existingTrip: Trip?,
    lastEndOdometer: Double?,
    onDismiss: () -> Unit,
    onSave: (Long, Double, Double, TripPurpose, String) -> Unit
) {
    val isEdit = existingTrip != null
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var dateMs by remember { mutableStateOf(existingTrip?.date ?: System.currentTimeMillis()) }
    var distanceOnly by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf(existingTrip?.distance?.let { String.format(Locale.US, "%.1f", it) } ?: "") }
    var startOdo by remember {
        mutableStateOf(
            existingTrip?.startOdometer?.let { String.format(Locale.US, "%.0f", it) }
                ?: lastEndOdometer?.let { String.format(Locale.US, "%.0f", it) } ?: ""
        )
    }
    var endOdo by remember { mutableStateOf(existingTrip?.endOdometer?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
    var purpose by remember { mutableStateOf(existingTrip?.purpose ?: TripPurpose.PERSONAL) }
    var notes by remember { mutableStateOf(existingTrip?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val computedDistance = if (distanceOnly) {
        distance.toDoubleOrNull() ?: 0.0
    } else {
        (endOdo.toDoubleOrNull() ?: 0.0) - (startOdo.toDoubleOrNull() ?: 0.0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (isEdit) "Edit Trip" else "Log Trip",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)

            DatePickerField(dateMs = dateMs, sdf = sdf) { showDatePicker = true }

            // Mode toggle
            if (!isEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(false to "Odometer", true to "Distance Only").forEach { (mode, label) ->
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (distanceOnly == mode) NeonCyanGlow else SurfaceContainerHigh)
                                .border(
                                    if (distanceOnly == mode) 1.dp else 0.dp,
                                    if (distanceOnly == mode) NeonCyanBorder else GlassBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { distanceOnly = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (distanceOnly == mode) NeonCyan else OnSurfaceSecondary,
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (distanceOnly) {
                OutlinedTextField(value = distance, onValueChange = { distance = it },
                    label = { Text("Distance (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = startOdo, onValueChange = { startOdo = it },
                        label = { Text("Start Odo (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = sheetFieldColors(),
                        suffix = if (lastEndOdometer != null && !isEdit) {
                            { Text("last: ${String.format(Locale.US, "%.0f", lastEndOdometer)}", color = OnSurfaceSecondary, fontSize = 10.sp) }
                        } else null)
                    OutlinedTextField(value = endOdo, onValueChange = { endOdo = it },
                        label = { Text("End Odo (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = sheetFieldColors())
                }
            }

            if (computedDistance > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distance", color = NeonCyan, style = MaterialTheme.typography.bodyMedium)
                    Text("%.1f km".format(computedDistance), color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            // Purpose chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Purpose", color = OnSurfaceSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripPurpose.entries.forEach { p ->
                        val selected = purpose == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) NeonCyanGlow else SurfaceContainerHigh)
                                .border(1.dp, if (selected) NeonCyanBorder else GlassBorder, RoundedCornerShape(8.dp))
                                .clickable { purpose = p }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(p.displayName, color = if (selected) NeonCyan else OnSurfaceSecondary,
                                style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Button(
                onClick = {
                    val finalStartOdo: Double
                    val finalEndOdo: Double
                    if (distanceOnly) {
                        val dist = distance.toDoubleOrNull() ?: return@Button
                        if (dist <= 0) return@Button
                        val base = lastEndOdometer ?: 0.0
                        finalStartOdo = base
                        finalEndOdo = base + dist
                    } else {
                        val s = startOdo.toDoubleOrNull() ?: return@Button
                        val e = endOdo.toDoubleOrNull() ?: return@Button
                        if (e <= s) return@Button
                        finalStartOdo = s; finalEndOdo = e
                    }
                    onSave(dateMs, finalStartOdo, finalEndOdo, purpose, notes)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (isEdit) "Update" else "Save", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { dateMs = it }; showDatePicker = false }) {
                    Text("OK", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = OnSurfaceSecondary) } }
        ) { DatePicker(state = state) }
    }
}
