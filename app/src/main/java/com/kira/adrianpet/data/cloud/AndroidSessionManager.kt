package com.kira.adrianpet.data.cloud

import android.content.Context
import androidx.core.content.edit
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

class AndroidSessionManager(context: Context) : SessionManager {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        preferences.edit(commit = true) { putString(SESSION_KEY, json.encodeToString(session)) }
    }

    override suspend fun loadSession(): UserSession? = preferences.getString(SESSION_KEY, null)?.let { encoded ->
        runCatching { json.decodeFromString<UserSession>(encoded) }
            .onFailure { preferences.edit(commit = true) { remove(SESSION_KEY) } }
            .getOrNull()
    }

    override suspend fun deleteSession() {
        preferences.edit(commit = true) { remove(SESSION_KEY) }
    }

    private companion object {
        const val PREFERENCES_NAME = "adrian_cloud_session"
        const val SESSION_KEY = "supabase_session"
    }
}
