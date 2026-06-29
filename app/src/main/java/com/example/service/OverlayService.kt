package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.Locale
import com.example.data.DriverRules
import com.example.data.SettingsManager
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.roundToInt

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null

    // Lifecycle, SavedState, and ViewModelStore implementations for ComposeView compatibility in Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        val context = this
        val settingsManager = SettingsManager(context)
        val rules = settingsManager.getRules()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        overlayView = FrameLayout(context).apply {
            // Setup owners for Jetpack ComposeView support
            setViewTreeLifecycleOwner(context)
            setViewTreeSavedStateRegistryOwner(context)
            setViewTreeViewModelStoreOwner(context)
        }

        val composeView = ComposeView(context).apply {
            setContent {
                MyApplicationTheme(darkTheme = true) {
                    FloatingOverlayContent(
                        rules = rules,
                        onPositionUpdate = { dx, dy ->
                            params.x = (params.x + dx).coerceAtLeast(0)
                            params.y = (params.y + dy).coerceAtLeast(0)
                            overlayView?.let { windowManager.updateViewLayout(it, params) }
                        },
                        onToggleFocus = { needsFocus ->
                            if (needsFocus) {
                                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                            } else {
                                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            }
                            overlayView?.let { windowManager.updateViewLayout(it, params) }
                        },
                        onCloseService = {
                            stopSelf()
                        }
                    )
                }
            }
        }

        overlayView?.addView(composeView)
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        overlayView?.let {
            windowManager.removeView(it)
        }
        super.onDestroy()
    }
}

@Composable
fun FloatingOverlayContent(
    rules: DriverRules,
    onPositionUpdate: (Int, Int) -> Unit,
    onToggleFocus: (Boolean) -> Unit,
    onCloseService: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Overlay position delta detection via detection drag gesture
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            onPositionUpdate(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
        }
    }

    if (!isExpanded) {
        // Compact Floating Bubble
        Box(
            modifier = dragModifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)) // Deep dark slate
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPositionUpdate(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { isExpanded = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Open Helper",
                    tint = Color(0xFF38BDF8), // Radiant sky blue
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    } else {
        // Expanded Panel (Mini Calculator)
        // Request focus so driver can input numbers
        LaunchedEffect(Unit) {
            onToggleFocus(true)
        }

        DisposableEffect(Unit) {
            onDispose {
                onToggleFocus(false)
            }
        }

        Card(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A) // Dark slate
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                // Header (Draggable part)
                Row(
                    modifier = dragModifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Car",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DriverMate Assistant",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Minimize,
                                contentDescription = "Minimize",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onCloseService,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calculator fields
                var pickupStr by remember { mutableStateOf("") }
                var dropoffStr by remember { mutableStateOf("") }
                var fareStr by remember { mutableStateOf("") }
                var timeStr by remember { mutableStateOf("") }

                val pickup = pickupStr.toDoubleOrNull() ?: 0.0
                val dropoff = dropoffStr.toDoubleOrNull() ?: 0.0
                val fare = fareStr.toDoubleOrNull() ?: 0.0
                val minutes = timeStr.toDoubleOrNull() ?: 0.0

                val totalDist = pickup + dropoff
                val ratePerKm = if (totalDist > 0) fare / totalDist else 0.0
                val hourlyRate = if (minutes > 0) (fare / minutes) * 60.0 else 0.0

                // Rule Evaluations
                val passKmRate = ratePerKm >= rules.minEarningsPerKm
                val passFare = fare >= rules.minFare
                val passPickup = pickup <= rules.maxPickupDistance
                val passHourRate = hourlyRate >= rules.minEarningsPerHour

                val totalPassed = (if (passKmRate) 1 else 0) +
                        (if (passFare) 1 else 0) +
                        (if (passPickup) 1 else 0) +
                        (if (passHourRate) 1 else 0)

                val verdict = when {
                    !passKmRate || totalPassed <= 1 -> "REJECT"
                    totalPassed == 4 -> "GOOD"
                    else -> "OKAY"
                }

                val verdictColor = when (verdict) {
                    "GOOD" -> Color(0xFF10B981) // Emerald
                    "OKAY" -> Color(0xFFF59E0B) // Amber
                    else -> Color(0xFFEF4444) // Rose
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pickupStr,
                        onValueChange = { pickupStr = it },
                        label = { Text("Pickup Km", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color.LightGray,
                            focusedLabelColor = Color(0xFF38BDF8)
                        )
                    )
                    OutlinedTextField(
                        value = dropoffStr,
                        onValueChange = { dropoffStr = it },
                        label = { Text("Dropoff Km", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color.LightGray,
                            focusedLabelColor = Color(0xFF38BDF8)
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fareStr,
                        onValueChange = { fareStr = it },
                        label = { Text("Fare $", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color.LightGray,
                            focusedLabelColor = Color(0xFF38BDF8)
                        )
                    )
                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = { Text("Duration Mins", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color.LightGray,
                            focusedLabelColor = Color(0xFF38BDF8)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live calculated stats
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rate/Km:", color = Color.LightGray, fontSize = 11.sp)
                            Text(
                                text = String.format(Locale.getDefault(), "$%.2f", ratePerKm),
                                color = if (passKmRate) Color.Green else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rate/Hour:", color = Color.LightGray, fontSize = 11.sp)
                            Text(
                                text = String.format(Locale.getDefault(), "$%.2f/hr", hourlyRate),
                                color = if (passHourRate) Color.Green else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Distance:", color = Color.LightGray, fontSize = 11.sp)
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f km", totalDist),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Verdict
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(verdictColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Verdict: $verdict",
                        color = verdictColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
