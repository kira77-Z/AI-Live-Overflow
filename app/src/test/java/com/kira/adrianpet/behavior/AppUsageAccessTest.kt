package com.kira.adrianpet.behavior

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppUsageAccessTest {

    @Test
    fun platformPermissionCheckFailureDoesNotCrashTheApp() {
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getSystemService(name: String): Any? {
                if (name == Context.APP_OPS_SERVICE) {
                    throw SecurityException("OEM AppOps service rejected the query")
                }
                return super.getSystemService(name)
            }
        }

        assertFalse(AppUsageAccess.isGranted(context))
    }
}
