package com.kira.adrianpet.ui

import android.content.Context
import com.kira.adrianpet.data.PetPreferences
import com.kira.adrianpet.data.auth.AuthRepository
import com.kira.adrianpet.data.auth.AuthResult
import com.kira.adrianpet.data.auth.SupabaseAuthTransport
import com.kira.adrianpet.data.cloud.CloudRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainViewModel(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = PetPreferences(context.applicationContext)
    private val authRepository = CloudRuntime.clientOrNull(context)?.let { AuthRepository(SupabaseAuthTransport(it, scope)) }
    private val loginMessage = MutableStateFlow<String?>(null)
    private val loginBusy = MutableStateFlow(false)
    val settings = preferences.overlay
    val cloud = if (authRepository == null) {
        MutableStateFlow(CloudUiState(configured = false))
    } else {
        combine(authRepository.session, loginBusy, loginMessage) { session, busy, message ->
            CloudUiState(
                configured = true,
                signedInEmail = session?.email,
                busy = busy,
                message = message,
            )
        }.stateIn(scope, SharingStarted.Eagerly, CloudUiState(configured = true, busy = true))
    }

    suspend fun setServiceEnabled(enabled: Boolean) = preferences.setServiceEnabled(enabled)
    suspend fun setPerchEnabled(enabled: Boolean) = preferences.setPerchEnabled(enabled)
    suspend fun setBubblesEnabled(enabled: Boolean) = preferences.setBubblesEnabled(enabled)
    suspend fun setHapticsEnabled(enabled: Boolean) = preferences.setHapticsEnabled(enabled)

    suspend fun signIn(email: String, password: String) {
        val repository = authRepository ?: return
        loginBusy.value = true
        loginMessage.value = null
        loginMessage.value = when (val result = repository.signIn(email, password)) {
            AuthResult.Success -> "已连接，远程气泡会自动送到小黑豹这里"
            is AuthResult.Failure -> result.message
        }
        loginBusy.value = false
    }

    suspend fun signOut() {
        loginBusy.value = true
        authRepository?.signOut()
        loginMessage.value = "已退出云端连接，本地互动仍会继续"
        loginBusy.value = false
    }

    fun close() = scope.cancel()
}

data class CloudUiState(
    val configured: Boolean,
    val signedInEmail: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)
