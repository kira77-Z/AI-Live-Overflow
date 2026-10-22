package com.kira.adrianpet.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionCoordinatorTest {
    private val coordinator = PermissionCoordinator()

    @Test
    fun `overlay denial is always the first step`() {
        assertEquals(
            PermissionStep.OVERLAY,
            coordinator.nextStep(PermissionState(apiLevel = 36, overlayGranted = false, notificationGranted = false)),
        )
    }

    @Test
    fun `notification is requested on API 33 and newer`() {
        assertEquals(
            PermissionStep.NOTIFICATION,
            coordinator.nextStep(PermissionState(apiLevel = 33, overlayGranted = true, notificationGranted = false)),
        )
    }

    @Test
    fun `notification runtime permission is not required before API 33`() {
        assertEquals(
            PermissionStep.READY,
            coordinator.nextStep(PermissionState(apiLevel = 26, overlayGranted = true, notificationGranted = false)),
        )
        assertEquals(
            PermissionStep.READY,
            coordinator.nextStep(PermissionState(apiLevel = 32, overlayGranted = true, notificationGranted = false)),
        )
    }

    @Test
    fun `revoked overlay wins even if service was running`() {
        assertEquals(
            PermissionStep.OVERLAY,
            coordinator.nextStep(
                PermissionState(apiLevel = 36, overlayGranted = false, notificationGranted = true, serviceRunning = true),
            ),
        )
    }

    @Test
    fun `all required permissions lead to ready`() {
        assertEquals(
            PermissionStep.READY,
            coordinator.nextStep(PermissionState(apiLevel = 36, overlayGranted = true, notificationGranted = true)),
        )
    }
}
