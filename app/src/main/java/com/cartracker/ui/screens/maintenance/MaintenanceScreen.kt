package com.cartracker.ui.screens.maintenance

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
import com.cartracker.data.db.entities.MaintenanceCategory
import com.cartracker.data.db.entities.MaintenanceLog
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.ui.screens.fuellog.DatePickerField
import com.cartracker.ui.screens.fuellog.sheetFieldColors
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.MaintenanceViewModel
import com.cartracker.ui.viewmodel.MaintenanceViewModelFactory
import com.cartracker.util.CurrencyPrefs
import com.cartracker.util.ServiceTemplates
import java.text.SimpleDateFormat
import java.util.*

private val categoryIcons = mapOf(
    MaintenanceCategory.OIL_CHANGE   to Icons.Filled.WaterDrop,
    MaintenanceCategory.TIRES        to Icons.Filled.RadioButtonUnchecked,
    MaintenanceCategory.BRAKES       to Icons.Filled.Report,
    MaintenanceCategory.BATTERY      to Icons.Filled.BatteryFull,
    MaintenanceCategory.FILTERS      to Icons.Filled.FilterList,
    MaintenanceCategory.WIPERS       to Icons.Filled.Opacity,
    MaintenanceCategory.INSURANCE    to Icons.Filled.Security,
    MaintenanceCategory.REGISTRATION to Icons.Filled.Description,
    MaintenanceCategory.TRANSMISSION to Icons.Filled.Settings,
    MaintenanceCategory.AC_SERVICE   to Icons.Filled.AcUnit,
    MaintenanceCategory.SPARK_PLUGS  to Icons.Filled.ElectricBolt,
    MaintenanceCategory.ALIGNMENT    to Icons.Filled.Tune,
    MaintenanceCategory.TIMING_BELT  to Icons.Filled.Loop,
    MaintenanceCategory.OTHER        to Icons.Filled.Build
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    carId: Long?,
    cars: List<Car> = emptyList(),
    onCarSelected: (Long) -> Unit = {},
    autoOpenSheet: Boolean = false,
    onSheetHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val currency = remember { CurrencyPrefs.getSymbol(context) }
    val viewModel: MaintenanceViewModel = viewModel(
        factory = MaintenanceViewModelFactory(context.applicationContext as android.app.Application)
    )
    val logs by viewModel.maintenanceLogs.observeAsState(emptyList())

    val selectedCar = cars.firstOrNull { it.id == carId }
    var editingLog by remember { mutableStateOf<MaintenanceLog?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MaintenanceLog?>(null) }
    var showCarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }
    LaunchedEffect(autoOpenSheet) {
        if (autoOpenSheet && carId != null) { showSheet = true; onSheetHandled() }
    }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Maintenance Log", fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
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
                FloatingActionButton(onClick = { editingLog = null; showSheet = true },
                    containerColor = NeonCyan, contentColor = TrueBlack, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Add, "Add Service")
                }
            }
        }
    ) { padding ->
        if (carId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a car to view maintenance records", color = OnSurfaceSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.Build, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                                Text("No service records yet", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                                Text("Log every service to track your maintenance history", color = OnSurfaceSecondary, fontSize = 13.sp)
                                Button(
                                    onClick = { showSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Log First Service", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
                items(logs, key = { it.id }) { log ->
                    MaintenanceCard(log = log, currency = currency,
                        onEdit = { editingLog = log; showSheet = true },
                        onDelete = { deleteTarget = log })
                }
            }
        }
    }

    if (showSheet && carId != null) {
        MaintenanceSheet(
            existingLog = editingLog,
            currency = currency,
            onDismiss = { showSheet = false; editingLog = null },
            onSave = { cat, type, date, mileage, cost, garage, nextKm, warrantyDate, warrantyNotes, notes ->
                if (editingLog != null) viewModel.updateLog(editingLog!!, cat, type, date, mileage, cost, garage, nextKm, warrantyDate, warrantyNotes, notes)
                else viewModel.addMaintenanceLog(carId, cat, type, date, mileage, cost, garage, nextKm, warrantyDate, warrantyNotes, notes)
                showSheet = false; editingLog = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete record?", color = OnSurfacePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this service record.", color = OnSurfaceSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteLog(target); deleteTarget = null }) {
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
private fun MaintenanceCard(log: MaintenanceLog, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val icon = categoryIcons[log.category] ?: Icons.Filled.Build
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(NeonCyanGlow), Alignment.Center) {
                        Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(log.serviceType, color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(log.category.displayName, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    }
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
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text("Date", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(sdf.format(Date(log.date)), color = OnSurfacePrimary, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Mileage", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("%.0f km".format(log.mileage), color = OnSurfacePrimary, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Cost", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("$currency %.3f".format(log.cost), color = OnSurfacePrimary,
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            if (log.garage.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.LocationOn, null, tint = OnSurfaceSecondary, modifier = Modifier.size(12.dp))
                    Text(log.garage, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (log.nextServiceKm != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Schedule, null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                    Text("Next service at %.0f km".format(log.nextServiceKm), color = NeonCyan,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            if (log.warrantyExpiryDate != null) {
                Spacer(Modifier.height(6.dp))
                val now = System.currentTimeMillis()
                val daysLeft = ((log.warrantyExpiryDate - now) / (1000L * 60 * 60 * 24)).toInt()
                val warrantyColor = when {
                    daysLeft < 0 -> OnSurfaceSecondary
                    daysLeft <= 30 -> WarnAmber
                    else -> SuccessGreen
                }
                val warrantyLabel = when {
                    daysLeft < 0 -> "Warranty expired ${sdf.format(Date(log.warrantyExpiryDate))}"
                    daysLeft == 0 -> "Warranty expires today"
                    daysLeft <= 30 -> "Warranty expiring in $daysLeft days"
                    else -> "Warranty until ${sdf.format(Date(log.warrantyExpiryDate))}"
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Verified, null, tint = warrantyColor, modifier = Modifier.size(12.dp))
                    Text(warrantyLabel, color = warrantyColor, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (log.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(log.notes, color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ─── Bottom Sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceSheet(
    existingLog: MaintenanceLog?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (MaintenanceCategory, String, Long, Double, Double, String, Double?, Long?, String, String) -> Unit
) {
    val isEdit = existingLog != null
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var selectedCategory by remember { mutableStateOf(existingLog?.category ?: MaintenanceCategory.OIL_CHANGE) }
    var serviceType by remember { mutableStateOf(existingLog?.serviceType ?: "") }
    var dateMs by remember { mutableStateOf(existingLog?.date ?: System.currentTimeMillis()) }
    var mileage by remember { mutableStateOf(existingLog?.mileage?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
    var cost by remember { mutableStateOf(existingLog?.cost?.let { String.format(Locale.US, "%.3f", it) } ?: "") }
    var garage by remember { mutableStateOf(existingLog?.garage ?: "") }
    var nextServiceKm by remember { mutableStateOf(existingLog?.nextServiceKm?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
    var warrantyExpiryMs by remember { mutableStateOf(existingLog?.warrantyExpiryDate) }
    var warrantyNotes by remember { mutableStateOf(existingLog?.warrantyNotes ?: "") }
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showWarrantyPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory) {
        if (!isEdit && serviceType.isBlank()) {
            serviceType = selectedCategory.displayName
        }
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
            Text(if (isEdit) "Edit Service Record" else "Log Service",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)

            DatePickerField(dateMs = dateMs, sdf = sdf) { showDatePicker = true }

            // Category chips (3 columns)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category", color = OnSurfaceSecondary, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaintenanceCategory.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { cat ->
                                val selected = selectedCategory == cat
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) NeonCyanGlow else SurfaceContainerHigh)
                                        .border(1.dp, if (selected) NeonCyanBorder else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedCategory = cat; if (!isEdit) serviceType = cat.displayName }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.displayName, color = if (selected) NeonCyan else OnSurfaceSecondary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            OutlinedTextField(value = serviceType, onValueChange = { serviceType = it },
                label = { Text("Service Description") }, modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = mileage, onValueChange = { mileage = it },
                    label = { Text("Mileage (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = cost, onValueChange = { cost = it },
                    label = { Text("Cost ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }

            // Service template suggestion
            val template = ServiceTemplates.forCategory(selectedCategory)
            val currentMileageVal = mileage.toDoubleOrNull()
            if (template != null && currentMileageVal != null && nextServiceKm.isBlank()) {
                val suggestedNextKm = (currentMileageVal + template.intervalKm).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyanGlow)
                        .border(1.dp, NeonCyanBorder, RoundedCornerShape(10.dp))
                        .clickable { nextServiceKm = suggestedNextKm.toString() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lightbulb, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("Recommended: next at %,d km".format(suggestedNextKm),
                                color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text(template.intervalLabel, color = NeonCyan.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    }
                    Text("Use", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(value = garage, onValueChange = { garage = it },
                label = { Text("Garage / Workshop (optional)") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = OnSurfaceSecondary, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            OutlinedTextField(value = nextServiceKm, onValueChange = { nextServiceKm = it },
                label = { Text("Next service at (km) — optional") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Auto-creates a mileage reminder", color = OnSurfaceSecondary, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Filled.Schedule, null, tint = OnSurfaceSecondary, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            // Warranty expiry (optional)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Warranty (optional)", color = OnSurfaceSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(4.dp))
                            .background(SurfaceContainerHigh)
                            .clickable { showWarrantyPicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(
                                warrantyExpiryMs?.let { sdf.format(Date(it)) } ?: "No warranty date",
                                color = if (warrantyExpiryMs != null) OnSurfacePrimary else OnSurfaceSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            if (warrantyExpiryMs != null) {
                                Icon(Icons.Filled.Close, "Clear", tint = OnSurfaceSecondary, modifier = Modifier.size(16.dp).clickable { warrantyExpiryMs = null })
                            } else {
                                Icon(Icons.Filled.CalendarMonth, null, tint = OnSurfaceSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                if (warrantyExpiryMs != null) {
                    OutlinedTextField(value = warrantyNotes, onValueChange = { warrantyNotes = it },
                        label = { Text("Warranty details (e.g. 3yr / 50,000 km)") },
                        modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Button(
                onClick = {
                    if (serviceType.isBlank()) return@Button
                    val mi = mileage.toDoubleOrNull() ?: return@Button
                    val co = cost.toDoubleOrNull() ?: return@Button
                    val nextKm = nextServiceKm.toDoubleOrNull()
                    onSave(selectedCategory, serviceType, dateMs, mi, co, garage, nextKm, warrantyExpiryMs, warrantyNotes, notes)
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

    if (showWarrantyPicker) {
        val warrantyState = rememberDatePickerState(
            initialSelectedDateMillis = warrantyExpiryMs ?: (System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showWarrantyPicker = false },
            confirmButton = {
                TextButton(onClick = { warrantyExpiryMs = warrantyState.selectedDateMillis; showWarrantyPicker = false }) {
                    Text("Set Expiry", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showWarrantyPicker = false }) { Text("Cancel", color = OnSurfaceSecondary) } }
        ) { DatePicker(state = warrantyState) }
    }
}
