package com.kira.adrianpet.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kira.adrianpet.MainActivity
import com.kira.adrianpet.R

object OverlayNotification {
    const val CHANNEL_ID = "adrian_pet_overlay"
    const val NOTIFICATION_ID = 1701

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notification_channel_description)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    fun build(context: Context, visible: Boolean, perch: Boolean): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(settingsAction(context))
            .addAction(0, context.getString(if (visible) R.string.action_hide else R.string.action_show), serviceAction(context, OverlayService.ACTION_TOGGLE_VISIBILITY, 1))
            .addAction(0, context.getString(if (perch) R.string.action_normal_mode else R.string.action_perch_mode), serviceAction(context, OverlayService.ACTION_TOGGLE_PERCH, 2))
            .addAction(0, context.getString(R.string.action_settings), settingsAction(context))
            .build()

    private fun serviceAction(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, OverlayService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun settingsAction(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        3,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
