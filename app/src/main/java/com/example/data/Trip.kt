package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long, // Saved timestamp in ms
    val platform: String, // Uber, DiDi, Ola, DoorDash, Other
    val pickupDistanceKm: Double,
    val dropoffDistanceKm: Double,
    val fare: Double,
    val durationMinutes: Double,
    val notes: String = ""
) {
    val totalDistanceKm: Double
        get() = pickupDistanceKm + dropoffDistanceKm

    val earningsPerKm: Double
        get() = if (totalDistanceKm > 0) fare / totalDistanceKm else 0.0

    val earningsPerHour: Double
        get() = if (durationMinutes > 0) (fare / durationMinutes) * 60.0 else 0.0
}
