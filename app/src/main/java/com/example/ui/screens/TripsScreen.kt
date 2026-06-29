package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Trip
import com.example.viewmodel.DateFilterRange
import com.example.viewmodel.DriverViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(viewModel: DriverViewModel) {
    val trips by viewModel.filteredTrips.collectAsState()
    val activePlatformFilter by viewModel.filterPlatform.collectAsState()
    val activeDateFilter by viewModel.filterDateRange.collectAsState()

    // Dialog state for edit/delete
    var selectedTripForEdit by remember { mutableStateOf<Trip?>(null) }

    // Summary calculations on the fly
    val totalEarnings = trips.sumOf { it.fare }
    val totalDistance = trips.sumOf { it.totalDistanceKm }
    val totalHours = trips.sumOf { it.durationMinutes } / 60.0
    val avgPerKm = if (totalDistance > 0) totalEarnings / totalDistance else 0.0
    val avgPerHour = if (totalHours > 0) totalEarnings / totalHours else 0.0

    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            val context = LocalContext.current
            TopAppBar(
                title = { Text("Trip History & Logs", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            val csvUri = viewModel.exportToCSV()
                            if (csvUri != null) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "DriverMate Trips Export")
                                    putExtra(Intent.EXTRA_STREAM, csvUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Trip Log CSV")
                                context.startActivity(shareIntent)
                            } else {
                                android.widget.Toast.makeText(context, "No saved trips found to export!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Interactive Filters Layout
            FilterSection(
                activePlatform = activePlatformFilter,
                activeDate = activeDateFilter,
                onPlatformSelect = { viewModel.setFilterPlatform(it) },
                onDateSelect = { viewModel.setFilterDateRange(it) }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2. Summary Dashboard Widget
                item {
                    TripLogsSummaryWidget(
                        totalEarnings = totalEarnings,
                        totalDistance = totalDistance,
                        totalHours = totalHours,
                        avgPerKm = avgPerKm,
                        avgPerHour = avgPerHour
                    )
                }

                item {
                    Text(
                        text = "Recorded Shifts & Trips (${trips.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // 3. Trips List
                if (trips.isEmpty()) {
                    item {
                        EmptyTripsState()
                    }
                } else {
                    items(trips, key = { it.id }) { trip ->
                        TripLogCard(
                            trip = trip,
                            formattedDate = dateFormat.format(Date(trip.timestamp)),
                            onEditClick = { selectedTripForEdit = trip }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Edit/Delete Trip Dialog
    if (selectedTripForEdit != null) {
        val trip = selectedTripForEdit!!
        EditTripDialog(
            trip = trip,
            onDismiss = { selectedTripForEdit = null },
            onSave = { updatedTrip ->
                viewModel.updateTrip(updatedTrip)
                selectedTripForEdit = null
            },
            onDelete = {
                viewModel.deleteTrip(trip)
                selectedTripForEdit = null
            }
        )
    }
}

@Composable
fun FilterSection(
    activePlatform: String?,
    activeDate: DateFilterRange,
    onPlatformSelect: (String?) -> Unit,
    onDateSelect: (DateFilterRange) -> Unit
) {
    val platforms = listOf("All", "Uber", "DiDi", "Ola", "DoorDash", "Other")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date range horizontal slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFilterRange.values().forEach { range ->
                val isSelected = activeDate == range
                FilterChip(
                    selected = isSelected,
                    onClick = { onDateSelect(range) },
                    label = {
                        Text(
                            text = when (range) {
                                DateFilterRange.ALL -> "All Dates"
                                DateFilterRange.TODAY -> "Today"
                                DateFilterRange.WEEK -> "This Week"
                                DateFilterRange.MONTH -> "This Month"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Platform horizontal slider (LazyRow style or simple row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            platforms.take(3).forEach { platform ->
                val platValue = if (platform == "All") null else platform
                val isSelected = activePlatform == platValue
                FilterChip(
                    selected = isSelected,
                    onClick = { onPlatformSelect(platValue) },
                    label = { Text(platform, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            platforms.drop(3).forEach { platform ->
                val platValue = if (platform == "All") null else platform
                val isSelected = activePlatform == platValue
                FilterChip(
                    selected = isSelected,
                    onClick = { onPlatformSelect(platValue) },
                    label = { Text(platform, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun TripLogsSummaryWidget(
    totalEarnings: Double,
    totalDistance: Double,
    totalHours: Double,
    avgPerKm: Double,
    avgPerHour: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SUMMARY FOR FILTERED PERIOD",
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "$%.2f", totalEarnings),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f km · %.1f hrs", totalDistance, totalHours),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "Avg rate per km: $%.2f/km", avgPerKm),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.getDefault(), "Avg rate per hour: $%.2f/hr", avgPerHour),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TripLogCard(
    trip: Trip,
    formattedDate: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(getPlatformColor(trip.platform).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trip.platform.take(2).uppercase(),
                            color = getPlatformColor(trip.platform),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = trip.platform,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "$%.2f", trip.fare),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AUD",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TripDataField(label = "Pickup", value = String.format(Locale.getDefault(), "%.1f km", trip.pickupDistanceKm))
                TripDataField(label = "Drop-off", value = String.format(Locale.getDefault(), "%.1f km", trip.dropoffDistanceKm))
                TripDataField(label = "Duration", value = String.format(Locale.getDefault(), "%.0f mins", trip.durationMinutes))
                TripDataField(label = "Rates", value = String.format(Locale.getDefault(), "$%.2f/km", trip.earningsPerKm))
            }

            if (trip.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = "Notes icon",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = trip.notes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripDataField(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun EmptyTripsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = "No Saved Trips",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No saved trips matched these filters.",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Use the Evaluator tab to calculate and log your rideshare or delivery runs.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripDialog(
    trip: Trip,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit,
    onDelete: () -> Unit
) {
    var platform by remember { mutableStateOf(trip.platform) }
    var pickupStr by remember { mutableStateOf(trip.pickupDistanceKm.toString()) }
    var dropoffStr by remember { mutableStateOf(trip.dropoffDistanceKm.toString()) }
    var fareStr by remember { mutableStateOf(trip.fare.toString()) }
    var durationStr by remember { mutableStateOf(trip.durationMinutes.toString()) }
    var notes by remember { mutableStateOf(trip.notes) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Trip Record") },
            text = { Text("Are you sure you want to permanently delete this trip log? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Trip Log") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = platform,
                        onValueChange = { platform = it },
                        label = { Text("Platform") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pickupStr,
                            onValueChange = { pickupStr = it },
                            label = { Text("Pickup Km") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dropoffStr,
                            onValueChange = { dropoffStr = it },
                            label = { Text("Dropoff Km") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fareStr,
                            onValueChange = { fareStr = it },
                            label = { Text("Fare $") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationStr,
                            onValueChange = { durationStr = it },
                            label = { Text("Duration Mins") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pickup = pickupStr.toDoubleOrNull() ?: trip.pickupDistanceKm
                        val dropoff = dropoffStr.toDoubleOrNull() ?: trip.dropoffDistanceKm
                        val fare = fareStr.toDoubleOrNull() ?: trip.fare
                        val duration = durationStr.toDoubleOrNull() ?: trip.durationMinutes

                        onSave(
                            trip.copy(
                                platform = platform,
                                pickupDistanceKm = pickup,
                                dropoffDistanceKm = dropoff,
                                fare = fare,
                                durationMinutes = duration,
                                notes = notes
                            )
                        )
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Log")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
