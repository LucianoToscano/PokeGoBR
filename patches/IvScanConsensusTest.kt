package br.com.pokegobr

import org.junit.Assert.*
import org.junit.Test

class IvScanConsensusTest {
    @Test fun automaticRequiresTwoMatchingFrames() {
        val gate = IvScanConsensus()
        assertEquals(IvReadiness.Waiting(1, 2),
            gate.observe("Froakie", 281, listOf(3, 15, 12), 1000L, true))
        assertEquals(IvReadiness.Ready(2),
            gate.observe("Froakie", 281, listOf(3, 15, 12), 2800L, true))
        assertEquals(IvReadiness.Waiting(1, 2),
            gate.observe("Froakie", 281, listOf(3, 15, 12), 4800L, true))
    }

    @Test fun mismatchSpeciesCpOrIvRestarts() {
        val gate = IvScanConsensus()
        gate.observe("Froakie", 281, listOf(3, 15, 12), 100L, true)
        assertEquals(IvReadiness.Waiting(1, 2),
            gate.observe("Froakie", 282, listOf(3, 15, 12), 150L, true))
        assertEquals(IvReadiness.Waiting(1, 2),
            gate.observe("Greninja", 282, listOf(3, 15, 12), 250L, true))
        assertEquals(IvReadiness.Waiting(1, 2),
            gate.observe("Greninja", 282, listOf(3, 15, 13), 350L, true))
    }

    @Test fun oldObservationMustExpire() {
        val gate = IvScanConsensus(maxGapMs = 5000L)
        gate.observe("Haunter", 1499, listOf(1, 15, 14), 100L, true)
        assertEquals(IvReadiness.Waiting(1, 2),
            gate.observe("Haunter", 1499, listOf(1, 15, 14), 6000L, true))
    }

    @Test fun manualModeOneFrameButNotVerified() {
        val gate = IvScanConsensus()
        assertEquals(IvReadiness.Ready(1),
            gate.observe("Froakie", 281, listOf(3, 15, 12), 1000L, false))
    }

    @Test fun impossibleIvsNeverBecomeReady() {
        val gate = IvScanConsensus()
        assertEquals(IvReadiness.Waiting(0, 2),
            gate.observe(null, null, listOf(15, 16, 15), 0L, true))
        assertEquals(IvReadiness.Waiting(0, 2),
            gate.observe("Froakie", 281, listOf(15, 15), 1000L, true))
    }

    @Test fun spatialResolverSelectsNamePlateOverCandyLabels() {
        val lines = listOf(
            SpatialOcrLine("Energias Greninja", .82f, .48f),
            SpatialOcrLine("PESO", .51f, .19f),
            SpatialOcrLine("Froakie③⑮⑫", .39f, .51f),
            SpatialOcrLine("PC281", .03f, .51f)
        )
        assertEquals("Froakie", SpatialSpeciesResolver.resolve(lines,
            listOf("Froakie", "Greninja", "Mewtwo")))
    }

    @Test fun spatialResolverRejectsOffPlateName() {
        assertNull(SpatialSpeciesResolver.resolve(
            listOf(SpatialOcrLine("Doces Mewtwo", .83f, .52f)), listOf("Mewtwo")))
        assertNull(SpatialSpeciesResolver.resolve(
            listOf(SpatialOcrLine("PESO", .41f, .52f)), listOf("Froakie")))
    }

    @Test fun acceptsLocaleInsensitivePokemonName() {
        assertEquals("Flabébé", SpatialSpeciesResolver.resolve(
            listOf(SpatialOcrLine("Flabebe", .43f, .52f)),
            listOf("Flabébé")))
    }
}
