package com.code4galaxy.leakcanarydemo

import android.util.Log

/**
 * Interface for sensor event updates.
 * When implemented by an Activity or Fragment, registering this listener on a Singleton
 * without unregistering upon lifecycle destruction causes a severe memory leak!
 */
interface SensorEventListener {
    fun onSensorDataChanged(value: String)
}

/**
 * Singleton object simulating a system or application-wide service (e.g. LocationManager,
 * NetworkStateMonitor, Custom Sensor SDK).
 *
 * 🚨 MEMORY LEAK CAUSE:
 * Since singletons live for the entire lifetime of the process/application, any strong reference
 * stored inside [listeners] will prevent GC from reclaiming the subscriber object (e.g., Activity).
 */
object SensorManagerSingleton {
    private const val TAG = "SensorManagerSingleton"
    private val listeners = mutableListOf<SensorEventListener>()

    /**
     * Registers a listener.
     */
    fun registerListener(listener: SensorEventListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            Log.d(TAG, "Listener registered: $listener. Total listeners: ${listeners.size}")
        }
    }

    /**
     * Unregisters a listener.
     *
     * ✅ FIX: Must be explicitly called in onDestroy() / onStop() to remove the Activity
     * reference from the singleton's list!
     */
    fun unregisterListener(listener: SensorEventListener) {
        if (listeners.remove(listener)) {
            Log.d(TAG, "Listener unregistered: $listener. Total listeners: ${listeners.size}")
        }
    }

    /**
     * Helper method to simulate emitting sensor events to all subscribers.
     */
    fun emitMockData(data: String) {
        Log.d(TAG, "Emitting mock sensor data to ${listeners.size} listeners")
        listeners.forEach { listener ->
            listener.onSensorDataChanged(data)
        }
    }

    /**
     * Returns current count of registered listeners.
     */
    fun getListenerCount(): Int = listeners.size
}
