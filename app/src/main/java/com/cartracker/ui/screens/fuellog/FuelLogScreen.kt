package com.cartracker.ui.screens.fuellog

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
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.data.db.entities.FuelType
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.FuelLogViewModel
import com.cartracker.ui.viewmodel.FuelLogViewModelFactory
import com.cartracker.util.CurrencyPrefs
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelLogScreen(carId: Long?, cars: List<Car> = emptyList(), onCarSelected: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val currency = remember { CurrencyPrefs.getSymbol(context) }
    val viewModel: FuelLogViewModel = viewModel(
        factory = FuelLogViewModelFactory(context.applicationContext as android.app.Application)
    )
    val fuelLogs by viewModel.fuelLogs.observeAsState(emptyList())
    val lastOdometer by viewModel.lastOdometer.observeAsState(null)
    val lastPricePerLiter by viewModel.lastPricePerLiter.observeAsState(null)

    val avgEfficiency = remember(fuelLogs) {
        fuelLogs.filter { it.fuelEfficiency > 0 && it.isFullTank }.map { it.fuelEfficiency }
            .average().takeIf { it.isFinite() } ?: 0.0
    }

    val selectedCar = cars.firstOrNull { it.id == carId }
    var editingLog by remember { mutableStateOf<FuelLog?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FuelLog?>(null) }
    var showCarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Fuel Log", fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
                        if (selectedCar != null) {
                            Text(selectedCar.name, color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    if (cars.size > 1) {
                        IconButton(onClick = { showCarPicker = true }) {
                            Icon(Icons.Filled.SwapHoriz, "Switch car", tint = NeonCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer, titleContentColor = OnSurfacePrimary)
            )
        },
        floatingActionButton = {
            if (carId != null) {
                FloatingActionButton(
                    onClick = { editingLog = null; showSheet = true },
                    containerColor = NeonCyan, contentColor = TrueBlack,
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Filled.Add, "Add Fuel Log") }
            }
        }
    ) { padding ->
        if (carId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a car to view fuel logs", color = OnSurfaceSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (fuelLogs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.LocalGasStation, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                                Text("No fuel logs yet", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                                Text("Track every fill-up to see fuel economy trends", color = OnSurfaceSecondary, fontSize = 13.sp)
                                Button(
                                    onClick = { showSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Log First Fill-up", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
                items(fuelLogs, key = { it.id }) { log ->
                    FuelLogCard(
                        log = log,
                        avgEfficiency = avgEfficiency,
                        currency = currency,
                        onEdit = { editingLog = log; showSheet = true },
                        onDelete = { deleteTarget = log }
                    )
                }
            }
        }
    }

    if (showSheet && carId != null) {
        FuelLogSheet(
            existingLog = editingLog,
            lastOdometer = if (editingLog == null) lastOdometer else null,
            lastPricePerLiter = if (editingLog == null) lastPricePerLiter else null,
            currency = currency,
            onDismiss = { showSheet = false; editingLog = null },
            onSave = { date, odo, liters, cpl, isFullTank, fuelType, notes ->
                if (editingLog != null) viewModel.updateFuelLog(editingLog!!, date, odo, liters, cpl, isFullTank, fuelType, notes)
                else viewModel.addFuelLog(carId, date, odo, liters, cpl, isFullTank, fuelType, notes)
                showSheet = false; editingLog = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete entry?", color = OnSurfacePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this fill-up record.", color = OnSurfaceSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFuelLog(target); deleteTarget = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = OnSurfaceSecondary) }
            }
        )
    }

    if (showCarPicker && cars.size > 1) {
        CarPickerSheet(cars = cars, selectedCarId = carId, onSelect = { onCarSelected(it); showCarPicker = false }, onDismiss = { showCarPicker = false })
    }
}

// ─── Card ─────────────────────────────────────────────────────────────────────

@Composable
private fun FuelLogCard(
    log: FuelLog,
    avgEfficiency: Double,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val efficiencyTrend = when {
        log.fuelEfficiency <= 0 || avgEfficiency <= 0 -> null
        log.fuelEfficiency >= avgEfficiency * 1.03 -> true  // 3%+ above avg = good
        log.fuelEfficiency <= avgEfficiency * 0.97 -> false // 3%+ below avg = bad
        else -> null
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(sdf.format(Date(log.date)), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelMedium)
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
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatCell("Odometer", "%.0f km".format(log.odometer))
                StatCell("Liters", "%.2f L".format(log.liters))
                StatCell("Total", "$currency %.3f".format(log.totalCost))
            }
            if (log.fuelEfficiency > 0 || !log.isFullTank || log.fuelType != FuelType.REGULAR) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (log.fuelType != FuelType.REGULAR) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceContainerHighest).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(log.fuelType.displayName, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (!log.isFullTank) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHighest)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Partial", color = OnSurfaceSecondary,
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (log.fuelEfficiency > 0) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(NeonCyanGlow)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "%.1f km/L".format(log.fuelEfficiency),
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                efficiencyTrend?.let { isGood ->
                                    Icon(
                                        if (isGood) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                        null,
                                        tint = if (isGood) SuccessGreen else ErrorRed,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (log.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(log.notes, color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column {
        Text(label, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = OnSurfacePrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Bottom Sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelLogSheet(
    existingLog: FuelLog?,
    lastOdometer: Double?,
    lastPricePerLiter: Double?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Long, Double, Double, Double, Boolean, FuelType, String) -> Unit
) {
    val isEdit = existingLog != null
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var dateMs by remember { mutableStateOf(existingLog?.date ?: System.currentTimeMillis()) }
    var odometer by remember {
        mutableStateOf(
            existingLog?.odometer?.let { String.format(Locale.US, "%.0f", it) }
                ?: lastOdometer?.let { String.format(Locale.US, "%.0f", it) } ?: ""
        )
    }
    var liters by remember { mutableStateOf(existingLog?.liters?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var costPerLiter by remember {
        mutableStateOf(
            existingLog?.costPerLiter?.let { String.format(Locale.US, "%.3f", it) }
                ?: lastPricePerLiter?.let { String.format(Locale.US, "%.3f", it) } ?: ""
        )
    }
    var isFullTank by remember { mutableStateOf(existingLog?.isFullTank ?: true) }
    var selectedFuelType by remember { mutableStateOf(existingLog?.fuelType ?: FuelType.REGULAR) }
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var odometerError by remember { mutableStateOf(false) }

    val totalCost = (liters.toDoubleOrNull() ?: 0.0) * (costPerLiter.toDoubleOrNull() ?: 0.0)

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
            Text(if (isEdit) "Edit Fill-up" else "Log Fill-up",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)

            DatePickerField(dateMs = dateMs, sdf = sdf) { showDatePicker = true }

            OutlinedTextField(
                value = odometer, onValueChange = { odometer = it; odometerError = false },
                label = { Text("Odometer (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = odometerError,
                supportingText = if (odometerError) {
                    { Text("Must be higher than last reading (${String.format(Locale.US, "%.0f", lastOdometer)} km)", color = ErrorRed) }
                } else null,
                suffix = if (lastOdometer != null && !isEdit && !odometerError) {
                    { Text("last: ${String.format(Locale.US, "%.0f", lastOdometer)}", color = OnSurfaceSecondary, fontSize = 11.sp) }
                } else null,
                colors = sheetFieldColors()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = liters, onValueChange = { liters = it }, label = { Text("Liters") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(
                    value = costPerLiter, onValueChange = { costPerLiter = it },
                    label = { Text("Price / L ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors()
                )
            }

            // Fuel type chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Fuel Type", color = OnSurfaceSecondary, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FuelType.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ft ->
                                val selected = selectedFuelType == ft
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) NeonCyanGlow else SurfaceContainerHigh)
                                        .border(1.dp, if (selected) NeonCyanBorder else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedFuelType = ft }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(ft.displayName, color = if (selected) NeonCyan else OnSurfaceSecondary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // Full tank toggle
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { isFullTank = !isFullTank }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Full Tank Fill-up", color = OnSurfacePrimary, fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (isFullTank) "Fuel efficiency will be calculated" else "Efficiency skipped for partial fill-up",
                        color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall
                    )
                }
                Switch(
                    checked = isFullTank,
                    onCheckedChange = { isFullTank = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TrueBlack, checkedTrackColor = NeonCyan)
                )
            }

            if (totalCost > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Cost", color = NeonCyan, style = MaterialTheme.typography.bodyMedium)
                    Text("$currency %.3f".format(totalCost), color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Button(
                onClick = {
                    val odo = odometer.toDoubleOrNull() ?: return@Button
                    val lit = liters.toDoubleOrNull() ?: return@Button
                    val cpl = costPerLiter.toDoubleOrNull() ?: return@Button
                    if (!isEdit && lastOdometer != null && odo < lastOdometer) {
                        odometerError = true; return@Button
                    }
                    onSave(dateMs, odo, lit, cpl, isFullTank, selectedFuelType, notes)
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

// ─── Shared UI helpers ────────────────────────────────────────────────────────

@Composable
internal fun DatePickerField(dateMs: Long, sdf: SimpleDateFormat, onTap: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(4.dp))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Date", color = OnSurfaceSecondary, fontSize = 11.sp)
                Text(sdf.format(Date(dateMs)), color = OnSurfacePrimary, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Filled.CalendarMonth, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan, cursorColor = NeonCyan,
    unfocusedBorderColor = GlassBorder, unfocusedLabelColor = OnSurfaceSecondary,
    focusedTextColor = OnSurfacePrimary, unfocusedTextColor = OnSurfacePrimary,
    focusedContainerColor = SurfaceContainerHigh, unfocusedContainerColor = SurfaceContainerHigh,
    focusedSuffixColor = OnSurfaceSecondary, unfocusedSuffixColor = OnSurfaceSecondary
)
