package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.DriverViewModel
import com.example.viewmodel.EvaluationVerdict
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: DriverViewModel,
    onTripSaved: () -> Unit
) {
    val rules by viewModel.currentRules.collectAsState()

    var pickupStr by remember { mutableStateOf("") }
    var dropoffStr by remember { mutableStateOf("") }
    var fareStr by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Uber") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val platforms = listOf("Uber", "DiDi", "Ola", "DoorDash", "Other")

    // Values conversions
    val pickup = pickupStr.toDoubleOrNull() ?: 0.0
    val dropoff = dropoffStr.toDoubleOrNull() ?: 0.0
    val fare = fareStr.toDoubleOrNull() ?: 0.0
    val duration = durationStr.toDoubleOrNull() ?: 0.0

    // Evaluate live
    val evaluation = viewModel.evaluateTrip(pickup, dropoff, fare, duration, rules)

    // Save dialog state
    var showSaveDialog by remember { mutableStateOf(false) }
    var tripNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Evaluator", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            pickupStr = ""
                            dropoffStr = ""
                            fareStr = ""
                            durationStr = ""
                            selectedPlatform = "Uber"
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Reset Fields")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Result Verdict Banner
            item {
                VerdictBanner(
                    verdict = evaluation.verdict,
                    ratePerKm = evaluation.earningsPerKm,
                    ratePerHour = evaluation.earningsPerHour,
                    totalDistance = pickup + dropoff
                )
            }

            // 2. Input Fields Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Trip Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Platform Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedPlatform,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Service Platform") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded = true }
                                    .testTag("platform_dropdown"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                platforms.forEach { platform ->
                                    DropdownMenuItem(
                                        text = { Text(platform) },
                                        onClick = {
                                            selectedPlatform = platform
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Distances fields
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = pickupStr,
                                onValueChange = { pickupStr = it },
                                label = { Text("Pickup Dist.") },
                                suffix = { Text("km") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pickup_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            OutlinedTextField(
                                value = dropoffStr,
                                onValueChange = { dropoffStr = it },
                                label = { Text("Drop-off Dist.") },
                                suffix = { Text("km") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dropoff_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Fare & Time fields
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = fareStr,
                                onValueChange = { fareStr = it },
                                label = { Text("Est. Fare") },
                                prefix = { Text("$") },
                                suffix = { Text("AUD") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("fare_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            OutlinedTextField(
                                value = durationStr,
                                onValueChange = { durationStr = it },
                                label = { Text("Est. Time") },
                                suffix = { Text("mins") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("duration_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // 3. Rule Checks Breakdown List
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Rule Compliance Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        RuleCheckItem(
                            ruleName = "Earnings Rate per Km",
                            ruleThreshold = String.format(Locale.getDefault(), "Min $%.2f/km", rules.minEarningsPerKm),
                            actualValue = String.format(Locale.getDefault(), "$%.2f/km", evaluation.earningsPerKm),
                            passed = evaluation.passKmRate
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                        RuleCheckItem(
                            ruleName = "Estimated Hourly Rate",
                            ruleThreshold = String.format(Locale.getDefault(), "Min $%.2f/hr", rules.minEarningsPerHour),
                            actualValue = String.format(Locale.getDefault(), "$%.2f/hr", evaluation.earningsPerHour),
                            passed = evaluation.passHourRate
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                        RuleCheckItem(
                            ruleName = "Minimum Trip Fare",
                            ruleThreshold = String.format(Locale.getDefault(), "Min $%.2f", rules.minFare),
                            actualValue = String.format(Locale.getDefault(), "$%.2f", fare),
                            passed = evaluation.passFare
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                        RuleCheckItem(
                            ruleName = "Maximum Pickup Distance",
                            ruleThreshold = String.format(Locale.getDefault(), "Max %.1f km", rules.maxPickupDistance),
                            actualValue = String.format(Locale.getDefault(), "%.1f km", pickup),
                            passed = evaluation.passPickup
                        )
                    }
                }
            }

            // 4. Action Logging Button
            item {
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_trip_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = fare > 0 && (pickup + dropoff) > 0 && duration > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Log Trip")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Completed Trips", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Save Trip Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Log Trip Summary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ready to log this trip to your local history?", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Platform: $selectedPlatform", fontWeight = FontWeight.Bold)
                    Text("Total Distance: ${String.format(Locale.getDefault(), "%.1f km", pickup + dropoff)}")
                    Text("Fare Earned: ${String.format(Locale.getDefault(), "$%.2f", fare)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tripNotes,
                        onValueChange = { tripNotes = it },
                        label = { Text("Add notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTrip(
                            platform = selectedPlatform,
                            pickupDistance = pickup,
                            dropoffDistance = dropoff,
                            fare = fare,
                            durationMinutes = duration,
                            notes = tripNotes
                        )
                        showSaveDialog = false
                        tripNotes = ""
                        // Trigger callback navigation or reset
                        onTripSaved()
                    }
                ) {
                    Text("Save Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VerdictBanner(
    verdict: EvaluationVerdict,
    ratePerKm: Double,
    ratePerHour: Double,
    totalDistance: Double
) {
    val (backgroundColor, textColor, title, icon) = when (verdict) {
        EvaluationVerdict.GOOD -> Quadruple(
            Color(0xFF10B981).copy(alpha = 0.12f),
            Color(0xFF10B981),
            "GOOD TRIP",
            Icons.Default.CheckCircle
        )
        EvaluationVerdict.OKAY -> Quadruple(
            Color(0xFFF59E0B).copy(alpha = 0.12f),
            Color(0xFFF59E0B),
            "OKAY TRIP",
            Icons.Default.Warning
        )
        EvaluationVerdict.REJECT -> Quadruple(
            Color(0xFFEF4444).copy(alpha = 0.12f),
            Color(0xFFEF4444),
            "REJECT TRIP",
            Icons.Default.Cancel
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (totalDistance > 0) {
                    Text(
                        text = String.format(Locale.getDefault(), "$%.2f/km  ·  $%.2f/hr", ratePerKm, ratePerHour),
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Enter trip parameters below to evaluate",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RuleCheckItem(
    ruleName: String,
    ruleThreshold: String,
    actualValue: String,
    passed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = ruleName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "Rule: $ruleThreshold", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = actualValue,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (passed) Color(0xFF10B981) else Color(0xFFEF4444)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (passed) "Passed" else "Failed",
                tint = if (passed) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Custom structure for simple grouping
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
