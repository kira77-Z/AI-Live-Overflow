package com.kira.adrianpet.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupabaseAuthTransport(
    private val client: SupabaseClient,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AuthTransport {
    private val mutableSession = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = mutableSession.asStateFlow()

    init {
        scope.launch {
            client.auth.awaitInitialization()
            client.auth.sessionStatus.collect(::updateSession)
        }
    }

    override suspend fun signIn(email: String, password: String) {
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (error: AuthRestException) {
            throw InvalidCredentialsException(error)
        }
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    private fun updateSession(status: SessionStatus) {
        mutableSession.value = (status as? SessionStatus.Authenticated)?.session?.user?.let { user ->
            AuthSession(userId = user.id, email = user.email)
        }
    }
}
