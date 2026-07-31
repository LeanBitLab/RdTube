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
    private var stableTicksCount = 0
    private val startupTimes = java.util.concurrent.CopyOnWriteArrayList<Long>()

    fun recordStartupTime(durationMs: Long) {
        startupTimes.add(durationMs)
        Log.i("PerfTelemetry", "Video startup latency: ${durationMs}ms (P95: ${getStartupP95()}ms)")
    }

    fun getStartupP95(): Long {
        if (startupTimes.isEmpty()) return 0L
        val sorted = startupTimes.sorted()
        val index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
        return sorted[index]
    }

    init {
        startTelemetryLoop()
    }

    @Volatile
    var isPaused: Boolean = false

    private fun startTelemetryLoop() {
        scope.launch {
            while (isActive) {
                if (!isPaused) {
                    try {
                        updateTelemetry()
                    } catch (e: Exception) {
                        Log.w("PerfTelemetry", "Error updating telemetry: ${e.message}")
                    }
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

        // Native memory budget dynamically derived from total system RAM
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).toInt()
        val isLowRam = am?.isLowRamDevice == true
        val nativeBudgetMb = when {
            isLowRam || totalRamMb < 3000 -> 256L
            totalRamMb <= 6000 -> 384L
            else -> 512L
        }
        val nativeAllocated = Debug.getNativeHeapAllocatedSize()
        val estimatedNativeCeiling = nativeBudgetMb * 1024L * 1024L
        val nativePressure = (nativeAllocated.toDouble() / estimatedNativeCeiling.toDouble()).toFloat().coerceIn(0f, 1f)

        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        var thermalStatus = 0.0f
        if (Build.VERSION.SDK_INT >= 29 && pm != null) {
            thermalStatus = when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> 0.0f
                PowerManager.THERMAL_STATUS_LIGHT -> 0.25f
                PowerManager.THERMAL_STATUS_MODERATE -> 0.50f
                PowerManager.THERMAL_STATUS_SEVERE -> 0.75f
                PowerManager.THERMAL_STATUS_CRITICAL -> 0.90f
                PowerManager.THERMAL_STATUS_EMERGENCY -> 1.0f
                else -> 0.0f
            }
        }

        val isPowerSave = pm?.isPowerSaveMode == true

        // Metered network awareness
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val isMetered = cm?.isActiveNetworkMetered == true

        // OOM Risk Equation strictly clamped 0.0..1.0
        val oomRisk = (
            sigmoid(10f * (memPressure - 0.82f)) +
            0.5f * sigmoid(10f * (nativePressure - 0.80f))
        ).coerceIn(0.0f, 1.0f)

        // Hysteresis adaptation: fast drop on risk, slow recovery (require 5 consecutive stable ticks)
        val isConstrained = oomRisk > 0.35f || memPressure > 0.85f || thermalStatus >= 0.75f || isPowerSave || isMetered
        if (isConstrained) {
            stableTicksCount = 0
        } else {
            stableTicksCount++
        }

        // Adaptive alpha (cache memory fraction)
        val alpha = (0.15f * (1.0f - 0.35f * memPressure) * (1.0f - 0.25f * thermalStatus)).coerceIn(0.08f, 0.20f)

        // Adaptive Prefetch Depth D
        var D = if (isConstrained) 0 else {
            floor(2.0f * (1.0f - memPressure) * (1.0f - thermalStatus)).toInt().coerceIn(0, 4)
        }
        if (!isConstrained && stableTicksCount < 5) {
            D = min(D, 2) // Slow recovery during initial stabilization phase
        }

        val videoStallRatio = (videoStallDurationMs.toDouble() / totalWatchDurationMs.toDouble()).toFloat().coerceIn(0f, 1f)

        val B_play = if (thermalStatus >= 0.75f || isPowerSave) 250 else (300f + 150f * videoStallRatio).toInt().coerceIn(250, 700)
        val B_rebuffer = if (thermalStatus >= 0.75f || isPowerSave) 600 else 800
        val B_max = if (thermalStatus >= 0.75f || isPowerSave) 8000 else (12000f * (1.0f + videoStallRatio)).toInt().coerceIn(8000, 20000)
        val B_min = (B_max / 2).coerceIn(4000, 12000)

        var k_net = if (isConstrained) 1 else floor(3.0f * (1.0f - memPressure) * (1.0f - thermalStatus)).toInt().coerceIn(1, 4)
        var q_img = (1.0f - 0.35f * memPressure - 0.30f * thermalStatus).coerceIn(0.45f, 1.0f)
        if (isPowerSave || isMetered) {
            q_img = min(q_img, 0.70f)
        }

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
        Log.d("PerfTelemetry", "Telemetry update: mem=${(memPressure*100).toInt()}%, native=${(nativePressure*100).toInt()}%, oomRisk=${String.format("%.2f", oomRisk)}, metered=$isMetered, stableTicks=$stableTicksCount")
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
