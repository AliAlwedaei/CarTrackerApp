package com.cartracker.ui.screens.cars

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cartracker.data.db.entities.Car
import com.cartracker.ui.viewmodel.CarsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarsScreen(carsViewModel: CarsViewModel) {
    val cars by carsViewModel.cars.observeAsState(emptyList())
    val selectedCarId by carsViewModel.selectedCarId.observeAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editCar by remember { mutableStateOf<Car?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Cars") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Car")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (cars.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null,
                                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No cars yet", style = MaterialTheme.typography.titleMedium)
                            Text("Tap + to add your first car", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(cars) { car ->
                CarCard(
                    car = car,
                    isSelected = car.id == (selectedCarId ?: cars.firstOrNull()?.id),
                    onSelect = { carsViewModel.selectCar(car.id) },
                    onEdit = { editCar = car },
                    onDelete = { carsViewModel.deleteCar(car) }
                )
            }
        }
    }

    if (showAddDialog) {
        CarDialog(
            car = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, make, model, year, plate, odometer ->
                carsViewModel.insertCar(Car(name = name, make = make, model = model,
                    year = year, plateNumber = plate, currentOdometer = odometer))
                showAddDialog = false
            }
        )
    }

    editCar?.let { car ->
        CarDialog(
            car = car,
            onDismiss = { editCar = null },
            onConfirm = { name, make, model, year, plate, odometer ->
                carsViewModel.updateCar(car.copy(name = name, make = make, model = model,
                    year = year, plateNumber = plate, currentOdometer = odometer))
                editCar = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarCard(
    car: Car,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = if (isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(car.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isSelected) Badge { Text("Active") }
                    }
                    Text("${car.make} ${car.model} · ${car.year}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (car.plateNumber.isNotBlank()) {
                            Text("Plate: ${car.plateNumber}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (car.currentOdometer > 0) {
                            Text("%.0f km".format(car.currentOdometer), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarDialog(
    car: Car?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(car?.name ?: "") }
    var make by remember { mutableStateOf(car?.make ?: "") }
    var model by remember { mutableStateOf(car?.model ?: "") }
    var year by remember { mutableStateOf(car?.year?.toString() ?: "") }
    var plate by remember { mutableStateOf(car?.plateNumber ?: "") }
    var odometer by remember { mutableStateOf(if ((car?.currentOdometer ?: 0.0) > 0) car?.currentOdometer?.toString() ?: "" else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (car == null) "Add Car" else "Edit Car") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Car Name (e.g. My Toyota)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = make, onValueChange = { make = it },
                    label = { Text("Make (e.g. Toyota)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it },
                    label = { Text("Model (e.g. Corolla)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = year, onValueChange = { year = it },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = plate, onValueChange = { plate = it },
                    label = { Text("Plate Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = odometer, onValueChange = { odometer = it },
                    label = { Text("Current Odometer (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || make.isBlank() || model.isBlank()) return@TextButton
                val y = year.toIntOrNull() ?: return@TextButton
                val odo = odometer.toDoubleOrNull() ?: 0.0
                onConfirm(name, make, model, y, plate, odo)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
