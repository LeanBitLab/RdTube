package com.lean.reddittube.util

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Adaptive Performance Telemetry & Controller
 * Implements the closed-loop control spec from Plan.md:
 * Audits JVM Heap, Native Heap, and Thermal Status to dynamically adjust:
 * - alpha (cache memory fraction)
 * - D (prefetch depth)
 * - B_play (playback start buffer ms)
 * - B_max (max player buffer ms)
 * - k_net (network concurrency)
 */
data class PerfParams(
    val alpha: Float = 0.15f,
    val prefetchDepth: Int = 2,
    val bufferPlayMs: Int = 300,
    val bufferRebufferMs: Int = 800,
    val bufferMaxMs: Int = 12000,
    val networkConcurrency: Int = 3,
    val imageQuality: Float = 1.0f,
    val oomRisk: Float = 0.0f,
    val thermalStatus: Float = 0.0f,
    val memPressure: Float = 0.0f,
    val nativePressure: Float = 0.0f
)

class PerfTelemetryController private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _params = MutableStateFlow(PerfParams())
    val params: StateFlow<PerfParams> = _params.asStateFlow()

    private var videoStallCount = 0
    private var videoStallDurationMs = 0L
    private var totalWatchDurationMs = 1L

    init {
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        scope.launch {
            while (isActive) {
                try {
                    updateTelemetry()
                } catch (e: Exception) {
                    Log.w("PerfTelemetry", "Error updating telemetry: ${e.message}")
                }
                delay(2000) // Audit every 2 seconds
            }
        }
    }

    private fun sigmoid(x: Float): Float {
        return (1.0f / (1.0f + exp(-x))).toFloat()
    }

    private fun updateTelemetry() {
        val runtime = Runtime.getRuntime()
        val totalHeap = runtime.totalMemory()
        val freeHeap = runtime.freeMemory()
        val maxHeap = runtime.maxMemory()
        val usedHeap = totalHeap - freeHeap
        val memPressure = (usedHeap.toDouble() / maxHeap.toDouble()).toFloat().coerceIn(0f, 1f)

        val nativeAllocated = Debug.getNativeHeapAllocatedSize()
        val estimatedNativeCeiling = 384L * 1024L * 1024L // 384 MB baseline safe ceiling
        val nativePressure = (nativeAllocated.toDouble() / estimatedNativeCeiling.toDouble()).toFloat().coerceIn(0f, 1f)

        var thermalStatus = 0.0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val status = pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            thermalStatus = (status.toFloat() / PowerManager.THERMAL_STATUS_SHUTDOWN.toFloat()).coerceIn(0f, 1f)
        }

        // OOM Risk Equation: sigmoid(10 * (mem_pressure - 0.82)) + 0.5 * sigmoid(10 * (native_pressure - 0.80))
        val oomRisk = (
            sigmoid(10f * (memPressure - 0.82f)) +
            0.5f * sigmoid(10f * (nativePressure - 0.80f))
        ).coerceIn(0f, 1f)

        // Adaptive alpha (cache memory fraction): 0.15 * (1 - 0.35 * mem_pressure) * (1 - 0.25 * thermal)
        val alpha = (0.15f * (1.0f - 0.35f * memPressure) * (1.0f - 0.25f * thermalStatus)).coerceIn(0.08f, 0.20f)

        // Adaptive Prefetch Depth D: floor(2 * (1 - mem_pressure) * (1 - thermal))
        val D = if (oomRisk > 0.35f || memPressure > 0.85f) 0 else {
            floor(2.0f * (1.0f - memPressure) * (1.0f - thermalStatus)).toInt().coerceIn(0, 4)
        }

        val videoStallRatio = (videoStallDurationMs.toDouble() / totalWatchDurationMs.toDouble()).toFloat().coerceIn(0f, 1f)

        // B_play: 300ms baseline + 150ms * video_stall
        val B_play = (300f + 150f * videoStallRatio).toInt().coerceIn(250, 900)

        // B_rebuffer: 800ms
        val B_rebuffer = 800

        // B_max: 12000ms * (1 + video_stall)
        val B_max = (12000f * (1.0f + videoStallRatio)).toInt().coerceIn(8000, 20000)

        // k_net: network concurrency
        val k_net = if (oomRisk > 0.35f) 1 else floor(3.0f * (1.0f - memPressure) * (1.0f - thermalStatus)).toInt().coerceIn(1, 4)

        val q_img = (1.0f - 0.35f * memPressure - 0.30f * thermalStatus).coerceIn(0.45f, 1.0f)

        val updated = PerfParams(
            alpha = alpha,
            prefetchDepth = D,
            bufferPlayMs = B_play,
            bufferRebufferMs = B_rebuffer,
            bufferMaxMs = B_max,
            networkConcurrency = k_net,
            imageQuality = q_img,
            oomRisk = oomRisk,
            thermalStatus = thermalStatus,
            memPressure = memPressure,
            nativePressure = nativePressure
        )

        _params.value = updated
        Log.d("PerfTelemetry", "Telemetry update: mem=${(memPressure*100).toInt()}%, native=${(nativePressure*100).toInt()}%, oomRisk=${String.format("%.2f", oomRisk)}, alpha=${String.format("%.2f", alpha)}, B_play=${B_play}ms")
    }

    fun recordStall(durationMs: Long) {
        videoStallCount++
        videoStallDurationMs += durationMs
    }

    fun recordWatchTime(durationMs: Long) {
        totalWatchDurationMs += durationMs
    }

    companion object {
        @Volatile
        private var instance: PerfTelemetryController? = null

        fun getInstance(context: Context): PerfTelemetryController {
            return instance ?: synchronized(this) {
                instance ?: PerfTelemetryController(context).also { instance = it }
            }
        }
    }
}
