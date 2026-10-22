package com.kira.adrianpet.behavior

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat

class ChargingMonitor(
    private val context: Context,
    private val onChargingChanged: (Boolean) -> Unit,
) {
    private var registered = false
    private var lastCharging: Boolean? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val previous = lastCharging
            lastCharging = charging
            if (previous != null && previous != charging) onChargingChanged(charging)
        }
    }

    fun start() {
        if (registered) return
        registered = true
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun stop() {
        if (!registered) return
        registered = false
        lastCharging = null
        context.unregisterReceiver(receiver)
    }
}
