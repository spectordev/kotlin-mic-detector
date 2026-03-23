package com.example.backgroundapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 (debug): default backend base URL must be set so the emulator can reach the host.
 * Physical device: override upload URL in UI to http://<PC_LAN_IP>:3000/recordings/upload
 */
class Phase5BackendConfigTest {

    @Test
    fun debugBuild_pointsEmulatorToHostLoopback() {
        if (BuildConfig.DEBUG) {
            assertEquals("http://localhost:3000", BuildConfig.BACKEND_BASE_URL)
            assertTrue(
                "${BuildConfig.BACKEND_BASE_URL}/recordings/upload".startsWith("http"),
            )
        }
    }
}
