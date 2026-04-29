package com.example.moves.domain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * Wraps the hardware step counter (Sensor.TYPE_STEP_COUNTER), which reports a
 * monotonically-increasing total since boot. We sample twice with a delay
 * between to compute deltas.
 */
class MovementDetector(private val context: Context) {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun isAvailable(): Boolean = stepSensor != null

    /**
     * Sample the cumulative step counter, wait `windowMillis`, sample again,
     * return the delta. Returns 0 if the sensor is unavailable or no event arrives.
     */
    suspend fun stepsInWindow(windowMillis: Long): Int {
        val start = readOnce() ?: return 0
        delay(windowMillis)
        val end = readOnce() ?: return 0
        return max(0, (end - start).toInt())
    }

    /** Stream cumulative step count changes. Used by AlertActivity to track move-goal progress. */
    fun stepStream(): Flow<Long> = callbackFlow {
        val sm = sensorManager ?: run { close(); return@callbackFlow }
        val sensor = stepSensor ?: run { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sm.unregisterListener(listener) }
    }

    private suspend fun readOnce(timeoutMillis: Long = 3_000L): Long? {
        val sm = sensorManager ?: return null
        val sensor = stepSensor ?: return null
        return suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sm.unregisterListener(this)
                    if (cont.isActive) cont.resume(event.values[0].toLong())
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            cont.invokeOnCancellation { sm.unregisterListener(listener) }
            // Best-effort timeout via a simple Handler
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                sm.unregisterListener(listener)
                if (cont.isActive) cont.resume(null)
            }, timeoutMillis)
        }
    }
}
