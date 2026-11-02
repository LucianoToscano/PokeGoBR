package br.com.pokegobr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameScanGateTest {
    @Test fun defaultAutoAllowsFirstFrame() {
        val gate = FrameScanGate(1700)
        assertTrue(gate.isAutomatic())
        assertTrue(gate.shouldProcess(1000))
    }

    @Test fun automaticIsThrottled() {
        val gate = FrameScanGate(1700)
        assertTrue(gate.shouldProcess(1000))
        assertFalse(gate.shouldProcess(1800))
        assertTrue(gate.shouldProcess(2700))
    }

    @Test fun tapModeDoesNotScanWithoutRequest() {
        val gate = FrameScanGate()
        gate.setAutomatic(false)
        assertFalse(gate.isAutomatic())
        assertFalse(gate.shouldProcess(1000))
        assertFalse(gate.shouldProcess(10000))
    }

    @Test fun requestedManualScanConsumesOneFrame() {
        val gate = FrameScanGate()
        gate.setAutomatic(false)
        gate.requestScan()
        assertTrue(gate.shouldProcess(1000))
        assertFalse(gate.shouldProcess(2000))
        gate.requestScan()
        assertTrue(gate.shouldProcess(2001))
        assertFalse(gate.shouldProcess(2002))
    }

    @Test fun manualRequestOverridesAutomaticCooldown() {
        val gate = FrameScanGate(1700)
        assertTrue(gate.shouldProcess(1000))
        gate.requestScan()
        assertTrue(gate.shouldProcess(1050))
        assertFalse(gate.shouldProcess(1200))
    }

    @Test fun switchingBackToAutoResumesScanning() {
        val gate = FrameScanGate(1700)
        gate.setAutomatic(false)
        assertFalse(gate.shouldProcess(1000))
        gate.setAutomatic(true)
        assertTrue(gate.shouldProcess(1001))
    }

    @Test fun timeResetDoesNotLockOutCapture() {
        val gate = FrameScanGate(1700)
        assertTrue(gate.shouldProcess(120000))
        assertTrue(gate.shouldProcess(20))
    }
}
