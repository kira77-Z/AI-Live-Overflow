package com.kira.adrianpet

import android.app.Activity
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class ProjectConfigTest {

    @Test
    fun launcherActivityStartsOnMinimumSupportedAndroidVersion() {
        val activityClass = Class.forName("com.kira.adrianpet.MainActivity")
            .asSubclass(Activity::class.java)

        val activity = Robolectric.buildActivity(activityClass).setup().get()

        assertFalse(activity.isFinishing)
    }

    @Test
    fun installedAppIncludesRequiredPermissionsAndNoUnrelatedSensitivePermissions() {
        val application = RuntimeEnvironment.getApplication()
        val packageInfo = application.packageManager.getPackageInfo(
            application.packageName,
            PackageManager.GET_PERMISSIONS,
        )

        val requested = packageInfo.requestedPermissions.orEmpty().toSet()
        val required = setOf(
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
            "android.permission.INTERNET",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.VIBRATE",
        )
        val forbidden = setOf(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_EXTERNAL_STORAGE",
        )

        assertTrue("Missing required permissions: ${required - requested}", requested.containsAll(required))
        assertEquals(emptySet<String>(), requested.intersect(forbidden))
    }
}
