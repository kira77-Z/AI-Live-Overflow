package com.kira.adrianpet.behavior

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process

data class ForegroundApp(
    val packageName: String,
    val isGame: Boolean,
)

object AppUsageAccess {
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }
}

class ForegroundAppReader(private val context: Context) {
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    fun current(nowMs: Long = System.currentTimeMillis()): ForegroundApp? {
        if (!AppUsageAccess.isGranted(context) || usageStats == null) return null
        val usageEvents = usageStats.queryEvents(nowMs - EVENT_LOOKBACK_MS, nowMs)
        val event = UsageEvents.Event()
        val candidates = mutableListOf<AppActivityEvent>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            candidates += AppActivityEvent(
                event.packageName,
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED,
                event.timeStamp,
            )
        }
        val packageName = LatestForegroundAppSelector.select(candidates) ?: return null
        return ForegroundApp(packageName, isGame(packageName))
    }

    private fun isGame(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        info.category == ApplicationInfo.CATEGORY_GAME ||
            info.flags and ApplicationInfo.FLAG_IS_GAME != 0
    }.getOrDefault(false)

    private companion object {
        const val EVENT_LOOKBACK_MS = 15_000L
    }
}
