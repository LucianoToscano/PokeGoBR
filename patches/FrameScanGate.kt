package br.com.pokegobr

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local throttling for the frame reader. User chooses automatic or tap-to-scan.
 * No permission token is cached or reused. A manual request is processed once.
 */
internal class FrameScanGate(private val autoIntervalMs: Long = 1700L) {
    private val auto = AtomicBoolean(true)
    private val manual = AtomicBoolean(false)
    private var lastAcceptedMs: Long? = null

    fun isAutomatic(): Boolean = auto.get()

    fun setAutomatic(enabled: Boolean) {
        auto.set(enabled)
        if (enabled) manual.set(false)
    }

    fun requestScan() {
        manual.set(true)
    }

    @Synchronized
    fun shouldProcess(nowMs: Long): Boolean {
        if (manual.getAndSet(false)) {
            lastAcceptedMs = nowMs
            return true
        }
        if (!auto.get()) return false
        val prior = lastAcceptedMs
        if (prior != null && nowMs >= prior && nowMs - prior < autoIntervalMs) return false
        lastAcceptedMs = nowMs
        return true
    }
}
