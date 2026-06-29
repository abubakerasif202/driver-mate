package com.example.data

import kotlinx.coroutines.flow.Flow

class TripRepository(private val tripDao: TripDao) {
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()

    fun getTripsInDateRange(startTime: Long, endTime: Long): Flow<List<Trip>> {
        return tripDao.getTripsInDateRange(startTime, endTime)
    }

    suspend fun insert(trip: Trip) {
        tripDao.insertTrip(trip)
    }

    suspend fun update(trip: Trip) {
        tripDao.updateTrip(trip)
    }

    suspend fun delete(trip: Trip) {
        tripDao.deleteTrip(trip)
    }

    suspend fun deleteById(id: Int) {
        tripDao.deleteTripById(id)
    }

    suspend fun clearAll() {
        tripDao.clearAllTrips()
    }
}
