package com.kira.adrianpet.overlay

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import androidx.core.net.toUri
import com.kira.adrianpet.data.PetPreferences
import com.kira.adrianpet.behavior.ChargingMonitor
import com.kira.adrianpet.behavior.AppSwitchGate
import com.kira.adrianpet.behavior.ForegroundAppBubbleCatalog
import com.kira.adrianpet.behavior.ForegroundAppReader
import com.kira.adrianpet.behavior.PetBehaviorEngine
import com.kira.adrianpet.data.auth.AuthRepository
import com.kira.adrianpet.data.auth.SupabaseAuthTransport
import com.kira.adrianpet.data.cloud.CloudRuntime
import com.kira.adrianpet.data.remote.PetCommand
import com.kira.adrianpet.data.remote.PetEvent
import com.kira.adrianpet.data.remote.PetRemoteRepository
import com.kira.adrianpet.data.remote.SupabasePetRemoteTransport
import com.kira.adrianpet.render.PetState
import com.kira.adrianpet.render.RenderEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var preferences: PetPreferences
    private lateinit var controller: OverlayController
    private lateinit var chargingMonitor: ChargingMonitor
    private val behavior = PetBehaviorEngine()
    private val appSwitchGate = AppSwitchGate()
    private val appBubbleCatalog = ForegroundAppBubbleCatalog()
    private var idleJob: Job? = null
    private var blinkJob: Job? = null
    private var appDetectionJob: Job? = null
    private var isSleeping = false
    private var perchEnabled = false
    private var remoteRepository: PetRemoteRepository? = null

    override fun onCreate() {
        super.onCreate()
        OverlayNotification.ensureChannel(this)
        preferences = PetPreferences(this)
        controller = OverlayController(
            context = this,
            onPositionSettled = { perch, position ->
                scope.launch { preferences.savePosition(perch, position) }
            },
            onInteraction = {
                isSleeping = false
                scheduleIdle()
            },
            onPetEvent = { type, payload -> recordPetEvent(PetEvent(type, payload)) },
            onOverlayFailure = ::disableAndStop,
        )
        chargingMonitor = ChargingMonitor(this) { charging ->
            behavior.onChargingChanged(charging)?.let { event ->
                if (controller.render(event)) updateRestState(event.state)
                recordPetEvent(PetEvent("charging", mapOf("connected" to charging.toString())))
            }
        }
        startForeground(
            OverlayNotification.NOTIFICATION_ID,
            OverlayNotification.build(this, visible = false, perch = false),
        )

        if (!Settings.canDrawOverlays(this)) {
            disableAndStop()
            return
        }

        scope.launch {
            val saved = preferences.overlay.first()
            perchEnabled = saved.perchEnabled
            controller.restorePositions(saved.normal, saved.perch)
            controller.setHapticsEnabled(saved.hapticsEnabled)
            controller.setBubblesEnabled(saved.bubblesEnabled)
            controller.setPerchMode(perchEnabled)
            controller.show()
            if (!controller.isShown) return@launch
            chargingMonitor.start()
            scheduleIdle()
            scheduleBlinking()
            startAppDetection()
            updateNotification()
            startCloudSync()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_VISIBILITY -> {
                if (controller.isShown) {
                    controller.hide()
                } else {
                    controller.show()
                    isSleeping = false
                    scheduleIdle()
                }
                updateNotification()
            }
            ACTION_TOGGLE_PERCH -> {
                perchEnabled = !perchEnabled
                controller.setPerchMode(perchEnabled)
                scope.launch { preferences.setPerchEnabled(perchEnabled) }
                updateNotification()
            }
            ACTION_SET_PERCH -> {
                perchEnabled = intent.getBooleanExtra(EXTRA_ENABLED, false)
                controller.setPerchMode(perchEnabled)
                scope.launch { preferences.setPerchEnabled(perchEnabled) }
                updateNotification()
            }
            ACTION_SET_HAPTICS -> {
                val enabled = intent.getBooleanExtra(EXTRA_ENABLED, true)
                controller.setHapticsEnabled(enabled)
                scope.launch { preferences.setHapticsEnabled(enabled) }
            }
            ACTION_SET_BUBBLES -> {
                val enabled = intent.getBooleanExtra(EXTRA_ENABLED, true)
                controller.setBubblesEnabled(enabled)
                scope.launch { preferences.setBubblesEnabled(enabled) }
            }
            ACTION_RESET_POSITION -> scope.launch {
                preferences.resetPositions()
                controller.restorePositions(
                    PetPreferences.DEFAULT_NORMAL_POSITION,
                    PetPreferences.DEFAULT_PERCH_POSITION,
                )
                controller.setPerchMode(perchEnabled)
            }
            ACTION_OPEN_SETTINGS -> openOverlaySettings()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller.reClamp()
    }

    override fun onDestroy() {
        idleJob?.cancel()
        blinkJob?.cancel()
        appDetectionJob?.cancel()
        chargingMonitor.stop()
        controller.destroy()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(
            OverlayNotification.NOTIFICATION_ID,
            OverlayNotification.build(this, controller.isShown, perchEnabled),
        )
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun scheduleIdle() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            val event = behavior.onIdleTimeout()
            controller.render(event)
            isSleeping = true
            recordPetEvent(PetEvent("idle"))
        }
    }

    private fun scheduleBlinking() {
        blinkJob?.cancel()
        blinkJob = scope.launch {
            while (true) {
                delay(BLINK_INTERVAL_MS)
                if (!Settings.canDrawOverlays(this@OverlayService)) {
                    preferences.setServiceEnabled(false)
                    stopSelf()
                    return@launch
                }
                if (!isSleeping) controller.render(RenderEvent(PetState.BLINK))
            }
        }
    }

    private fun startAppDetection() {
        appDetectionJob?.cancel()
        val reader = ForegroundAppReader(this)
        appDetectionJob = scope.launch {
            while (isActive) {
                val app = withContext(Dispatchers.Default) {
                    runCatching { reader.current() }.getOrNull()
                }
                val enteredPackage = appSwitchGate.observe(
                    app?.packageName,
                    SystemClock.elapsedRealtime(),
                )
                val bubble = enteredPackage?.let { packageName ->
                    appBubbleCatalog.bubbleFor(packageName, app?.isGame == true)
                }
                if (bubble != null) {
                    val event = RenderEvent(PetState.IDLE, bubble)
                    if (controller.render(event)) updateRestState(event.state)
                }
                delay(APP_DETECTION_INTERVAL_MS)
            }
        }
    }

    private fun startCloudSync() {
        val client = CloudRuntime.clientOrNull(this) ?: return
        val auth = AuthRepository(SupabaseAuthTransport(client, scope))
        val remote = PetRemoteRepository(SupabasePetRemoteTransport(client))
        remoteRepository = remote
        scope.launch {
            auth.session.collectLatest { session ->
                if (session == null) return@collectLatest
                runCatching { remote.recordEvent(PetEvent("app_open")) }
                while (true) {
                    remote.observeCommands(session.userId).collect { command ->
                        presentRemoteCommand(remote, session.userId, command)
                    }
                    delay(CLOUD_RECONNECT_DELAY_MS)
                }
            }
        }
    }

    private suspend fun presentRemoteCommand(
        remote: PetRemoteRepository,
        userId: String,
        command: PetCommand,
    ) {
        val commandKey = "$userId:${command.id}"
        if (preferences.wasCommandPresented(commandKey)) {
            acknowledgeUntilSuccessful(remote, userId, command.id, commandKey)
            return
        }
        while (currentCoroutineContext().isActive && !controller.render(command.asRenderEvent())) {
            delay(RENDER_RETRY_DELAY_MS)
        }
        updateRestState(command.state)
        preferences.markCommandPresented(commandKey)
        acknowledgeUntilSuccessful(remote, userId, command.id, commandKey)
    }

    private suspend fun acknowledgeUntilSuccessful(
        remote: PetRemoteRepository,
        userId: String,
        commandId: String,
        commandKey: String,
    ) {
        while (currentCoroutineContext().isActive) {
            val acknowledged = runCatching { remote.acknowledge(userId, commandId) }.isSuccess
            if (acknowledged) {
                preferences.clearPresentedCommand(commandKey)
                return
            }
            delay(CLOUD_RECONNECT_DELAY_MS)
        }
    }

    private fun recordPetEvent(event: PetEvent) {
        val remote = remoteRepository ?: return
        scope.launch { runCatching { remote.recordEvent(event) } }
    }

    private fun updateRestState(state: PetState) {
        if (state == PetState.SLEEP) {
            isSleeping = true
            idleJob?.cancel()
        } else {
            isSleeping = false
            scheduleIdle()
        }
    }

    private fun disableAndStop() {
        scope.launch {
            try {
                preferences.setServiceEnabled(false)
            } finally {
                stopSelf()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_VISIBILITY = "com.kira.adrianpet.action.TOGGLE_VISIBILITY"
        const val ACTION_TOGGLE_PERCH = "com.kira.adrianpet.action.TOGGLE_PERCH"
        const val ACTION_OPEN_SETTINGS = "com.kira.adrianpet.action.OPEN_SETTINGS"
        const val ACTION_SET_PERCH = "com.kira.adrianpet.action.SET_PERCH"
        const val ACTION_SET_HAPTICS = "com.kira.adrianpet.action.SET_HAPTICS"
        const val ACTION_SET_BUBBLES = "com.kira.adrianpet.action.SET_BUBBLES"
        const val ACTION_RESET_POSITION = "com.kira.adrianpet.action.RESET_POSITION"
        const val EXTRA_ENABLED = "enabled"
        private const val IDLE_TIMEOUT_MS = 5 * 60 * 1_000L
        private const val CLOUD_RECONNECT_DELAY_MS = 5_000L
        private const val RENDER_RETRY_DELAY_MS = 100L
        private const val BLINK_INTERVAL_MS = 15_000L
        private const val APP_DETECTION_INTERVAL_MS = 3_000L
    }
}
