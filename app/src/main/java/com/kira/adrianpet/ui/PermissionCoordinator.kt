package com.kira.adrianpet.ui

data class PermissionState(
    val apiLevel: Int,
    val overlayGranted: Boolean,
    val notificationGranted: Boolean,
    val serviceRunning: Boolean = false,
)

enum class PermissionStep { OVERLAY, NOTIFICATION, READY }

class PermissionCoordinator {
    fun nextStep(state: PermissionState): PermissionStep = when {
        !state.overlayGranted -> PermissionStep.OVERLAY
        state.apiLevel >= 33 && !state.notificationGranted -> PermissionStep.NOTIFICATION
        else -> PermissionStep.READY
    }
}
