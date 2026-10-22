package com.kira.adrianpet.behavior

import org.junit.Assert.assertFalse
import org.junit.Test

class AppUsageAccessTest {

    @Test
    fun platformPermissionCheckFailureDoesNotCrashTheApp() {
        val granted = AppUsageAccess.isModeAllowed {
            throw SecurityException("OEM AppOps service rejected the query")
        }

        assertFalse(granted)
    }
}
