package com.cartracker.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.cartracker.data.db.entities.Reminder
import com.cartracker.data.db.entities.ReminderType
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.ui.screens.fuellog.DatePickerField
import com.cartracker.ui.screens.fuellog.sheetFieldColors
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.RemindersViewModel
import com.cartracker.ui.viewmodel.RemindersViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(carId: Long?, cars: List<Car> = emptyList(), onCarSelected: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: RemindersViewModel = viewModel(
        factory = RemindersViewModelFactory(context.applicationContext as android.app.Application)
    )
    val reminders by viewModel.reminders.observeAsState(emptyList())

    val selectedCar = cars.firstOrNull { it.id == carId }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showCarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Reminders", fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
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
                FloatingActionButton(onClick = { editingReminder = null; showSheet = true },
                    containerColor = NeonCyan, contentColor = TrueBlack, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Add, "Add Reminder")
                }
            }
        }
    ) { padding ->
        if (carId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a car to manage reminders", color = OnSurfaceSecondary)
            }
        } else {
            val active = reminders.filter { !it.isCompleted }
            val done = reminders.filter { it.isCompleted }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (active.isEmpty() && done.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Notifications, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                                Text("No reminders set", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                                Text("Tap + to add a reminder", color = OnSurfaceSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
                if (active.isNotEmpty()) {
                    item {
                        Text("UPCOMING", color = NeonCyan, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 2.dp))
                    }
                    items(active, key = { it.id }) { r ->
                        ReminderCard(reminder = r,
                            onComplete = { viewModel.markCompleted(r.id) },
                            onEdit = { editingReminder = r; showSheet = true },
                            onDelete = { viewModel.deleteReminder(r) })
                    }
                }
                if (done.isNotEmpty()) {
                    item {
                        Text("COMPLETED", color = OnSurfaceSecondary, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = if (active.isNotEmpty()) 8.dp else 0.dp, bottom = 2.dp))
                    }
                    items(done, key = { it.id }) { r ->
                        ReminderCard(reminder = r, onComplete = null,
                            onEdit = null,
                            onDelete = { viewModel.deleteReminder(r) })
                    }
                }
            }
        }
    }

    if (showSheet && carId != null) {
        ReminderSheet(
            existingReminder = editingReminder,
            onDismiss = { showSheet = false; editingReminder = null },
            onSave = { title, type, mileage, date, notes ->
                if (editingReminder != null) viewModel.updateReminder(editingReminder!!, title, type, mileage, date, notes)
                else viewModel.addReminder(carId, title, type, mileage, date, notes)
                showSheet = false; editingReminder = null
            }
        )
    }

    if (showCarPicker && cars.size > 1) {
        CarPickerSheet(cars = cars, selectedCarId = carId, onSelect = { onCarSelected(it); showCarPicker = false }, onDismiss = { showCarPicker = false })
    }
}

// ─── Card ─────────────────────────────────────────────────────────────────────

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onComplete: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isDone = reminder.isCompleted
    val accentColor = if (isDone) OnSurfaceSecondary else WarnAmber

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, if (isDone) GlassBorder else accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)), Alignment.Center
                ) {
                    Icon(
                        if (isDone) Icons.Filled.CheckCircle else Icons.Filled.NotificationsActive,
                        null, tint = accentColor, modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(reminder.title, color = if (isDone) OnSurfaceSecondary else OnSurfacePrimary,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    when (reminder.type) {
                        ReminderType.MILEAGE -> reminder.targetMileage?.let {
                            Text("At %.0f km".format(it), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        ReminderType.DATE -> reminder.targetDate?.let {
                            Text("On ${sdf.format(Date(it))}", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (reminder.notes.isNotBlank()) {
                        Text(reminder.notes, color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row {
                if (onComplete != null) {
                    IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Check, "Done", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    }
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, "Edit", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─── Bottom Sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSheet(
    existingReminder: Reminder?,
    onDismiss: () -> Unit,
    onSave: (String, ReminderType, Double?, Long?, String) -> Unit
) {
    val isEdit = existingReminder != null
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var type by remember { mutableStateOf(existingReminder?.type ?: ReminderType.DATE) }
    var targetMileage by remember {
        mutableStateOf(existingReminder?.targetMileage?.let { String.format(Locale.US, "%.0f", it) } ?: "")
    }
    var targetDateMs by remember {
        mutableStateOf(existingReminder?.targetDate ?: System.currentTimeMillis())
    }
    var notes by remember { mutableStateOf(existingReminder?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

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
            Text(if (isEdit) "Edit Reminder" else "Add Reminder",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)

            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("What needs to be done?") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            // Trigger type toggle
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Remind me by", color = OnSurfaceSecondary, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceContainerHigh).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReminderType.entries.forEach { t ->
                        val selected = type == t
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) NeonCyanGlow else SurfaceContainerHigh)
                                .border(if (selected) 1.dp else 0.dp, if (selected) NeonCyanBorder else GlassBorder, RoundedCornerShape(8.dp))
                                .clickable { type = t }.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t.displayName, color = if (selected) NeonCyan else OnSurfaceSecondary,
                                style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }

            when (type) {
                ReminderType.DATE -> DatePickerField(dateMs = targetDateMs, sdf = sdf) { showDatePicker = true }
                ReminderType.MILEAGE -> OutlinedTextField(
                    value = targetMileage, onValueChange = { targetMileage = it },
                    label = { Text("Target Mileage (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors()
                )
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val mileage = if (type == ReminderType.MILEAGE) (targetMileage.toDoubleOrNull() ?: return@Button) else null
                    val date = if (type == ReminderType.DATE) targetDateMs else null
                    onSave(title, type, mileage, date, notes)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (isEdit) "Update" else "Save", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = targetDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { targetDateMs = it }; showDatePicker = false }) {
                    Text("OK", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = OnSurfaceSecondary) } }
        ) { DatePicker(state = state) }
    }
}
