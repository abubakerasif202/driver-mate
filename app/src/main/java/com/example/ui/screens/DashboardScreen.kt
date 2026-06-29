package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Trip
import com.example.ui.theme.*
import com.example.viewmodel.DashboardStats
import com.example.viewmodel.DriverViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DriverViewModel,
    onNavigateToCalculator: () -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val todayTrips by viewModel.todayTrips.collectAsState()
    val allTrips by viewModel.allTrips.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "DriverMate Logo",
                            tint = TrueGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DriverMate",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = PureWhite
                        )
                    }
                },
                actions = {
                    // Quick stats pill
                    Surface(
                        shape = CircleShape,
                        color = if (isOnline) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isOnline) TrueGold.copy(alpha = 0.4f) else Color.Transparent),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "Active Shift" else "Offline",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            // 1. Shift Controller Card
            item {
                ShiftStatusCard(
                    isOnline = isOnline,
                    onlineSeconds = stats.onlineTimeSeconds,
                    onToggleOnline = { viewModel.toggleOnline() },
                    onAdjustTime = { delta -> viewModel.adjustOnlineTime(delta) },
                    onResetTime = { viewModel.resetOnlineTime() }
                )
            }

            // 2. Today's Giant Earnings Card with integrated CSV Export
            item {
                TodayEarningsCard(
                    earnings = stats.todayEarnings,
                    tripCount = stats.tripCount,
                    totalDistance = stats.totalDistance,
                    onExportCSV = {
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
                            Toast.makeText(context, "No saved trips found to export!", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // 3. Premium Canvas Weekly Earnings Chart
            item {
                WeeklyEarningsChart(trips = allTrips)
            }

            // 4. Performance Metrics Grid (Averages)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Rate / Hour",
                        value = String.format(Locale.getDefault(), "$%.2f", stats.avgEarningsPerHour),
                        unit = "per hr",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconColor = TrueGold,
                        modifier = Modifier.weight(1.5f)
                    )
                    MetricCard(
                        title = "Rate / Km",
                        value = String.format(Locale.getDefault(), "$%.2f", stats.avgEarningsPerKm),
                        unit = "per km",
                        icon = Icons.Default.AltRoute,
                        iconColor = TrueGold,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }

            // 5. Secondary stats (Active Driving vs Shift ratio)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, PremiumCardBorder),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Shift Productivity Metrics",
                            style = MaterialTheme.typography.titleSmall,
                            color = MutedGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Active Fare Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f hrs", stats.activeDrivingMinutes / 60.0),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Distance Run", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f km", stats.totalDistance),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 6. Recent Today Trips header/action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Log (${todayTrips.size} trips)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    TextButton(onClick = onNavigateToCalculator) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = "Add Trip", modifier = Modifier.size(16.dp), tint = TrueGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Trip", color = TrueGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (todayTrips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No trips logged today. Tap 'New Trip' to calculate and log a trip!",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(todayTrips.take(3)) { trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PremiumCardBorder.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(getPlatformColor(trip.platform).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = trip.platform.take(2).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        color = getPlatformColor(trip.platform),
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = trip.platform,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f km · %.0f mins", trip.totalDistanceKm, trip.durationMinutes),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.getDefault(), "$%.2f", trip.fare),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = TrueGold
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "$%.2f/km", trip.earningsPerKm),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ShiftStatusCard(
    isOnline: Boolean,
    onlineSeconds: Long,
    onToggleOnline: () -> Unit,
    onAdjustTime: (Long) -> Unit,
    onResetTime: () -> Unit
) {
    val hrs = onlineSeconds / 3600
    val mins = (onlineSeconds % 3600) / 60
    val secs = onlineSeconds % 60
    val timerString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isOnline) TrueGold.copy(alpha = 0.5f) else PremiumCardBorder),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) PremiumDarkSurface else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shift Time Tracker",
                    fontSize = 14.sp,
                    color = MutedGold,
                    fontWeight = FontWeight.Bold
                )
                if (onlineSeconds > 0) {
                    IconButton(
                        onClick = onResetTime,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Shift Time",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = timerString,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = if (isOnline) TrueGold else PureWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isOnline && onlineSeconds > 0) {
                    OutlinedButton(
                        onClick = { onAdjustTime(-300) }, // Subtract 5 mins
                        shape = CircleShape,
                        border = BorderStroke(1.dp, PremiumCardBorder),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("-5m", color = MutedGold)
                    }
                }

                Button(
                    onClick = onToggleOnline,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnline) MaterialTheme.colorScheme.error else TrueGold,
                        contentColor = if (isOnline) PureWhite else PremiumBlack
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("online_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isOnline) "Pause Shift" else "Start Shift"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOnline) "Go Offline" else "Go Online",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isOnline && onlineSeconds > 0) {
                    OutlinedButton(
                        onClick = { onAdjustTime(300) }, // Add 5 mins
                        shape = CircleShape,
                        border = BorderStroke(1.dp, PremiumCardBorder),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("+5m", color = MutedGold)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayEarningsCard(
    earnings: Double,
    tripCount: Int,
    totalDistance: Double,
    onExportCSV: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, TrueGold.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = PremiumDarkSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TrueGold.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(24.dp)) // Spacer to align text center
                    Text(
                        text = "TODAY'S TOTAL EARNINGS",
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = onExportCSV,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TrueGold.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = TrueGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = String.format(Locale.getDefault(), "$%.2f", earnings),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = TrueGold
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = PremiumCardBorder, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TRIPS COMPLETE",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "$tripCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOTAL DISTANCE",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f km", totalDistance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onExportCSV,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TrueGold.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TrueGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "CSV", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Tax & Income Logs (CSV)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun WeeklyEarningsChart(trips: List<Trip>) {
    val today = LocalDate.now()
    val last7Days = remember { (0..6).map { today.minusDays(it.toLong()) }.reversed() }
    val formatter = remember { DateTimeFormatter.ofPattern("E", Locale.getDefault()) }
    
    // Calculate daily earnings
    val tripsByDate = remember(trips) {
        trips.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    
    val dailyEarnings = remember(tripsByDate) {
        last7Days.map { date ->
            tripsByDate[date]?.sumOf { it.fare } ?: 0.0
        }
    }
    
    val labels = remember { last7Days.map { it.format(formatter) } }
    val maxEarnings = remember(dailyEarnings) { (dailyEarnings.maxOrNull() ?: 0.0).coerceAtLeast(50.0) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PremiumCardBorder),
        colors = CardDefaults.cardColors(
            containerColor = PremiumDarkSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EARNINGS TREND",
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last 7 Days (AUD)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite
                    )
                }
                
                val totalWeekly = remember(dailyEarnings) { dailyEarnings.sum() }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Weekly Total",
                        fontSize = 10.sp,
                        color = MutedGold
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "$%.2f", totalWeekly),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrueGold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val gridColor = Color.White.copy(alpha = 0.05f)
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val width = size.width
                val height = size.height
                
                // Draw horizontal grid lines
                val numGridLines = 4
                for (i in 0 until numGridLines) {
                    val y = height * (i.toFloat() / (numGridLines - 1)) * 0.85f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Draw bars
                val paddingPercent = 0.35f
                val numBars = dailyEarnings.size
                val barWidth = (width / numBars) * (1f - paddingPercent)
                val barSpacing = (width / numBars) * paddingPercent
                
                for (i in dailyEarnings.indices) {
                    val earnings = dailyEarnings[i]
                    val pct = (earnings / maxEarnings).toFloat()
                    
                    val chartHeight = height * 0.85f
                    val barHeight = chartHeight * pct
                    
                    val x = i * (barWidth + barSpacing) + (barSpacing / 2)
                    val y = chartHeight - barHeight
                    
                    // Draw baseline background bar track (premium look)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.02f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    
                    if (barHeight > 0) {
                        // Draw actual filled gold bar gradient
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(GoldGradientStart, GoldGradientEnd)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        
                        // Draw dot on top of active bars
                        drawCircle(
                            color = TrueGold,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x + barWidth / 2, y)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in labels.indices) {
                    val earnings = dailyEarnings[i]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = labels[i],
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (earnings > 0) TrueGold else MutedGold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (earnings > 0) String.format(Locale.getDefault(), "$%.0f", earnings) else "-",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (earnings > 0) PureWhite else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PremiumCardBorder),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MutedGold,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

// Global Helper to display Platform Brand Colors nicely
fun getPlatformColor(platform: String): Color {
    return when (platform.lowercase()) {
        "uber" -> Color(0xFFE5E5E5)
        "didi" -> Color(0xFFFC5B13)
        "ola" -> Color(0xFFC0EB1B)
        "doordash" -> Color(0xFFFF3008)
        else -> Color(0xFF8A94A6)
    }
}
