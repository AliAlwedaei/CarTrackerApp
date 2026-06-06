package com.cartracker.ui.screens.cars

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cartracker.data.db.entities.Car
import com.cartracker.ui.components.CarLogoImage
import com.cartracker.ui.screens.fuellog.sheetFieldColors
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.CarsViewModel
import com.cartracker.util.PhotoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarsScreen(carsViewModel: CarsViewModel) {
    val cars by carsViewModel.cars.observeAsState(emptyList())
    val selectedCarId by carsViewModel.selectedCarId.observeAsState()
    val activeId = selectedCarId ?: cars.firstOrNull()?.id

    var showSheet by remember { mutableStateOf(false) }
    var editCar by remember { mutableStateOf<Car?>(null) }
    var deleteTarget by remember { mutableStateOf<Car?>(null) }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = { Text("My Cars", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer, titleContentColor = OnSurfacePrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editCar = null; showSheet = true },
                containerColor = NeonCyan, contentColor = TrueBlack,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Filled.Add, "Add Car") }
        }
    ) { padding ->
        if (cars.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Garage, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                    Text("No cars yet", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                    Text("Tap + to add your first car", color = OnSurfaceSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                items(cars, key = { it.id }) { car ->
                    CarCard(
                        car = car,
                        isActive = car.id == activeId,
                        onSelect = { carsViewModel.selectCar(car.id) },
                        onEdit = { editCar = car; showSheet = true },
                        onDelete = { deleteTarget = car },
                        onUpdatePhoto = { path -> carsViewModel.updateCarPhoto(car.id, path) }
                    )
                }
            }
        }
    }

    if (showSheet) {
        CarSheet(
            existingCar = editCar,
            onDismiss = { showSheet = false; editCar = null },
            onSave = { name, make, model, year, plate, odo ->
                if (editCar != null) {
                    carsViewModel.updateCar(editCar!!.copy(name = name, make = make, model = model, year = year, plateNumber = plate, currentOdometer = odo))
                } else {
                    carsViewModel.insertCar(Car(name = name, make = make, model = model, year = year, plateNumber = plate, currentOdometer = odo))
                }
                showSheet = false; editCar = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Remove ${target.name}?", color = OnSurfacePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("All fuel, trip, and maintenance records for this car will be deleted.", color = OnSurfaceSecondary) },
            confirmButton = {
                TextButton(onClick = { carsViewModel.deleteCar(target); deleteTarget = null }) {
                    Text("Remove", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = OnSurfaceSecondary) } }
        )
    }
}

// ─── Card ─────────────────────────────────────────────────────────────────────

@Composable
private fun CarCard(
    car: Car, isActive: Boolean,
    onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onUpdatePhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val path = PhotoUtil.copyToInternal(context, it, car.id)
                withContext(Dispatchers.Main) { onUpdatePhoto(path) }
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, if (isActive) NeonCyanBorder else GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) NeonCyanGlow else SurfaceContainerHigh)
                            .clickable {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        Alignment.Center
                    ) {
                        if (car.photoUri.isNotBlank() && File(car.photoUri).exists()) {
                            AsyncImage(
                                model = File(car.photoUri),
                                contentDescription = car.name,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            CarLogoImage(
                                make = car.make,
                                modifier = Modifier.size(34.dp),
                                tint = if (isActive) NeonCyan else Color.White,
                                fallbackTint = if (isActive) NeonCyan else OnSurfaceSecondary
                            )
                        }
                        // Camera overlay hint
                        Box(Modifier.align(Alignment.BottomEnd).size(16.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp))
                            .background(TrueBlack.copy(alpha = 0.6f)), Alignment.Center) {
                            Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(car.name, color = if (isActive) NeonCyan else OnSurfacePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            if (isActive) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(4.dp)).background(NeonCyanGlow)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                        Text("${car.make} ${car.model} · ${car.year}", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelMedium)
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

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (car.plateNumber.isNotBlank()) {
                    StatChip(label = "Plate", value = car.plateNumber)
                }
                if (car.currentOdometer > 0) {
                    StatChip(label = "Odometer", value = String.format(Locale.US, "%,d km", car.currentOdometer.toLong()))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = OnSurfacePrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Bottom Sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarSheet(
    existingCar: Car?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, Double) -> Unit
) {
    val isEdit = existingCar != null

    var name by remember { mutableStateOf(existingCar?.name ?: "") }
    var make by remember { mutableStateOf(existingCar?.make ?: "") }
    var model by remember { mutableStateOf(existingCar?.model ?: "") }
    var year by remember { mutableStateOf(existingCar?.year?.toString() ?: "") }
    var plate by remember { mutableStateOf(existingCar?.plateNumber ?: "") }
    var odometer by remember {
        mutableStateOf(
            if ((existingCar?.currentOdometer ?: 0.0) > 0)
                String.format(Locale.US, "%.0f", existingCar!!.currentOdometer)
            else ""
        )
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
            Text(
                if (isEdit) "Edit Car" else "Add Car",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary
            )

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Car Nickname (e.g. My Camry)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = make, onValueChange = { make = it },
                    label = { Text("Make") }, modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = model, onValueChange = { model = it },
                    label = { Text("Model") }, modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = year, onValueChange = { year = it },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = plate, onValueChange = { plate = it },
                    label = { Text("Plate") }, modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }

            OutlinedTextField(value = odometer, onValueChange = { odometer = it },
                label = { Text("Current Odometer (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Button(
                onClick = {
                    if (name.isBlank() || make.isBlank() || model.isBlank()) return@Button
                    val y = year.toIntOrNull() ?: return@Button
                    val odo = odometer.toDoubleOrNull() ?: 0.0
                    onSave(name, make, model, y, plate, odo)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (isEdit) "Update" else "Add Car", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}
