package com.kira.adrianpet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.annotation.RequiresApi
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.kira.adrianpet.behavior.AppUsageAccess
import com.kira.adrianpet.data.OverlayPreferences
import com.kira.adrianpet.overlay.OverlayService
import com.kira.adrianpet.ui.MainViewModel
import com.kira.adrianpet.ui.PermissionCoordinator
import com.kira.adrianpet.ui.PermissionState
import com.kira.adrianpet.ui.PermissionStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val coordinator = PermissionCoordinator()
    private lateinit var viewModel: MainViewModel
    private var settings: OverlayPreferences? = null
    private var updatingSwitches = false
    private var cloudConfigured = com.kira.adrianpet.data.cloud.CloudRuntime.isConfigured
    private var cloudSignedIn = false

    private val overlaySettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshPermissionUi()
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissionUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = MainViewModel(this)
        bindControls()
        scope.launch {
            viewModel.settings.collectLatest { saved ->
                settings = saved
                updateSwitches(saved)
                refreshPermissionUi()
            }
        }
        scope.launch {
            viewModel.cloud.collectLatest(::updateCloudUi)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUi()
    }

    override fun onDestroy() {
        viewModel.close()
        scope.cancel()
        super.onDestroy()
    }

    private fun bindControls() {
        findViewById<MaterialButton>(R.id.permission_button).setOnClickListener {
            when (currentPermissionStep()) {
                PermissionStep.OVERLAY -> overlaySettings.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()),
                )
                PermissionStep.NOTIFICATION -> if (Build.VERSION.SDK_INT >= 33) {
                    requestOrRecoverNotificationPermission()
                }
                PermissionStep.READY -> Unit
            }
        }
        findViewById<MaterialSwitch>(R.id.service_switch).setOnCheckedChangeListener { _, enabled ->
            if (updatingSwitches) return@setOnCheckedChangeListener
            if (enabled && currentPermissionStep() != PermissionStep.READY) {
                settings?.let(::updateSwitches)
                refreshPermissionUi()
                return@setOnCheckedChangeListener
            }
            scope.launch { viewModel.setServiceEnabled(enabled) }
            if (enabled) {
                ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
            } else {
                stopService(Intent(this, OverlayService::class.java))
            }
        }
        bindBooleanSwitch(R.id.perch_switch, OverlayService.ACTION_SET_PERCH) { viewModel.setPerchEnabled(it) }
        bindBooleanSwitch(R.id.bubbles_switch, OverlayService.ACTION_SET_BUBBLES) { viewModel.setBubblesEnabled(it) }
        bindBooleanSwitch(R.id.haptics_switch, OverlayService.ACTION_SET_HAPTICS) { viewModel.setHapticsEnabled(it) }
        findViewById<MaterialButton>(R.id.recalibrate_button).setOnClickListener {
            sendServiceAction(OverlayService.ACTION_RESET_POSITION)
        }
        findViewById<MaterialButton>(R.id.usage_access_button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        findViewById<MaterialButton>(R.id.cloud_login_button).setOnClickListener {
            val email = findViewById<EditText>(R.id.cloud_email).text.toString()
            val passwordField = findViewById<EditText>(R.id.cloud_password)
            val password = passwordField.text.toString()
            scope.launch {
                viewModel.signIn(email, password)
                passwordField.text?.clear()
            }
        }
        findViewById<MaterialButton>(R.id.cloud_logout_button).setOnClickListener {
            scope.launch { viewModel.signOut() }
        }
    }

    private fun bindBooleanSwitch(id: Int, action: String, persist: suspend (Boolean) -> Unit) {
        findViewById<MaterialSwitch>(id).setOnCheckedChangeListener { _, enabled ->
            if (updatingSwitches) return@setOnCheckedChangeListener
            scope.launch { persist(enabled) }
            sendServiceAction(action, enabled)
        }
    }

    private fun updateSwitches(saved: OverlayPreferences) {
        updatingSwitches = true
        findViewById<MaterialSwitch>(R.id.service_switch).isChecked = saved.serviceEnabled
        findViewById<MaterialSwitch>(R.id.perch_switch).isChecked = saved.perchEnabled
        findViewById<MaterialSwitch>(R.id.bubbles_switch).isChecked = saved.bubblesEnabled
        findViewById<MaterialSwitch>(R.id.haptics_switch).isChecked = saved.hapticsEnabled
        listOf(R.id.perch_switch, R.id.bubbles_switch, R.id.haptics_switch, R.id.recalibrate_button).forEach { id ->
            findViewById<View>(id).isEnabled = saved.serviceEnabled
        }
        updatingSwitches = false
    }

    private fun updateCloudUi(state: com.kira.adrianpet.ui.CloudUiState) {
        cloudConfigured = state.configured
        cloudSignedIn = state.signedInEmail != null
        val loginForm = findViewById<View>(R.id.cloud_login_form)
        val logout = findViewById<MaterialButton>(R.id.cloud_logout_button)
        val status = findViewById<TextView>(R.id.cloud_status)
        val signedIn = state.signedInEmail != null
        loginForm.visibility = if (state.configured && !signedIn) View.VISIBLE else View.GONE
        logout.visibility = if (state.configured && signedIn) View.VISIBLE else View.GONE
        logout.isEnabled = !state.busy
        findViewById<MaterialButton>(R.id.cloud_login_button).isEnabled = !state.busy
        status.text = when {
            !state.configured -> getString(R.string.cloud_unconfigured)
            signedIn -> getString(R.string.cloud_connected, state.signedInEmail)
            state.busy -> getString(R.string.cloud_connecting)
            else -> state.message ?: getString(R.string.cloud_signed_out)
        }
        state.message?.takeIf { signedIn }?.let { status.text = it }
        refreshPermissionUi()
    }

    private fun refreshPermissionUi() {
        if (!::viewModel.isInitialized) return
        val step = currentPermissionStep()
        val loginRequired = cloudConfigured && !cloudSignedIn && step != PermissionStep.READY
        findViewById<View>(R.id.welcome_panel).visibility = if (step == PermissionStep.READY) View.GONE else View.VISIBLE
        findViewById<View>(R.id.settings_panel).visibility = if (step == PermissionStep.READY) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.permission_explanation).setText(
            when {
                loginRequired -> R.string.cloud_login_first
                step == PermissionStep.NOTIFICATION -> R.string.notification_explanation
                else -> R.string.overlay_explanation
            },
        )
        findViewById<MaterialButton>(R.id.permission_button).apply {
            visibility = if (loginRequired) View.GONE else View.VISIBLE
            setText(
                if (step == PermissionStep.NOTIFICATION) R.string.grant_notification else R.string.grant_overlay,
            )
        }
        if (step == PermissionStep.OVERLAY && settings?.serviceEnabled == true) {
            stopService(Intent(this, OverlayService::class.java))
            scope.launch { viewModel.setServiceEnabled(false) }
        }
        refreshUsageAccessUi()
    }

    private fun refreshUsageAccessUi() {
        val granted = AppUsageAccess.isGranted(this)
        findViewById<TextView>(R.id.usage_access_status).setText(
            if (granted) R.string.usage_access_on else R.string.usage_access_off,
        )
        findViewById<MaterialButton>(R.id.usage_access_button).setText(
            if (granted) R.string.manage_usage_access else R.string.grant_usage_access,
        )
    }

    private fun currentPermissionStep(): PermissionStep = coordinator.nextStep(
        PermissionState(
            apiLevel = Build.VERSION.SDK_INT,
            overlayGranted = Settings.canDrawOverlays(this),
            notificationGranted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
            serviceRunning = settings?.serviceEnabled == true,
        ),
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestOrRecoverNotificationPermission() {
        val preferences = getPreferences(MODE_PRIVATE)
        val askedBefore = preferences.getBoolean(KEY_NOTIFICATION_ASKED, false)
        if (askedBefore && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
            return
        }
        preferences.edit { putBoolean(KEY_NOTIFICATION_ASKED, true) }
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun sendServiceAction(action: String, enabled: Boolean? = null) {
        if (settings?.serviceEnabled != true) return
        val intent = Intent(this, OverlayService::class.java).setAction(action)
        enabled?.let { intent.putExtra(OverlayService.EXTRA_ENABLED, it) }
        startService(intent)
    }

    private companion object {
        const val KEY_NOTIFICATION_ASKED = "notification_asked"
    }
}
