package com.cartracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cartracker.data.db.entities.Car
import com.cartracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarPickerSheet(
    cars: List<Car>,
    selectedCarId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceContainerHighest)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Switch Car",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfacePrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            cars.forEach { car ->
                val isSelected = car.id == selectedCarId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) NeonCyanGlow else SurfaceContainerHigh)
                        .border(1.dp, if (isSelected) NeonCyanBorder else GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { onSelect(car.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyanGlow else SurfaceContainerHighest),
                            Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DirectionsCar, null,
                                tint = if (isSelected) NeonCyan else OnSurfaceSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                car.name,
                                color = if (isSelected) NeonCyan else OnSurfacePrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${car.make} ${car.model} · ${car.year}",
                                color = OnSurfaceSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (car.currentOdometer > 0) {
                                Text(
                                    "%.0f km".format(car.currentOdometer),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.7f) else OnSurfaceSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Filled.Check, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
